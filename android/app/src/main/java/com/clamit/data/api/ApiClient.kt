package com.clamit.data.api

import com.clamit.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Base URL is injected per build type (see build.gradle.kts). On the phone the
    // backend runs on the same device, so 127.0.0.1 is correct there.
    private const val DEFAULT_BASE_URL = BuildConfig.API_BASE_URL

    private val json = com.google.gson.GsonBuilder()
        .setLenient()
        .create()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(DEFAULT_BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(json))
        .build()

    val scheduleApi: ScheduleApi = retrofit.create(ScheduleApi::class.java)
}
