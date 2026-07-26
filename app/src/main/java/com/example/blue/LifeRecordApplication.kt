package com.example.blue

import android.app.Application
import com.example.blue.core.AppContainer
import com.example.blue.core.DefaultAppContainer

class LifeRecordApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
