package com.clamit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.clamit.ui.schedule.ScheduleScreen
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinContext
import org.koin.core.context.startKoin
import com.clamit.di.appModule
import com.clamit.ui.schedule.ScheduleViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        setContent {
            KoinContext {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ScheduleScreen(
                            viewModel = ScheduleViewModel(
                                repository = com.clamit.data.repository.ScheduleRepository(
                                    api = com.clamit.data.api.ApiClient.scheduleApi
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
