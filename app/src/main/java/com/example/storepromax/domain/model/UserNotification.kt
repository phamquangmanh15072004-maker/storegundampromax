package com.example.storepromax.domain.model

import com.google.firebase.firestore.PropertyName


data class UserNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val orderId: String? = null,
    val action: String? = null,
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)