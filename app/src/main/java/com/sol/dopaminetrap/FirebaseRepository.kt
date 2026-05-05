package com.sol.dopaminetrap

import android.content.Context
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sol.dopaminetrap.analysis.WellbeingCalculator
import com.sol.dopaminetrap.data.FamilySettings
import com.sol.dopaminetrap.data.WellbeingProfile
import kotlinx.coroutines.tasks.await

data class ChildInfo(val childId: String, val childName: String)

object FirebaseRepository {
    private const val TAG = "FirebaseRepository"
    private const val DOC_SETTINGS = "settings"

    private var listenerRegistration: ListenerRegistration? = null

    private suspend fun ensureAuth() {
        if (Firebase.auth.currentUser == null) {
            runCatching { Firebase.auth.signInAnonymously().await() }
        }
    }

    var currentSettings: FamilySettings = FamilySettings()
        private set

    // ── Path helpers ──────────────────────────────────────────────────────────

    private fun childRef(familyId: String, childId: String): DocumentReference =
        Firebase.firestore
            .collection("families").document(familyId)
            .collection("children").document(childId)

    private fun settingsRef(familyId: String, childId: String) =
        childRef(familyId, childId).collection("config").document(DOC_SETTINGS)

    private fun parentRef(familyId: String) =
        Firebase.firestore
            .collection("families").document(familyId)
            .collection("config").document("parent")

    // ── Child listener ────────────────────────────────────────────────────────

    /**
     * Pornit de copil la lansarea aplicatiei.
     * Asculta Firestore real-time si aplica setarile in VPN service.
     */
    fun startChildListener(context: Context, familyId: String, childId: String) {
        stopListener()
        listenerRegistration = settingsRef(familyId, childId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val settings = FamilySettings.fromMap(snapshot.data ?: return@addSnapshotListener)
            applySettingsToChild(context, settings)
        }
    }

    fun stopListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    suspend fun pushSettings(familyId: String, childId: String, settings: FamilySettings) {
        ensureAuth()
        settingsRef(familyId, childId).set(settings.toMap()).await()
        currentSettings = settings
    }

    suspend fun fetchSettings(familyId: String, childId: String): FamilySettings {
        ensureAuth()
        val snapshot = settingsRef(familyId, childId).get().await()
        if (!snapshot.exists()) return FamilySettings()
        return FamilySettings.fromMap(snapshot.data ?: return FamilySettings())
    }

    // ── Children list (pentru UI-ul parintelui) ───────────────────────────────

    suspend fun fetchChildren(familyId: String): List<ChildInfo> {
        ensureAuth()
        val snapshot = Firebase.firestore
            .collection("families").document(familyId)
            .collection("children")
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            ChildInfo(childId = doc.id, childName = name)
        }
    }

    // ── Rapoarte ──────────────────────────────────────────────────────────────

    suspend fun fetchReports(familyId: String, childId: String): List<Map<String, Any>> {
        ensureAuth()
        val snapshot = childRef(familyId, childId)
            .collection("reports")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    // ── Alerte recente ────────────────────────────────────────────────────────

    suspend fun fetchRecentAlerts(familyId: String, childId: String): List<Map<String, Any>> {
        ensureAuth()
        val snapshot = childRef(familyId, childId)
            .collection("alerts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    // ── Well-being profile ────────────────────────────────────────────────────

    suspend fun pushWellbeingProfile(familyId: String, childId: String, profile: WellbeingProfile) {
        childRef(familyId, childId)
            .collection("wellbeing")
            .document("latest")
            .set(WellbeingCalculator.toMap(profile))
            .await()
    }

    suspend fun fetchWellbeingProfile(familyId: String, childId: String): WellbeingProfile? {
        ensureAuth()
        val snapshot = childRef(familyId, childId)
            .collection("wellbeing")
            .document("latest")
            .get()
            .await()
        if (!snapshot.exists()) return null
        return runCatching { WellbeingCalculator.fromMap(snapshot.data ?: return null) }.getOrNull()
    }

    // ── Generare cod pairing (pentru reconectare de pe telefonul copilului) ───

    suspend fun generatePairingCode(familyId: String, childId: String, childName: String): String {
        val code = (100000..999999).random().toString()
        Firebase.firestore.collection("pairing").document(code).set(
            mapOf(
                "familyId"  to familyId,
                "childId"   to childId,
                "childName" to childName,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        return code
    }

    // ── Feedback suport ───────────────────────────────────────────────────────

    suspend fun submitFeedback(familyId: String, childId: String, category: String, message: String) {
        Firebase.firestore
            .collection("feedback")
            .add(mapOf(
                "familyId"  to familyId,
                "childId"   to childId,
                "category"  to category,
                "message"   to message,
                "timestamp" to System.currentTimeMillis()
            ))
            .await()
    }

    // ── Inregistrare / eliminare copil ────────────────────────────────────────

    suspend fun removeChild(familyId: String, childId: String) {
        childRef(familyId, childId).delete().await()
    }

    suspend fun addChildFromPairingCode(parentFamilyId: String, code: String): ChildInfo {
        val doc = Firebase.firestore.collection("pairing").document(code).get().await()
        if (!doc.exists()) throw Exception("Cod invalid sau expirat.")
        val createdAt = doc.getLong("createdAt") ?: 0L
        if (System.currentTimeMillis() - createdAt > 15 * 60 * 1000L)
            throw Exception("Codul a expirat. Generează unul nou de pe telefonul copilului.")
        val childId   = doc.getString("childId")   ?: throw Exception("Date corupte.")
        val childName = doc.getString("childName") ?: "Copil"
        registerChild(parentFamilyId, childId, childName)
        // Semnalează copilului să folosească familyId-ul părintelui
        Firebase.firestore.collection("pairing").document(code)
            .update("resolvedFamilyId", parentFamilyId).await()
        return ChildInfo(childId, childName)
    }

    suspend fun registerChild(familyId: String, childId: String, childName: String) {
        childRef(familyId, childId).set(mapOf("name" to childName)).await()
    }

    // ── Apply settings ────────────────────────────────────────────────────────

    private fun applySettingsToChild(context: Context, settings: FamilySettings) {
        currentSettings = settings

        SettingsManager.setEnabled(context, ProtectedApp.TIKTOK, settings.tiktokEnabled)
        SettingsManager.setEnabled(context, ProtectedApp.INSTAGRAM, settings.instagramEnabled)
        SettingsManager.setEnabled(context, ProtectedApp.YOUTUBE_SHORTS, settings.youtubeShortsEnabled)

        DopamineVpnService.burstBytes.set(settings.burstSizeKb * 1024L)
        DopamineVpnService.pauseMs.set(settings.pauseDurationMs)

        SessionTracker.setLimits(
            tiktokMin         = settings.tiktokLimitMinutes,
            instagramMin      = settings.instagramLimitMinutes,
            youtubeShortsMin  = settings.youtubeShortsLimitMinutes
        )

        DopamineVpnService.instance?.rebuildTunnel()

        Log.d(TAG, "Setari aplicate: $settings")
    }
}
