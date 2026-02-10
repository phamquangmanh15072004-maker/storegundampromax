package com.example.storepromax.domain.model

import com.google.firebase.firestore.PropertyName

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",

    val price: Long = 0,
    val originalPrice: Long = 0,
    val stock:Int = 0,
    @get:PropertyName("isNew")
    val isNew: Boolean = false,
    @get:PropertyName("isActive")
    val isActive: Boolean = true,

    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val model3DUrl: String? = null,
    val category: String = "",

    val rating: Double = 0.0,
    val sold: Int = 0,
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) { fun getDiscountPercentage(): Int {
        if (originalPrice > 0 && price < originalPrice) {
            return ((originalPrice - price).toDouble() / originalPrice * 100).toInt()
        }
        return 0
    }
}