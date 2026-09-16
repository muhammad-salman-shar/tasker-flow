package com.taskerflow.app.data.db

import androidx.room.*
import com.taskerflow.app.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert suspend fun insert(e: EventEntity): Long
    @Query("SELECT * FROM events WHERE occurrenceId = :occId ORDER BY timestamp ASC")
    fun observeForOccurrence(occId: Long): Flow<List<EventEntity>>
    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<EventEntity>>
}
