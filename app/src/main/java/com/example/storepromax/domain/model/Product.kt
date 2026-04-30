package com.example.storepromax.domain.model

import com.google.firebase.firestore.PropertyName

data class Product(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    val description: String = "",

    val costPrice: Long = 0,
    val price: Long = 0,
    val originalPrice: Long = 0,

    val stock: Int = 0,
    @get:PropertyName("isActive")
    val isActive: Boolean = true,
    @get:PropertyName("isFeatured")
    val isFeatured: Boolean = false,

    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val model3DUrl: String? = null,
    val category: String = "",
    val name_lowercase: String = "",
    val rating: Double = 0.0,
    val sold: Int = 0,
    val weight: Int = 0,
    val isLowStockNotified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val has3D: Boolean = false
) {
    fun getDiscountPercentage(): Int {
        if (originalPrice > 0 && price < originalPrice) {
            return ((originalPrice - price).toDouble() / originalPrice * 100).toInt()
        }
        return 0
    }

    fun isNewProduct(): Boolean {
        val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
        val currentTime = System.currentTimeMillis()
        return (currentTime - createdAt) <= sevenDaysInMillis
    }
    fun isHotProduct(): Boolean {
        return sold > 50 || isFeatured
    }
}