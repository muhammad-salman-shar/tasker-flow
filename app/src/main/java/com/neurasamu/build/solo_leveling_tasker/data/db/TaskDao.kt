package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.*
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity)
    @Query("UPDATE tasks SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM tasks") suspend fun deleteAll()
    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?
    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>
}
