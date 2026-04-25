package com.sol.dopaminetrap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContentEventDao {

    @Insert
    suspend fun insert(event: ContentEvent)

    @Query("SELECT * FROM content_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<ContentEvent>

    @Query("SELECT * FROM content_events ORDER BY timestamp DESC")
    suspend fun getAll(): List<ContentEvent>

    @Query("DELETE FROM content_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
