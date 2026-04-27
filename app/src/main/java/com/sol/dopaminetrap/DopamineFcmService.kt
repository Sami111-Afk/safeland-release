package com.sol.dopaminetrap

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DopamineFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "dopamine_alerts"
        private const val CHANNEL_ID_REPORTS = "dopamine_reports"
        private const val NOTIF_ID_ALERT  = 101
        private const val NOTIF_ID_REPORT = 102

        /**
         * Salveaza token-ul FCM al parintelui in Firestore.
         * Apelat la startup din MainActivity cand modul e PARENT.
         */
        fun saveParentToken(familyId: String) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val token = Firebase.messaging.token.await()
                    Firebase.firestore
                        .collection("families")
                        .document(familyId)
                        .collection("config")
                        .document("parent")
                        .set(mapOf("fcmToken" to token))
                        .await()
                }
            }
        }
    }

    // Apelat cand token-ul FCM se schimba — actualizam Firestore
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val context = applicationContext
        val mode = OnboardingManager.getMode(context) ?: return
        if (mode != OnboardingManager.DeviceMode.PARENT) return
        val familyId = OnboardingManager.getFamilyId(context) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                Firebase.firestore
                    .collection("families")
                    .document(familyId)
                    .collection("config")
                    .document("parent")
                    .set(mapOf("fcmToken" to token))
                    .await()
            }
        }
    }

    // Apelat mereu (foreground + background) deoarece Cloud Functions trimit doar data, fara notification field
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: message.notification?.title ?: "Alertă Safeland"
        val body  = message.data["body"]  ?: message.notification?.body  ?: return
        val type  = message.data["type"]  ?: "alert"

        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, type: String) {
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Alerte parinti", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerte critice despre continutul consumat de copil"
                    enableVibration(true)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID_REPORTS, "Rapoarte saptamanale", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Rezumat saptamanal despre continutul consumat"
                }
            )
        }

        val isReport = type == "weekly_report"
        val channelId = if (isReport) CHANNEL_ID_REPORTS else CHANNEL_ID
        val notifId   = if (isReport) NOTIF_ID_REPORT else NOTIF_ID_ALERT

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(if (isReport) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notifId, notif)
    }
}
