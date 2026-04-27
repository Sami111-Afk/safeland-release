package com.sol.dopaminetrap.data

data class FamilySettings(
    val tiktokEnabled: Boolean = true,
    val instagramEnabled: Boolean = true,
    val youtubeShortsEnabled: Boolean = true,
    val burstSizeKb: Int = 64,
    val pauseDurationMs: Long = 3000L,
    // Limită zilnică per app (minute). 0 = fără limită.
    val tiktokLimitMinutes: Int = 0,
    val instagramLimitMinutes: Int = 0,
    val youtubeShortsLimitMinutes: Int = 0
) {
    companion object {
        fun fromMap(map: Map<String, Any>): FamilySettings = FamilySettings(
            tiktokEnabled            = map["tiktokEnabled"]   as? Boolean ?: true,
            instagramEnabled         = map["instagramEnabled"] as? Boolean ?: true,
            youtubeShortsEnabled     = map["youtubeShortsEnabled"] as? Boolean ?: true,
            burstSizeKb              = (map["burstSizeKb"]    as? Long)?.toInt() ?: 64,
            pauseDurationMs          = map["pauseDurationMs"] as? Long ?: 3000L,
            tiktokLimitMinutes       = (map["tiktokLimitMinutes"] as? Long)?.toInt() ?: 0,
            instagramLimitMinutes    = (map["instagramLimitMinutes"] as? Long)?.toInt() ?: 0,
            youtubeShortsLimitMinutes= (map["youtubeShortsLimitMinutes"] as? Long)?.toInt() ?: 0
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
        "youtubeShortsLimitMinutes" to youtubeShortsLimitMinutes
    )
}
