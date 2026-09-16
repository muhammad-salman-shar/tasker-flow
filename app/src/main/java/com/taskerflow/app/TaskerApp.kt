package com.taskerflow.app

import android.app.Application
import com.taskerflow.app.data.db.AppDatabase
import com.taskerflow.app.data.repo.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskerApp : Application() {
    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = TaskRepository(db)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.ensureStatsRow()
        }
    }
}
