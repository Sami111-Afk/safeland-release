package com.sol.dopaminetrap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.sol.dopaminetrap.analysis.ContentAnalyzer

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val text = messages.joinToString(" ") { it.messageBody ?: "" }.trim()
        if (text.isBlank()) return

        Log.d("SmsBroadcastReceiver", "SMS primit: ${text.take(50)}...")
        ContentAnalyzer.analyze(context, text, "SMS")
    }
}
