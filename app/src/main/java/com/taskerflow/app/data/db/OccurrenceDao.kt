package com.taskerflow.app.data.db

import androidx.room.*
import com.taskerflow.app.data.model.OccurrenceEntity
import com.taskerflow.app.data.model.OccurrenceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OccurrenceDao {
    @Insert suspend fun insert(o: OccurrenceEntity): Long
    @Insert suspend fun insertAll(list: List<OccurrenceEntity>): List<Long>
    @Update suspend fun update(o: OccurrenceEntity)
    @Query("SELECT * FROM occurrences WHERE taskId = :taskId ORDER BY scheduledAt ASC")
    fun observeForTask(taskId: Long): Flow<List<OccurrenceEntity>>
    @Query("SELECT * FROM occurrences WHERE scheduledAt BETWEEN :from AND :to ORDER BY scheduledAt ASC")
    fun observeRange(from: Long, to: Long): Flow<List<OccurrenceEntity>>
    @Query("SELECT * FROM occurrences WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): OccurrenceEntity?
    @Query("SELECT * FROM occurrences WHERE status = 'PENDING' AND deadlineAt < :now")
    suspend fun getOverdue(now: Long): List<OccurrenceEntity>
    @Query("SELECT * FROM occurrences WHERE status = 'PENDING'")
    suspend fun getAllPending(): List<OccurrenceEntity>
}
