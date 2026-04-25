package com.sol.dopaminetrap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_events")
data class ContentEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceApp: String,
    val rawText: String,
    val categories: String,   // pipe-separated enum names: "SPORT_MISCARE|DANS"
    val concernLevel: Int     // ConcernLevel.ordinal — 0=NONE … 4=CRITICAL
)
