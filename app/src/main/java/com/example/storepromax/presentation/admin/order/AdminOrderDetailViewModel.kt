package com.example.storepromax.presentation.admin.order

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminOrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    val order = orderRepository.getOrderById(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus).onSuccess {
                val currentOrder = order.value
                if (currentOrder != null) {
                    sendNotificationToUser(currentOrder.userId, currentOrder.id, newStatus)
                }
            }
        }
    }

    fun cancelOrder(reason: String) {
        viewModelScope.launch {
            orderRepository.cancelOrder(orderId, reason)
            val currentOrder = order.value
            if (currentOrder != null) {
                sendNotificationToUser(currentOrder.userId, currentOrder.id, "CANCELLED", reason)
            }
        }
    }

    private fun sendNotificationToUser(userId: String, orderId: String, status: String, reason: String = "") {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                val token = document.getString("fcmToken") ?: ""
                NotificationHelper.sendOrderNotification(
                    context = context,
                    userToken = token,
                    userId = userId,
                    orderId = orderId,
                    status = status,
                    cancelReason = reason
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}