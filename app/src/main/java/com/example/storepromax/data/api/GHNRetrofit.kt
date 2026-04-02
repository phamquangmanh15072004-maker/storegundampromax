package com.example.storepromax.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GHNRetrofit {
    private const val BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api/"
    private const val TOKEN = "9e6845d5-2dd1-11f1-b9c2-2615c76d97a1"
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Token", TOKEN)
            .build()
        chain.proceed(request)
    }.apply {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        addInterceptor(logging)
    }.build()

    val api: GHNApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GHNApi::class.java)
    }
}