package com.example.storepromax.presentation.admin.order

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

object OrderStatus {
    const val PENDING = "PENDING"
    const val CONFIRMED = "CONFIRMED"
    const val SHIPPING = "SHIPPING"
    const val DELIVERED = "DELIVERED"
    const val CANCELLED = "CANCELLED"
    const val REFUNDING = "REFUNDING"
    const val REFUNDED = "REFUNDED"
}

@HiltViewModel
class AdminOrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _allOrders = orderRepository.getAllOrders()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val orders = combine(_allOrders, _searchQuery) { orders, query ->
        if (query.isBlank()) {
            orders
        } else {
            orders.filter { order ->
                order.id.contains(query, ignoreCase = true) ||
                        order.receiverName.contains(query, ignoreCase = true) ||
                        order.receiverPhone.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    fun updateStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus).onSuccess {
                val currentOrder = _allOrders.first().find { it.id == orderId }
                if (currentOrder != null) {
                    sendNotificationToUser(currentOrder.userId, currentOrder.id, newStatus)
                }
            }
        }
    }
    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            val currentOrder = _allOrders.first().find { it.id == orderId } ?: return@launch
            val isPaid = currentOrder.paymentStatus == "PAID"

            orderRepository.cancelOrder(
                orderId = orderId,
                reason = "Shop hủy: $reason",
                isPaid = isPaid,
                bankBin = null,
                bankShortName = null,
                accountNumber = null,
                accountName = null
            )
            val notifyStatus = if (isPaid) OrderStatus.REFUNDING else OrderStatus.CANCELLED
            sendNotificationToUser(currentOrder.userId, currentOrder.id, notifyStatus, reason)
        }
    }
    fun confirmRefund(orderId: String) {
        viewModelScope.launch {
            val currentOrder = _allOrders.first().find { it.id == orderId } ?: return@launch
            if (currentOrder.status == OrderStatus.REFUNDING) {
                orderRepository.updateOrderStatus(orderId, OrderStatus.REFUNDED).onSuccess {
                    sendNotificationToUser(
                        userId = currentOrder.userId,
                        orderId = currentOrder.id,
                        status = OrderStatus.REFUNDED,
                        reason = "Tiền đã được hoàn về tài khoản ${currentOrder.refundBankShortName} của bạn."
                    )
                }
            }
        }
    }

    private fun sendNotificationToUser(userId: String, orderId: String, status: String, reason: String = "") {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                val token = document.getString("fcmToken") ?: ""
                if (token.isNotEmpty()) {
                    NotificationHelper.sendOrderNotification(
                        context = context,
                        userToken = token,
                        userId = userId,
                        orderId = orderId,
                        status = status,
                        cancelReason = reason
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}