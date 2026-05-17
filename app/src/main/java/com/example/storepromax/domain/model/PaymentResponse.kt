package com.example.storepromax.domain.model

data class PaymentRequest(
    val orderId: String,
    val amount: Long,
    val description: String
)
data class PaymentResponse(
    val success: Boolean,
    val bin: String?,
    val accountNumber: String?,
    val checkoutUrl: String?,
    val description: String?,
    val orderShortCode: String? = null,
    val itemSummary: String? = null
)
