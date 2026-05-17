package com.example.storepromax.presentation.admin.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.UserNotification
import com.example.storepromax.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // 🌟 THÊM IMPORT NÀY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // 🌟 THÊM IMPORT NÀY
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<UserNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    init {
        listenToNotifications()
    }

    private fun listenToNotifications() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            repository.getUserNotifications(uid).collect { notifList ->
                val filteredList = notifList.filter { notif ->
                    notif.type != "CHAT_MESSAGE" && notif.type != "CHAT"
                }

                _notifications.value = filteredList
                _unreadCount.value = filteredList.count { !it.isRead }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (notificationId.isEmpty()) return

        viewModelScope.launch {
            repository.markAsRead(uid, notificationId)
        }
    }

    fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.markAllAsRead(uid)
        }
    }

    fun deleteAllNotifications() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.deleteAllNotifications(uid)
        }
    }
    fun checkCanReviewOrder(orderId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("orders").document(orderId).get().await()
                if (!snapshot.exists()) {
                    onResult(false, "Đơn hàng không tồn tại!")
                    return@launch
                }
                val status = snapshot.getString("status") ?: ""
                if (status == "COMPLETED" || status == "DELIVERED" || status == "RETURN_REJECTED") {
                    onResult(true, "")
                } else {
                    onResult(false, "Đơn hàng đang xử lý hoàn trả/khiếu nại. Không thể đánh giá!")
                }
            } catch (e: Exception) {
                onResult(false, "Lỗi kiểm tra dữ liệu. Vui lòng thử lại!")
            }
        }
    }
    fun getCurrentOrderTabIndex(orderId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("orders").document(orderId).get().await()
                if (!snapshot.exists()) {
                    onResult(0)
                    return@launch
                }
                val status = snapshot.getString("status") ?: ""
                val tabIndex = when (status) {
                    "PENDING" -> 1
                    "CONFIRMED" -> 2
                    "SHIPPING" -> 3
                    "COMPLETED" -> 4
                    "RETURN_PENDING" -> 5
                    "RETURN_APPROVED" -> 6
                    "RETURNING" -> 7
                    "RETURN_REJECTED" -> 8
                    "CANCELLED" -> 9
                    "REFUNDING" -> 10
                    "REFUNDED" -> 11
                    else -> 0
                }
                onResult(tabIndex)
            } catch (e: Exception) {
                onResult(0)
            }
        }
    }
}
