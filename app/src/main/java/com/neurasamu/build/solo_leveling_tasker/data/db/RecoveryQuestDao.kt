package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.*
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryQuestDao {
    @Insert suspend fun insert(q: RecoveryQuestEntity): Long
    @Update suspend fun update(q: RecoveryQuestEntity)
    @Query("DELETE FROM recovery_quests WHERE taskId = :taskId") suspend fun deleteByTask(taskId: Long)
    @Query("DELETE FROM recovery_quests") suspend fun deleteAll()
    @Query("SELECT * FROM recovery_quests WHERE status = 'ACTIVE' ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<RecoveryQuestEntity>>
    @Query("SELECT * FROM recovery_quests WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(): RecoveryQuestEntity?
}
