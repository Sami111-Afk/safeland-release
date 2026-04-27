package com.sol.dopaminetrap.data

data class WellbeingProfile(
    val emotionalScore: Float,   // 0-1, mai mare = mai bine
    val socialScore: Float,      // 0-1
    val selfImageScore: Float,   // 0-1
    val academicScore: Float,    // 0-1
    val riskFlags: List<String>, // displayName ale categoriilor CRITICAL detectate
    val totalEvents: Int,
    val weekLabel: String
)
