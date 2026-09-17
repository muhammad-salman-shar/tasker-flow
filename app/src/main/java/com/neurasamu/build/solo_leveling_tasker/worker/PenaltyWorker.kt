package com.neurasamu.build.solo_leveling_tasker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neurasamu.build.solo_leveling_tasker.TaskerApp

class PenaltyWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = (applicationContext as TaskerApp).repository
            repo.applyPenaltiesTick()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "tasker_penalty_tick"
    }
}
