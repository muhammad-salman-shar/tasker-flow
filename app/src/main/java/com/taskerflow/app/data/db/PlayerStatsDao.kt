package com.taskerflow.app.data.db

import androidx.room.*
import com.taskerflow.app.data.model.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    fun observe(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    suspend fun get(): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(s: PlayerStatsEntity)

    @Update suspend fun update(s: PlayerStatsEntity)
}
