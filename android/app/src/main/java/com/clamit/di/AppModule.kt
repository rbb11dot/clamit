package com.clamit.di

import com.clamit.data.api.ApiClient
import com.clamit.data.repository.ScheduleRepository
import com.clamit.ui.schedule.ScheduleViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ApiClient.scheduleApi }
    single { ScheduleRepository(get()) }
    viewModel { ScheduleViewModel(get()) }
}
