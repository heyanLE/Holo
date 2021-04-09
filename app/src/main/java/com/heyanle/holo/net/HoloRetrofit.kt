package com.heyanle.holo.net

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor


object HoloRetrofit {

    //public ExecutorService normalExecutorService = Executors.newFixedThreadPool(5);
    var executorService: ScheduledExecutorService = ScheduledThreadPoolExecutor(8,
            DefaultThreadFactory())

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY //这里可以选择拦截级别
            addInterceptor(loggingInterceptor)
        }.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .client(client)
                .baseUrl("http://honglong.kicosoft.com")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    val holoService: HoloService by lazy {
        retrofit.create(HoloService::class.java)
    }




}