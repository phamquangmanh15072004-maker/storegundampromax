package com.example.storepromax.domain.model

import java.util.UUID

data class CartItem(
    val id: String = UUID.randomUUID().toString(),

    val product: Product = Product(),
    var quantity: Int = 0,
    var isSelected: Boolean = false
) {
    val totalPrice: Long
        get() = product.price * quantity
}