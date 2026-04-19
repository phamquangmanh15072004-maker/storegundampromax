package com.example.storepromax.domain.model

import java.util.UUID

data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val product: Product = Product(),
    var quantity: Int = 0,
    var isSelected: Boolean = false,
    var purchasedPrice: Long = 0,
    var costPriceAtPurchase: Long = 0
) {

    val liveTotalPrice: Long
        get() = product.price * quantity

    val snapshotTotalPrice: Long
        get() = purchasedPrice * quantity

    val itemProfit: Long
        get() = (purchasedPrice - costPriceAtPurchase) * quantity
}