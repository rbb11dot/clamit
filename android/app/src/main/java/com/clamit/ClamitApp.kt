package com.clamit

import android.app.Application
import com.clamit.di.appModule
import org.koin.core.context.startKoin

class ClamitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ClamitApp)
            modules(appModule)
        }
    }
}
