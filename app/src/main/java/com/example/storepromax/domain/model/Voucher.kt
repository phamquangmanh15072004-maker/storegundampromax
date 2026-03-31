package com.example.storepromax.domain.model

data class Voucher(
    val id: String = "",
    val code: String = "",
    val title: String = "",
    val type: String = "DISCOUNT",
    val discountType: String = "FIXED",
    val discountValue: Long = 0L,
    val maxDiscount: Long? = null,
    val minOrderValue: Long = 0L,
    val usageLimit: Int = 0,
    val usedCount: Int = 0,
    val expirationDate: Long = 0L
)