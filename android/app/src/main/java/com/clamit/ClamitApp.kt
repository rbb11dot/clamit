package com.clamit

import android.app.Application
import android.util.Log
import com.clamit.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ClamitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            startKoin {
                androidContext(this@ClamitApp)
                modules(appModule)
            }
        } catch (e: Exception) {
            Log.e("ClamitApp", "Koin init failed", e)
        }
    }
}
