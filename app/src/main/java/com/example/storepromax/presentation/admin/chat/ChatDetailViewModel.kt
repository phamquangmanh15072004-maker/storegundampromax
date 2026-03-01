package com.example.storepromax.presentation.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.model.ChatChannel
import com.example.storepromax.domain.model.ChatMessage
import com.example.storepromax.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume

data class UploadingMedia(
    val uri: Uri,
    val isVideo: Boolean
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentChannel = MutableStateFlow<ChatChannel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _uploadingMedia = MutableStateFlow<UploadingMedia?>(null)
    val uploadingMedia = _uploadingMedia.asStateFlow()

    val currentUserId = auth.currentUser?.uid ?: ""

    fun loadMessages(channelId: String) {
        viewModelScope.launch {
            chatRepo.getMessages(channelId).collect { listMsg ->
                _messages.value = listMsg
            }
        }
        viewModelScope.launch {
            firestore.collection("channels").document(channelId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val channel = snapshot.toObject(ChatChannel::class.java)
                        _currentChannel.value = channel
                    }
                }
        }
    }
    private suspend fun triggerPushNotification(content: String, isSenderAdmin: Boolean) {
        try {
            val channel = _currentChannel.value
            if (channel == null) {
                Log.e("FCM_CHECK", "❌ LỖI: channel đang bị NULL, bị ngắt ngang tại đây!")
                return
            }
            val customerId = channel.userId
            Log.d("FCM_CHECK", "2. Đã lấy được Channel. ID Khách: $customerId")

            if (isSenderAdmin) {
                val userDoc = firestore.collection("users").document(customerId).get().await()
                val fcmToken = userDoc.getString("fcmToken")

                if (!fcmToken.isNullOrEmpty()) {
                    NotificationHelper.sendChatNotification(
                        context = context,
                        receiverToken = fcmToken,
                        senderName = "CSKH StoreProMax",
                        messageContent = content,
                        channelId = channel.id
                    )
                } else {
                    Log.e("FCM_CHECK", "❌ LỖI: Token của khách trống!")
                }
            } else {
                val senderName = auth.currentUser?.displayName ?: "Khách hàng"
                Log.d("FCM_CHECK", "3. Khách đang bắn thông báo lên Topic Admin")
                NotificationHelper.sendChatNotificationToAdmin(
                    context = context,
                    senderName = senderName,
                    messageContent = content,
                    channelId = channel.id
                )
            }
        } catch (e: Exception) {
            Log.e("FCM_CHECK", "❌ LỖI TRY-CATCH: ${e.message}")
        }
    }
    fun sendMessage(channelId: String, content: String, isSenderAdmin: Boolean) {
        if (content.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(
                channelId = channelId,
                content = content,
                type = "TEXT",
                mediaUrl = "",
                isAdmin = isSenderAdmin
            )

            triggerPushNotification(content, isSenderAdmin)
        }
    }

    fun sendMedia(channelId: String, uri: Uri, isVideo: Boolean, isSenderAdmin: Boolean) {
        viewModelScope.launch {
            _uploadingMedia.value = UploadingMedia(uri, isVideo)
            val url = uploadMediaToCloudinary(uri, isVideo)

            if (url != null) {
                val type = if (isVideo) "VIDEO" else "IMAGE"
                val contentText = if (isVideo) "[Đã gửi một video 🎬]" else "[Đã gửi một ảnh 📸]"

                // 1. Chờ gửi tin nhắn ảnh xong
                chatRepo.sendMessage(
                    channelId = channelId,
                    content = contentText,
                    type = type,
                    mediaUrl = url,
                    isAdmin = isSenderAdmin
                )

                triggerPushNotification(contentText, isSenderAdmin)
            }
            _uploadingMedia.value = null
        }
    }

    fun closeTicket(channelId: String) {
        viewModelScope.launch {
            chatRepo.updateChannelStatus(channelId, "SOLVED")
        }
    }

    private suspend fun uploadMediaToCloudinary(uri: Uri, isVideo: Boolean): String? = suspendCancellableCoroutine { cont ->
        val type = if (isVideo) "video" else "image"
        try {
            MediaManager.get().upload(uri)
                .option("resource_type", type)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        cont.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        cont.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        } catch (e: Exception) {
            e.printStackTrace()
            cont.resume(null)
        }
    }
}