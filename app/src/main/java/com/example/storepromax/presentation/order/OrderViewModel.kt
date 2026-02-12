package com.example.storepromax.presentation.order

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    val orders: StateFlow<List<Order>> = orderRepository.getOrders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                orderRepository.cancelOrder(orderId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun placeOrder(order: Order, context: Context) {
        viewModelScope.launch {
            val result = orderRepository.createOrder(order)
            result.onSuccess { newOrderId ->
                NotificationHelper.sendOrderNotificationToAdmin(
                    context = context,
                    orderId = newOrderId,
                    totalAmount = order.totalPrice.toDouble()
                )
            }

            result.onFailure { exception ->
            }
        }
    }
}