package com.sol.dopaminetrap.worker

import android.content.Context
import androidx.work.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.analysis.WellbeingCalculator
import com.sol.dopaminetrap.data.AppDatabase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class DailyWellbeingWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        private const val WORK_NAME = "daily_wellbeing"
        private const val DROP_THRESHOLD = 0.25f
        private const val CRITICAL_SCORE = 0.22f

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyWellbeingWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(2, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val familyId = OnboardingManager.getFamilyId(applicationContext) ?: return Result.success()
        val childId  = OnboardingManager.getChildId(applicationContext)  ?: return Result.success()

        val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val events = AppDatabase.get(applicationContext).contentEventDao().getEventsSince(dayAgo)

        if (events.isEmpty()) return Result.success()

        val todayProfile    = WellbeingCalculator.calculate(events)
        val previousProfile = FirebaseRepository.fetchWellbeingProfile(familyId, childId)

        FirebaseRepository.pushWellbeingProfile(familyId, childId, todayProfile)

        val shouldAlert = when {
            todayProfile.emotionalScore < CRITICAL_SCORE -> true
            previousProfile != null &&
                (previousProfile.emotionalScore - todayProfile.emotionalScore) >= DROP_THRESHOLD &&
                todayProfile.emotionalScore < 0.42f -> true
            else -> false
        }

        if (shouldAlert) {
            sendWellbeingDropAlert(familyId, childId, todayProfile.emotionalScore, previousProfile?.emotionalScore)
        }

        return Result.success()
    }

    private suspend fun sendWellbeingDropAlert(
        familyId: String,
        childId: String,
        todayScore: Float,
        previousScore: Float?
    ) {
        val childName = OnboardingManager.getChildName(applicationContext) ?: "Copilul tău"
        val message = when {
            todayScore < CRITICAL_SCORE ->
                "$childName pare că are o zi cu adevărat grea. Poate fi un moment important pentru o conversație."
            previousScore != null ->
                "$childName pare mai trist sau anxios decât de obicei astăzi. Merită să îi acorzi atenție."
            else ->
                "Starea emoțională a lui $childName necesită atenție."
        }

        runCatching {
            Firebase.firestore
                .collection("families").document(familyId)
                .collection("children").document(childId)
                .collection("alerts")
                .add(mapOf<String, Any>(
                    "sourceApp"     to "Well-being Monitor",
                    "category"      to "Schimbare stare emoțională",
                    "allCategories" to "WELLBEING_DROP",
                    "sourceType"    to "wellbeing",
                    "message"       to message,
                    "emotionalScore" to todayScore,
                    "timestamp"     to System.currentTimeMillis()
                ))
                .await()
        }
    }
}
