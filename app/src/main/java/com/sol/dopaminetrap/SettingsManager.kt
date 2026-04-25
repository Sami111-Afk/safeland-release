package com.sol.dopaminetrap

import android.content.Context

enum class ProtectedApp(
    val displayName: String,
    val packages: List<String>,
    val prefKey: String
) {
    TIKTOK(
        "TikTok",
        listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme"),
        "protect_tiktok"
    ),
    INSTAGRAM(
        "Instagram",
        listOf("com.instagram.android"),
        "protect_instagram"
    ),
    YOUTUBE_SHORTS(
        "YouTube Shorts",
        listOf("com.google.android.youtube"),
        "protect_youtube_shorts"
    )
}

object SettingsManager {
    private const val PREFS_NAME = "dopamine_settings"

    fun isEnabled(context: Context, app: ProtectedApp): Boolean =
        prefs(context).getBoolean(app.prefKey, true)

    fun setEnabled(context: Context, app: ProtectedApp, enabled: Boolean) =
        prefs(context).edit().putBoolean(app.prefKey, enabled).apply()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
