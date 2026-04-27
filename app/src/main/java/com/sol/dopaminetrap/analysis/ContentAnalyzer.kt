package com.sol.dopaminetrap.analysis

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.data.AppDatabase
import com.sol.dopaminetrap.data.ConcernLevel
import com.sol.dopaminetrap.data.ContentEvent
import com.sol.dopaminetrap.ml.ModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import java.util.LinkedList

object ContentAnalyzer {

    private const val TAG = "ContentAnalyzer"
    private const val MAX_RAW_TEXT = 500
    private const val MIN_WORD_COUNT = 5
    private const val CONTEXT_BUFFER_SIZE = 5

    // ─── Surse de mesagerie (debounce mic, threshold mic) ─────────────────────
    private val MESSAGING_SOURCES = setOf(
        "SMS", "WhatsApp", "WhatsApp Business",
        "Telegram", "Messenger", "Signal", "Viber", "Snapchat",
        "Instagram DM"
    )

    // Content (TikTok, Instagram feed, YouTube)
    private const val CONTENT_DEBOUNCE_MS   = 3_000L
    private const val CONTENT_THRESHOLD     = 5
    private const val CONTENT_WINDOW_MS     = 10 * 60 * 1000L

    // Mesagerie
    private const val MESSAGING_DEBOUNCE_MS = 500L
    private const val MESSAGING_THRESHOLD   = 3
    private const val MESSAGING_WINDOW_MS   = 5 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Debounce per sursă
    private val lastKeyPerSource  = ConcurrentHashMap<String, String>()
    private val lastTimePerSource = ConcurrentHashMap<String, Long>()

    // Timestamps per tip de sursă
    private val contentTimestamps   = mutableListOf<Long>()
    private val messagingTimestamps = mutableListOf<Long>()

    // Rolling context buffer per sursă de mesagerie (ultimele CONTEXT_BUFFER_SIZE mesaje)
    private val messagingContext = ConcurrentHashMap<String, LinkedList<String>>()

    fun analyze(context: Context, text: String, sourceApp: String) {
        if (text.isBlank()) return

        // Pre-filtrare: minim MIN_WORD_COUNT cuvinte
        val wordCount = text.trim().split(Regex("\\s+")).size
        if (wordCount < MIN_WORD_COUNT) {
            Log.d(TAG, "[$sourceApp] Skip text scurt ($wordCount cuvinte)")
            return
        }

        val isMessaging = sourceApp in MESSAGING_SOURCES
        val debounceMs  = if (isMessaging) MESSAGING_DEBOUNCE_MS else CONTENT_DEBOUNCE_MS
        val now = System.currentTimeMillis()
        val key = "$sourceApp:${text.take(100).hashCode()}"

        val lastKey  = lastKeyPerSource[sourceApp] ?: ""
        val lastTime = lastTimePerSource[sourceApp] ?: 0L

        if (key == lastKey) return
        if (now - lastTime < debounceMs) return

        lastKeyPerSource[sourceApp]  = key
        lastTimePerSource[sourceApp] = now

        // Pentru mesagerie: acumulăm context și analizăm textul combinat
        val textToAnalyze = if (isMessaging) {
            val buffer = messagingContext.getOrPut(sourceApp) { LinkedList() }
            synchronized(buffer) {
                buffer.addLast(text)
                if (buffer.size > CONTEXT_BUFFER_SIZE) buffer.removeFirst()
                buffer.joinToString(" | ")
            }
        } else {
            text
        }

        // Keyword-first: dacă găsim CRITICAL → skip ML (economisim inferența)
        val keywordCategories = KeywordMatcher.analyze(textToAnalyze)
        val keywordConcern = if (keywordCategories.isNotEmpty()) KeywordMatcher.maxConcernLevel(keywordCategories) else null

        val mlCategories = if (keywordConcern == ConcernLevel.CRITICAL) {
            Log.d(TAG, "[$sourceApp] CRITICAL via keywords — skip ML")
            emptyList()
        } else {
            ModelManager.classify(context, textToAnalyze)
        }

        val categories = (mlCategories + keywordCategories).distinct()
        if (categories.isEmpty()) return

        val concernLevel = KeywordMatcher.maxConcernLevel(categories)
        val categoriesStr = categories.joinToString("|") { it.name }

        Log.d(TAG, "[$sourceApp] ${categories.size} categorii: $categoriesStr (concern: $concernLevel)")

        scope.launch {
            AppDatabase.get(context).contentEventDao().insert(
                ContentEvent(
                    sourceApp = sourceApp,
                    rawText = text.take(MAX_RAW_TEXT),
                    categories = categoriesStr,
                    concernLevel = concernLevel.ordinal
                )
            )

            checkAndAlert(context, sourceApp, categoriesStr, concernLevel, isMessaging, now)
        }
    }

    private suspend fun checkAndAlert(
        context: Context,
        sourceApp: String,
        categoriesStr: String,
        concernLevel: ConcernLevel,
        isMessaging: Boolean,
        now: Long
    ) {
        // Mesagerie: contam HIGH + CRITICAL (bullying poate fi HIGH)
        // Content: contam doar CRITICAL
        val shouldCount = if (isMessaging)
            concernLevel == ConcernLevel.CRITICAL || concernLevel == ConcernLevel.HIGH
        else
            concernLevel == ConcernLevel.CRITICAL

        if (!shouldCount) return

        val threshold  = if (isMessaging) MESSAGING_THRESHOLD  else CONTENT_THRESHOLD
        val windowMs   = if (isMessaging) MESSAGING_WINDOW_MS  else CONTENT_WINDOW_MS
        val timestamps = if (isMessaging) messagingTimestamps   else contentTimestamps

        synchronized(timestamps) {
            timestamps.removeAll { now - it > windowMs }
            timestamps.add(now)
            Log.d(TAG, "${if (isMessaging) "Mesagerie" else "Content"} alert count: ${timestamps.size}/$threshold")
            if (timestamps.size >= threshold) {
                timestamps.clear()
                Log.d(TAG, "Prag atins — trimit alerta ($sourceApp)")
                scope.launch { sendAlert(context, sourceApp, categoriesStr, isMessaging) }
            }
        }
    }

    private suspend fun sendAlert(
        context: Context,
        sourceApp: String,
        categories: String,
        isMessaging: Boolean
    ) {
        val familyId = OnboardingManager.getFamilyId(context) ?: return
        val childId  = OnboardingManager.getChildId(context)  ?: return
        runCatching {
            Firebase.firestore
                .collection("families").document(familyId)
                .collection("children").document(childId)
                .collection("alerts")
                .add(mapOf<String, Any>(
                    "sourceApp"    to sourceApp,
                    "category"     to (categories.split("|").firstOrNull { it.isNotEmpty() } ?: categories),
                    "allCategories" to categories,
                    "sourceType"   to if (isMessaging) "messaging" else "content",
                    "timestamp"    to System.currentTimeMillis()
                ))
                .await()
            Log.d(TAG, "Alerta trimisa Firestore: $categories ($sourceApp)")
        }.onFailure {
            Log.e(TAG, "Eroare la trimitere alerta: ${it.message}")
        }
    }
}
