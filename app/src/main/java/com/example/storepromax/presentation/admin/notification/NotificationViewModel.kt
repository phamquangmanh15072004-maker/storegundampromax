package com.example.storepromax.presentation.admin.notification

import androidx.lifecycle.ViewModel
import com.example.storepromax.domain.model.UserNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
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

        firestore.collection("users").document(uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserNotification::class.java)?.copy(id = doc.id)
                    }

                    _notifications.value = notifList
                    _unreadCount.value = notifList.count { !it.isRead }
                }
            }
    }

    fun markAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (notificationId.isEmpty()) return

        firestore.collection("users").document(uid)
            .collection("notifications").document(notificationId)
            .update("isRead", true)
    }

    fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        val unreadNotifs = _notifications.value.filter { !it.isRead }

        val batch = firestore.batch()
        unreadNotifs.forEach { notif ->
            if (notif.id.isNotEmpty()) {
                val ref = firestore.collection("users").document(uid)
                    .collection("notifications").document(notif.id)
                batch.update(ref, "isRead", true)
            }
        }
        batch.commit()
    }

    fun deleteAllNotifications() {
        val uid = auth.currentUser?.uid ?: return
        val allNotifs = _notifications.value

        val batch = firestore.batch()
        allNotifs.forEach { notif ->
            if (notif.id.isNotEmpty()) {
                val ref = firestore.collection("users").document(uid)
                    .collection("notifications").document(notif.id)
                batch.delete(ref)
            }
        }
        batch.commit()
    }
}