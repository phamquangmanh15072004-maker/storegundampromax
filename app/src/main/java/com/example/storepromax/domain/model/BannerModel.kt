package com.example.storepromax.domain.model

import com.google.firebase.firestore.PropertyName

data class BannerModel(
    val id: String = "",
    val imageUrl: String = "",
    val headline: String = "",
    val subHeadline: String = "",
    val targetId: String = "",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    val priority: Int = 0
)