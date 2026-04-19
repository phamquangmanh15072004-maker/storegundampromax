package com.example.storepromax.domain.model

data class Order(
    val id: String = "",
    val userId: String = "",

    val items: List<CartItem> = emptyList(),

    val totalPrice: Long = 0,
    val totalCostPrice: Long = 0,
    val totalProfit: Long = 0,

    val receiverName: String = "",
    val receiverPhone: String = "",
    val address: String = "",

    val status: String = "PENDING",

    val paymentMethod: String = "COD",
    val paymentStatus: String = "UNPAID",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val cancelReason: String = "",
    val shippingMethod: String = "STANDARD",

    val discountCode: String? = null,
    val freeshipCode: String? = null,

    val refundBankBin: String? = null,
    val refundBankShortName: String? = null,
    val refundAccountNumber: String? = null,
    val refundAccountName: String? = null,
    val cancelledBy: String = "",
    val refundReceiptUrl: String? = null
)