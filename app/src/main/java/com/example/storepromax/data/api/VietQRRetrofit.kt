package com.example.storepromax.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object VietQRRetrofit {
    private const val BASE_URL = "https://api.vietqr.io/"

    val api: VietQRService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VietQRService::class.java)
    }
}