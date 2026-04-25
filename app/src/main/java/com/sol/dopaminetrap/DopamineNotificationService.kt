package com.sol.dopaminetrap

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sol.dopaminetrap.analysis.ContentAnalyzer

/**
 * Ascultă notificările de la aplicații de mesagerie.
 * Textul din preview-ul notificării e trimis la ContentAnalyzer — același model AI.
 * Impact baterie: zero (event-driven, nu rulează continuu).
 */
class DopamineNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "DopamineNotif"

        private val MONITORED_PACKAGES = mapOf(
            "com.whatsapp"                    to "WhatsApp",
            "com.whatsapp.w4b"                to "WhatsApp Business",
            "org.telegram.messenger"          to "Telegram",
            "org.telegram.messenger.web"      to "Telegram",
            "com.facebook.orca"               to "Messenger",
            "org.thoughtcrime.securesms"      to "Signal",
            "com.viber.voip"                  to "Viber",
            "com.snapchat.android"            to "Snapchat",
            "com.instagram.android"           to "Instagram DM"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val appName = MONITORED_PACKAGES[pkg] ?: return

        val extras = sbn.notification?.extras ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val content = if (bigText.length > text.length) bigText else text
        if (content.isBlank()) return

        Log.d(TAG, "Notificare $appName: ${content.take(50)}...")
        ContentAnalyzer.analyze(this, content, appName)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
