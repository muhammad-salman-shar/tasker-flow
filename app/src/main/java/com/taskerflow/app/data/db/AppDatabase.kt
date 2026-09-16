package com.taskerflow.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.taskerflow.app.data.model.*

@Database(
    entities = [
        TaskEntity::class,
        OccurrenceEntity::class,
        EventEntity::class,
        TaskDebtEntity::class,
        RecoveryQuestEntity::class,
        PlayerStatsEntity::class
    ],
    version = 2,
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
                    "tasker_flow.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
