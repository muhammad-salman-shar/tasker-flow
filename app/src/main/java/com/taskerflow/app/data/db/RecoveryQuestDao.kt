package com.taskerflow.app.data.db

import androidx.room.*
import com.taskerflow.app.data.model.RecoveryQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryQuestDao {
    @Insert suspend fun insert(q: RecoveryQuestEntity): Long
    @Update suspend fun update(q: RecoveryQuestEntity)
    @Query("SELECT * FROM recovery_quests WHERE status = 'ACTIVE' ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<RecoveryQuestEntity>>
    @Query("SELECT * FROM recovery_quests WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(): RecoveryQuestEntity?
}
