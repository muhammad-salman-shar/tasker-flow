package com.neurasamu.build.solo_leveling_tasker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.neurasamu.build.solo_leveling_tasker.data.model.*

@Database(
    entities = [
        TaskEntity::class,
        OccurrenceEntity::class,
        EventEntity::class,
        TaskDebtEntity::class,
        RecoveryQuestEntity::class,
        PlayerStatsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun occurrenceDao(): OccurrenceDao
    abstract fun eventDao(): EventDao
    abstract fun taskDebtDao(): TaskDebtDao
    abstract fun recoveryQuestDao(): RecoveryQuestDao
    abstract fun playerStatsDao(): PlayerStatsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "solo_leveling_tasker.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
