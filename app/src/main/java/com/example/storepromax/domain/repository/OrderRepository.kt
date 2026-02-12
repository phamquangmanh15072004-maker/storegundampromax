package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    suspend fun createOrder(order: Order): Result<String>
    suspend fun cancelOrder(orderId: String)
    fun getAllOrders(): Flow<List<Order>>
    fun getOrderById(orderId: String): Flow<Order?>
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Boolean>
}