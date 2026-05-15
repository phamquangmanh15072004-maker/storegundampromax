package com.example.storepromax.data.api

import com.example.storepromax.domain.model.FcmRequest
import com.example.storepromax.domain.model.PaymentRequest
import com.example.storepromax.domain.model.PaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GunplaBackendApi {
    @POST("create-payment-link")
    suspend fun createPaymentLink(@Body request: PaymentRequest): Response<PaymentResponse>

    @POST("api/send-fcm")
    suspend fun sendFcmNotification(@Body request: FcmRequest): Response<Unit>

    @POST("api/ai/chat")
    suspend fun sendAiChatMessage(
        @Header("Authorization") authorization: String,
        @Body request: AiChatRequest
    ): Response<AiChatResponse>
}

data class AiChatRequest(
    val message: String,
    val imageUrl: String? = null,
    val history: List<AiChatHistoryItem> = emptyList()
)

data class AiChatHistoryItem(
    val role: String,
    val content: String
)

data class AiChatResponse(
    val success: Boolean = false,
    val text: String? = null,
    val errorCode: String? = null,
    val message: String? = null
)
