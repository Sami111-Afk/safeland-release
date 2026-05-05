package com.sol.dopaminetrap.data

data class FamilySettings(
    // App access/throttle
    val tiktokEnabled: Boolean = true,
    val instagramEnabled: Boolean = true,
    val youtubeShortsEnabled: Boolean = true,
    val burstSizeKb: Int = 64,
    val pauseDurationMs: Long = 3000L,
    val tiktokLimitMinutes: Int = 0,
    val instagramLimitMinutes: Int = 0,
    val youtubeShortsLimitMinutes: Int = 0,
    // Feature toggles (parent controls what runs on child device)
    val contentDetectionEnabled: Boolean = true,
    val messagingMonitorEnabled: Boolean = true,
    val smsMonitorEnabled: Boolean = true,
    val weeklyReportEnabled: Boolean = true,
    val alertNotificationsEnabled: Boolean = true,
    // App control
    val appEnabled: Boolean = true,
    val lockEnabled: Boolean = false,
    val lockCode: String = "",
    // Child profile
    val childAge: Int = 13
) {
    companion object {
        fun fromMap(map: Map<String, Any>): FamilySettings = FamilySettings(
            tiktokEnabled             = map["tiktokEnabled"]             as? Boolean ?: true,
            instagramEnabled          = map["instagramEnabled"]          as? Boolean ?: true,
            youtubeShortsEnabled      = map["youtubeShortsEnabled"]      as? Boolean ?: true,
            burstSizeKb               = (map["burstSizeKb"]             as? Long)?.toInt() ?: 64,
            pauseDurationMs           = map["pauseDurationMs"]           as? Long ?: 3000L,
            tiktokLimitMinutes        = (map["tiktokLimitMinutes"]      as? Long)?.toInt() ?: 0,
            instagramLimitMinutes     = (map["instagramLimitMinutes"]   as? Long)?.toInt() ?: 0,
            youtubeShortsLimitMinutes = (map["youtubeShortsLimitMinutes"] as? Long)?.toInt() ?: 0,
            contentDetectionEnabled   = map["contentDetectionEnabled"]  as? Boolean ?: true,
            messagingMonitorEnabled   = map["messagingMonitorEnabled"]  as? Boolean ?: true,
            smsMonitorEnabled         = map["smsMonitorEnabled"]        as? Boolean ?: true,
            weeklyReportEnabled       = map["weeklyReportEnabled"]      as? Boolean ?: true,
            alertNotificationsEnabled = map["alertNotificationsEnabled"] as? Boolean ?: true,
            appEnabled                = map["appEnabled"]               as? Boolean ?: true,
            lockEnabled               = map["lockEnabled"]              as? Boolean ?: false,
            lockCode                  = map["lockCode"]                 as? String  ?: "",
            childAge                  = (map["childAge"]               as? Long)?.toInt() ?: 13
        )
    }

    fun toMap(): Map<String, Any> = mapOf(
        "tiktokEnabled"             to tiktokEnabled,
        "instagramEnabled"          to instagramEnabled,
        "youtubeShortsEnabled"      to youtubeShortsEnabled,
        "burstSizeKb"               to burstSizeKb,
        "pauseDurationMs"           to pauseDurationMs,
        "tiktokLimitMinutes"        to tiktokLimitMinutes,
        "instagramLimitMinutes"     to instagramLimitMinutes,
        "youtubeShortsLimitMinutes" to youtubeShortsLimitMinutes,
        "contentDetectionEnabled"   to contentDetectionEnabled,
        "messagingMonitorEnabled"   to messagingMonitorEnabled,
        "smsMonitorEnabled"         to smsMonitorEnabled,
        "weeklyReportEnabled"       to weeklyReportEnabled,
        "alertNotificationsEnabled" to alertNotificationsEnabled,
        "appEnabled"                to appEnabled,
        "lockEnabled"               to lockEnabled,
        "lockCode"                  to lockCode,
        "childAge"                  to childAge
    )
}
