package com.example.storepromax.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _isUserBanned = MutableStateFlow(false)
    val isUserBanned = _isUserBanned.asStateFlow()

    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount = _unreadChatCount.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null

    private val _scrollToTopEvent = MutableSharedFlow<String>()
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    init {
        monitorUserStatus()
        monitorUnreadChats()
    }

    fun triggerScrollToTop(route: String) {
        viewModelScope.launch {
            _scrollToTopEvent.emit(route)
        }
    }

    fun monitorUserStatus() {
        val uid = auth.currentUser?.uid ?: return
        userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return@addSnapshotListener
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _isUserBanned.value = snapshot.getBoolean("isBanned") ?: false
                }
            }
    }
    private fun monitorUnreadChats() {
        val uid = auth.currentUser?.uid ?: return

        chatListener = firestore.collection("channels")
            .where(
                Filter.or(
                    Filter.equalTo("userId", uid),
                    Filter.equalTo("receiverId", uid)
                )
            )
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    var totalUnread = 0
                    for (doc in snapshot.documents) {
                        val unreadMap = doc.get("unreadCounts") as? Map<String, Long>
                        val myUnread = unreadMap?.get(uid)?.toInt() ?: 0
                        totalUnread += myUnread
                    }
                    _unreadChatCount.value = totalUnread
                }
            }
    }

    fun stopMonitoring() {
        userListener?.remove()
        userListener = null
        chatListener?.remove()
        chatListener = null
    }

    fun logout(onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                try {
                    firestore.collection("users").document(userId).update("fcmToken", FieldValue.delete())
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_notifications")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        stopMonitoring()
        auth.signOut()
        onSuccess()
    }
}