package com.sol.dopaminetrap

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sol.dopaminetrap.data.FamilySettings
import kotlinx.coroutines.tasks.await

data class ChildInfo(val childId: String, val childName: String)

object FirebaseRepository {
    private const val TAG = "FirebaseRepository"
    private const val DOC_SETTINGS = "settings"

    private var listenerRegistration: ListenerRegistration? = null

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
        settingsRef(familyId, childId).set(settings.toMap()).await()
        currentSettings = settings
    }

    suspend fun fetchSettings(familyId: String, childId: String): FamilySettings {
        val snapshot = settingsRef(familyId, childId).get().await()
        if (!snapshot.exists()) return FamilySettings()
        return FamilySettings.fromMap(snapshot.data ?: return FamilySettings())
    }

    // ── Children list (pentru UI-ul parintelui) ───────────────────────────────

    suspend fun fetchChildren(familyId: String): List<ChildInfo> {
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
        val snapshot = childRef(familyId, childId)
            .collection("alerts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.data }
    }

    // ── Inregistrare copil in Firestore (la onboarding copil) ─────────────────

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

        DopamineVpnService.instance?.rebuildTunnel()

        Log.d(TAG, "Setari aplicate: $settings")
    }
}
