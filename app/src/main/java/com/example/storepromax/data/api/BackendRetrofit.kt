package com.example.storepromax.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BackendRetrofit {
    private const val BASE_URL = "https://edwina-nonredemptible-asthmatically.ngrok-free.dev/"
    val api: GunplaBackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GunplaBackendApi::class.java)
    }
}