package com.example.storepromax.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String = "",

    val role: String = "USER",

    val shippingAddress: String = "",

    @field:JvmField
    val isLocked: Boolean = false,

    val lockReason: String = "",
    val lockedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String = ""
)