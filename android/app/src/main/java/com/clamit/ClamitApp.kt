package com.clamit

import android.app.Application
import com.clamit.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ClamitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // No try/catch: if Koin fails, nothing works — fail fast with the real
        // error instead of a confusing "viewModel() not found" crash later.
        startKoin {
            androidContext(this@ClamitApp)
            modules(appModule)
        }
    }
}
