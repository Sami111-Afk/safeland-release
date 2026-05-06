package com.sol.dopaminetrap

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object SessionTracker {

    private const val PREFS_NAME        = "session_tracker"
    private const val KEY_DATE          = "reset_date"
    private const val KEY_TIKTOK        = "tiktok_ms"
    private const val KEY_INSTA         = "instagram_ms"
    private const val KEY_INSTA_REELS   = "instagram_reels_ms"
    private const val KEY_YT_SHORTS     = "youtube_shorts_ms"
    private const val KEY_YT            = "youtube_ms"
    private const val KEY_FACEBOOK      = "facebook_ms"

    private val accumulatedMs = ConcurrentHashMap<ProtectedApp, Long>()
    private val sessionStart  = ConcurrentHashMap<ProtectedApp, Long>()
    private val limitMs       = ConcurrentHashMap<ProtectedApp, Long>()

    // ── Init / persist ────────────────────────────────────────────────────────

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        if (prefs.getString(KEY_DATE, "") != today) {
            prefs.edit()
                .putString(KEY_DATE,        today)
                .putLong(KEY_TIKTOK,        0)
                .putLong(KEY_INSTA,         0)
                .putLong(KEY_INSTA_REELS,   0)
                .putLong(KEY_YT_SHORTS,     0)
                .putLong(KEY_YT,            0)
                .putLong(KEY_FACEBOOK,      0)
                .apply()
        }

        accumulatedMs[ProtectedApp.TIKTOK]           = prefs.getLong(KEY_TIKTOK,       0)
        accumulatedMs[ProtectedApp.INSTAGRAM]        = prefs.getLong(KEY_INSTA,         0)
        accumulatedMs[ProtectedApp.INSTAGRAM_REELS]  = prefs.getLong(KEY_INSTA_REELS,   0)
        accumulatedMs[ProtectedApp.YOUTUBE_SHORTS]   = prefs.getLong(KEY_YT_SHORTS,     0)
        accumulatedMs[ProtectedApp.YOUTUBE]          = prefs.getLong(KEY_YT,            0)
        accumulatedMs[ProtectedApp.FACEBOOK]         = prefs.getLong(KEY_FACEBOOK,      0)
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_TIKTOK,      getTotalMs(ProtectedApp.TIKTOK))
            .putLong(KEY_INSTA,       getTotalMs(ProtectedApp.INSTAGRAM))
            .putLong(KEY_INSTA_REELS, getTotalMs(ProtectedApp.INSTAGRAM_REELS))
            .putLong(KEY_YT_SHORTS,   getTotalMs(ProtectedApp.YOUTUBE_SHORTS))
            .putLong(KEY_YT,          getTotalMs(ProtectedApp.YOUTUBE))
            .putLong(KEY_FACEBOOK,    getTotalMs(ProtectedApp.FACEBOOK))
            .apply()
    }

    // ── Setare limite ─────────────────────────────────────────────────────────

    fun setLimits(
        tiktokMin: Int,
        instagramMin: Int,
        instagramReelsMin: Int,
        youtubeShortsMin: Int,
        youtubeMin: Int,
        facebookMin: Int
    ) {
        limitMs[ProtectedApp.TIKTOK]          = tiktokMin        * 60_000L
        limitMs[ProtectedApp.INSTAGRAM]       = instagramMin     * 60_000L
        limitMs[ProtectedApp.INSTAGRAM_REELS] = instagramReelsMin * 60_000L
        limitMs[ProtectedApp.YOUTUBE_SHORTS]  = youtubeShortsMin * 60_000L
        limitMs[ProtectedApp.YOUTUBE]         = youtubeMin       * 60_000L
        limitMs[ProtectedApp.FACEBOOK]        = facebookMin      * 60_000L
    }

    // ── Sesiune ───────────────────────────────────────────────────────────────

    fun onEnter(app: ProtectedApp) {
        sessionStart.putIfAbsent(app, System.currentTimeMillis())
    }

    fun onExit(app: ProtectedApp, context: Context? = null) {
        val start = sessionStart.remove(app) ?: return
        accumulatedMs[app] = (accumulatedMs[app] ?: 0L) + (System.currentTimeMillis() - start)
        context?.let { persist(it) }
    }

    // ── Interogare ────────────────────────────────────────────────────────────

    fun getTotalMs(app: ProtectedApp): Long {
        val acc     = accumulatedMs[app] ?: 0L
        val current = sessionStart[app]?.let { System.currentTimeMillis() - it } ?: 0L
        return acc + current
    }

    fun getRemainingMinutes(app: ProtectedApp): Int? {
        val lim = limitMs[app] ?: 0L
        if (lim <= 0) return null
        return ((lim - getTotalMs(app)).coerceAtLeast(0) / 60_000).toInt()
    }

    /**
     * null  = fără throttle (viteză normală)
     * 0.5f  = ușor perceptibil     (80-87% din limită)
     * 0.18f = buffering clar        (87-93%)
     * 0.05f = aproape inutilizabil  (93-100%)
     * 0.02f = limită depășită
     *
     * Fără limită setată → 1f (throttle de bază mereu activ).
     */
    fun getThrottleMultiplier(app: ProtectedApp): Float? {
        val lim = limitMs[app] ?: 0L
        if (lim <= 0) return 1f
        val progress = getTotalMs(app).toFloat() / lim
        return when {
            progress < 0.80f -> null
            progress < 0.87f -> 0.50f
            progress < 0.93f -> 0.18f
            progress < 1.00f -> 0.05f
            else             -> 0.02f
        }
    }
}
