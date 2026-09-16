package com.taskerflow.app.data.db

import androidx.room.*
import com.taskerflow.app.data.model.TaskDebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDebtDao {
    @Insert suspend fun insert(d: TaskDebtEntity): Long
    @Update suspend fun update(d: TaskDebtEntity)
    @Query("SELECT * FROM task_debt WHERE resolved = 0 ORDER BY debtCreatedAt ASC")
    fun observeActive(): Flow<List<TaskDebtEntity>>
    @Query("SELECT * FROM task_debt WHERE occurrenceId = :occId AND resolved = 0 LIMIT 1")
    suspend fun getActiveForOccurrence(occId: Long): TaskDebtEntity?
    @Query("SELECT COUNT(*) FROM task_debt WHERE resolved = 0")
    fun observeActiveCount(): Flow<Int>
}
