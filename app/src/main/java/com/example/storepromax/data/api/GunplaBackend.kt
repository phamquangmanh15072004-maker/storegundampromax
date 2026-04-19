package com.example.storepromax.data.api

import com.example.storepromax.domain.model.FcmRequest
import com.example.storepromax.domain.model.PaymentRequest
import com.example.storepromax.domain.model.PaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GunplaBackendApi {
    @POST("create-payment-link")
    suspend fun createPaymentLink(@Body request: PaymentRequest): Response<PaymentResponse>

    @POST("api/send-fcm")
    suspend fun sendFcmNotification(@Body request: FcmRequest): Response<Unit>
}

