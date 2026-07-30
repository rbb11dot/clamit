package com.clamit.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8080/"

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
