package com.example.storepromax.domain.model

import com.google.firebase.firestore.PropertyName

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String = "",

    val role: String = "USER",

    val shippingAddress: String = "",

    val specificAddress: String = "",
    val provinceId: Int = 0,
    val districtId: Int = 0,
    val wardCode: String = "",

    val provinceName: String = "",
    val districtName: String = "",
    val wardName: String = "",

    @get:PropertyName("isLocked")
    @set:PropertyName("isLocked")
    var isLocked: Boolean = false,

    val lockReason: String = "",
    val lockedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String = "",
    val lastActive: Long = System.currentTimeMillis()
)