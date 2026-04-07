package com.example.storepromax.data.api

import com.example.storepromax.domain.model.VietQRResponse
import retrofit2.Response
import retrofit2.http.GET

interface VietQRService {
    @GET("v2/banks")
    suspend fun getBanks(): Response<VietQRResponse>
}