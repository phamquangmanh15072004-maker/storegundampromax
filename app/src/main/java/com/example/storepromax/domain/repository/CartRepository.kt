package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(): Flow<List<CartItem>>

    suspend fun addToCart(product: Product,quantity:Int)
    suspend fun removeFromCart(productId: String)
    suspend fun updateQuantity(productId: String, newQuantity: Int)
    suspend fun updateSelection(productId: String, isSelected: Boolean)
    suspend fun clearCart()
    suspend fun decreaseStock(productId: String, quantity: Int)
}