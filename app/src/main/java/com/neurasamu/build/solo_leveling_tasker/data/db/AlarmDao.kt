package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.*
import com.neurasamu.build.solo_leveling_tasker.data.model.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Insert suspend fun insert(a: AlarmEntity): Long
    @Update suspend fun update(a: AlarmEntity)
    @Query("DELETE FROM alarms WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM alarms") suspend fun deleteAll()
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun observeAll(): Flow<List<AlarmEntity>>
    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AlarmEntity?
    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AlarmEntity>
}
