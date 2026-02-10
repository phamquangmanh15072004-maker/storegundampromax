package com.example.storepromax.presentation.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper // Import file Helper vừa tạo
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.repository.OrderRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

object OrderStatus {
    const val PENDING = "PENDING"
    const val CONFIRMED = "CONFIRMED"
    const val SHIPPING = "SHIPPING"
    const val DELIVERED = "DELIVERED"
    const val CANCELLED = "CANCELLED"
}

@HiltViewModel
class AdminOrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
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
                        (order.receiverName).contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }


    fun updateStatus(orderId: String, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = when (currentStatus) {
                OrderStatus.PENDING -> OrderStatus.CONFIRMED
                OrderStatus.CONFIRMED -> OrderStatus.SHIPPING
                OrderStatus.SHIPPING -> OrderStatus.DELIVERED
                else -> return@launch
            }

            if (currentStatus == OrderStatus.PENDING && nextStatus == OrderStatus.CONFIRMED) {
                val currentOrder = _allOrders.first().find { it.id == orderId }
                currentOrder?.items?.forEach { cartItem ->
                    productRepository.updateProductStock(cartItem.product.id, -cartItem.quantity)
                }
            }
            orderRepository.updateOrderStatus(orderId, nextStatus)

            val currentOrder = _allOrders.first().find { it.id == orderId }
            if (currentOrder != null) {
                sendNotificationToUser(currentOrder.userId, currentOrder.id, nextStatus)
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String = "Admin hủy đơn") {
        viewModelScope.launch {
            val currentOrder = _allOrders.first().find { it.id == orderId }

            if (currentOrder != null &&
                (currentOrder.status == OrderStatus.CONFIRMED || currentOrder.status == OrderStatus.SHIPPING)) {
                currentOrder.items.forEach { cartItem ->
                    productRepository.updateProductStock(cartItem.product.id, cartItem.quantity)
                }
            }
            orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
            if (currentOrder != null) {
                sendNotificationToUser(currentOrder.userId, currentOrder.id, OrderStatus.CANCELLED, reason)
            }
        }
    }
    private fun sendNotificationToUser(userId: String, orderId: String, status: String, reason: String = "") {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                if (!token.isNullOrEmpty()) {
                    viewModelScope.launch {
                        NotificationHelper.sendOrderNotification(
                            context = context,
                            userToken = token,
                            orderId = orderId,
                            status = status,
                            cancelReason = reason
                        )
                    }
                }
            }
    }
}