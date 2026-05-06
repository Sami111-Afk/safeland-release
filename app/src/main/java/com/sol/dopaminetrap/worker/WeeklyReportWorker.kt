package com.sol.dopaminetrap.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sol.dopaminetrap.FirebaseRepository
import com.sol.dopaminetrap.OnboardingManager
import com.sol.dopaminetrap.analysis.WellbeingCalculator
import com.sol.dopaminetrap.data.AppDatabase
import com.sol.dopaminetrap.data.CategoryType
import com.sol.dopaminetrap.data.ConcernLevel
import com.sol.dopaminetrap.data.ContentCategory
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class WeeklyReportWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        private const val CHANNEL_ID = "SafelandReport"
        private const val NOTIF_ID = 100
        private const val WORK_NAME = "weekly_report"
        private const val KEEP_DATA_DAYS = 90L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(7, TimeUnit.DAYS)
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
        val db = AppDatabase.get(applicationContext)

        if (!FirebaseRepository.currentSettings.weeklyReportEnabled) {
            val cutoff = System.currentTimeMillis() - KEEP_DATA_DAYS * 24 * 60 * 60 * 1000L
            db.contentEventDao().deleteOlderThan(cutoff)
            return Result.success()
        }

        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val events = db.contentEventDao().getEventsSince(weekAgo)

        if (events.isNotEmpty()) {
            // Agregare frecvențe per categorie
            val counts = mutableMapOf<ContentCategory, Int>()
            events.forEach { event ->
                event.categories.split("|")
                    .mapNotNull { name -> runCatching { ContentCategory.valueOf(name) }.getOrNull() }
                    .forEach { cat -> counts[cat] = (counts[cat] ?: 0) + 1 }
            }

            val interests = counts.entries
                .filter { it.key.type == CategoryType.INTEREST }
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }

            val concerns = counts.entries
                .filter { it.key.type == CategoryType.CONCERN }
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }

            val maxConcern = concerns.maxByOrNull { it.concernLevel.ordinal }?.concernLevel
                ?: ConcernLevel.NONE

            val title = buildTitle(maxConcern)
            val message = buildMessage(interests, concerns, maxConcern)

            pushReportToFirestore(title, message, maxConcern, interests, concerns)

            // Well-being profile
            val familyId = OnboardingManager.getFamilyId(applicationContext)
            val childId  = OnboardingManager.getChildId(applicationContext)
            if (familyId != null && childId != null) {
                val profile = WellbeingCalculator.calculate(events)
                runCatching { FirebaseRepository.pushWellbeingProfile(familyId, childId, profile) }
            }
        }

        // Păstrăm datele 90 de zile pentru viitorul model de AI
        val cutoff = System.currentTimeMillis() - KEEP_DATA_DAYS * 24 * 60 * 60 * 1000L
        db.contentEventDao().deleteOlderThan(cutoff)

        return Result.success()
    }

    private fun buildTitle(concern: ConcernLevel): String = when (concern) {
        ConcernLevel.NONE, ConcernLevel.LOW -> "Raport săptămânal — totul pare în regulă"
        ConcernLevel.MEDIUM -> "Raport săptămânal — câteva lucruri de urmărit"
        ConcernLevel.HIGH -> "Raport săptămânal — merită o conversație"
        ConcernLevel.CRITICAL -> "Raport săptămânal — te rugăm să citești"
    }

    private fun buildMessage(
        interests: List<ContentCategory>,
        concerns: List<ContentCategory>,
        maxConcern: ConcernLevel
    ): String {
        val sb = StringBuilder()

        if (interests.isNotEmpty()) {
            val names = interests.joinToString(", ") { it.displayName }
            sb.append("Această săptămână copilul tău a arătat interes pentru $names.")
        }

        if (concerns.isNotEmpty()) {
            sb.append(" ")
            val mainConcern = concerns.first().displayName
            when (maxConcern) {
                ConcernLevel.LOW ->
                    sb.append("Am observat ocazional conținut despre $mainConcern — nimic îngrijorător deocamdată.")
                ConcernLevel.MEDIUM ->
                    sb.append("A apărut un interes față de \"$mainConcern\". Poate fi un moment bun pentru o conversație relaxată.")
                ConcernLevel.HIGH ->
                    sb.append("Am detectat în mod repetat conținut despre \"$mainConcern\". Îți recomandăm să discuți cu copilul tău.")
                ConcernLevel.CRITICAL ->
                    sb.append("Am detectat conținut serios legat de \"$mainConcern\". Te rugăm să discuți cu copilul tău cât mai curând.")
                else -> {}
            }
        }

        return sb.toString().trim().ifEmpty {
            "O săptămână liniștită, fără activitate notabilă detectată."
        }
    }

    private suspend fun pushReportToFirestore(
        title: String,
        message: String,
        maxConcern: ConcernLevel,
        interests: List<ContentCategory>,
        concerns: List<ContentCategory>
    ) {
        val familyId = OnboardingManager.getFamilyId(applicationContext) ?: return
        val childId = OnboardingManager.getChildId(applicationContext) ?: return
        runCatching {
            Firebase.firestore
                .collection("families").document(familyId)
                .collection("children").document(childId)
                .collection("reports")
                .add(mapOf<String, Any>(
                    "title" to title,
                    "message" to message,
                    "concernLevel" to maxConcern.name,
                    "timestamp" to System.currentTimeMillis(),
                    "interests" to interests.map { it.name },
                    "concerns" to concerns.map { it.name }
                ))
                .await()
        }.onFailure {
            android.util.Log.e("WeeklyReportWorker", "Eroare la trimitere raport Firestore: ${it.message}")
        }
    }

    private fun showNotification(title: String, message: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Rapoarte săptămânale",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Rezumat săptămânal despre conținutul consumat" }
            )
        }

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(NOTIF_ID, notif)
    }
}
