package com.neurasamu.build.solo_leveling_tasker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neurasamu.build.solo_leveling_tasker.data.block.BlockedAppsRepository
import com.neurasamu.build.solo_leveling_tasker.data.db.AppDatabase
import com.neurasamu.build.solo_leveling_tasker.data.profile.ProfileRepository
import com.neurasamu.build.solo_leveling_tasker.data.repo.TaskRepository
import com.neurasamu.build.solo_leveling_tasker.worker.NotificationHelper
import com.neurasamu.build.solo_leveling_tasker.worker.PenaltyWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TaskerApp : Application() {
    lateinit var repository: TaskRepository
        private set
    lateinit var profileRepository: ProfileRepository
        private set
    lateinit var blockedAppsRepository: BlockedAppsRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = TaskRepository(db, this)
        profileRepository = ProfileRepository(this)
        blockedAppsRepository = BlockedAppsRepository(this)
        NotificationHelper.ensureChannel(this)

        appScope.launch { repository.ensureStatsRow() }

        schedulePenaltyWorker()
        startForegroundPenaltyTicker()
    }

    private fun schedulePenaltyWorker() {
        val request = PeriodicWorkRequestBuilder<PenaltyWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PenaltyWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun startForegroundPenaltyTicker() {
        appScope.launch {
            while (true) {
                try { repository.applyPenaltiesTick() } catch (_: Exception) {}
                delay(5_000L)
            }
        }
    }
}
