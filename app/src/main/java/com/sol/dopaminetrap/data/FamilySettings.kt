package com.sol.dopaminetrap.data

data class FamilySettings(
    val tiktokEnabled: Boolean = true,
    val instagramEnabled: Boolean = true,
    val youtubeShortsEnabled: Boolean = true,
    val burstSizeKb: Int = 64,
    val pauseDurationMs: Long = 3000L
) {
    companion object {
        fun fromMap(map: Map<String, Any>): FamilySettings = FamilySettings(
            tiktokEnabled = map["tiktokEnabled"] as? Boolean ?: true,
            instagramEnabled = map["instagramEnabled"] as? Boolean ?: true,
            youtubeShortsEnabled = map["youtubeShortsEnabled"] as? Boolean ?: true,
            burstSizeKb = (map["burstSizeKb"] as? Long)?.toInt() ?: 64,
            pauseDurationMs = map["pauseDurationMs"] as? Long ?: 3000L
        )
    }

    fun toMap(): Map<String, Any> = mapOf(
        "tiktokEnabled" to tiktokEnabled,
        "instagramEnabled" to instagramEnabled,
        "youtubeShortsEnabled" to youtubeShortsEnabled,
        "burstSizeKb" to burstSizeKb,
        "pauseDurationMs" to pauseDurationMs
    )
}
