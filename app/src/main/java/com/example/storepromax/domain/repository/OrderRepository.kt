package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    suspend fun createOrder(
        order: Order,
        discountCode: String? = null,
        freeshipCode: String? = null
    ): Result<String>

    suspend fun cancelOrder(
        orderId: String,
        reason: String,
        isPaid: Boolean,
        bankBin: String?,
        bankShortName: String?,
        accountNumber: String?,
        accountName: String?
    )
    fun getAllOrders(): Flow<List<Order>>
    fun getOrderById(orderId: String): Flow<Order?>
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Boolean>
    suspend fun confirmRefundWithReceipt(orderId: String, receiptUrl: String): Result<Boolean>
    suspend fun requestReturnRefund(
        orderId: String,
        reason: String,
        description: String,
        images: List<String>,
        bankBin: String?,
        bankShortName: String?,
        accountNumber: String?,
        accountName: String?
    )
    suspend fun submitReturnTrackingCode(orderId: String, trackingCode: String): Result<Boolean>
}