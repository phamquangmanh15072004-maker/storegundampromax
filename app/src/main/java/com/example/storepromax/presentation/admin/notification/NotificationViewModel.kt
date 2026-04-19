package com.example.storepromax.presentation.admin.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.UserNotification
import com.example.storepromax.domain.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val auth: FirebaseAuth
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
                _notifications.value = notifList
                _unreadCount.value = notifList.count { !it.isRead }
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
}