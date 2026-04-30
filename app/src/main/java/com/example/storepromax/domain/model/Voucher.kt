package com.example.storepromax.domain.model

import com.google.firebase.firestore.PropertyName

data class Voucher(
    val id: String = "",
    val code: String = "",
    val title: String = "",
    val type: String = "DISCOUNT",
    val discountType: String = "FIXED",
    val discountValue: Long = 0L,
    val maxDiscount: Long? = null,
    val minOrderValue: Long = 0L,
    val usageLimit: Long = 0L,
    val usedCount: Long = 0L,
    val startDate: Long = 0L,
    val expirationDate: Long = 0L,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = true
)