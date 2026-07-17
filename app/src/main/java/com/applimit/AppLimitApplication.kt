package com.applimit

import android.app.Application
import com.applimit.data.repository.AppLimitRepository

class AppLimitApplication : Application() {
    lateinit var repository: AppLimitRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppLimitRepository.get(this)
    }
}
