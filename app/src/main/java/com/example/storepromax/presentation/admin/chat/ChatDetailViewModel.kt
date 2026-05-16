package com.example.storepromax.presentation.admin.chat

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.domain.model.ChatChannel
import com.example.storepromax.domain.model.ChatMessage
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

enum class PendingMessageStatus {
    SENDING,
    FAILED
}

data class PendingChatMessage(
    val id: String,
    val channelId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val type: String = "TEXT",
    val localUri: Uri? = null,
    val mediaUrl: String = "",
    val replyToId: String? = null,
    val isVideo: Boolean = false,
    val status: PendingMessageStatus = PendingMessageStatus.SENDING,
    val errorMessage: String = ""
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _pendingMessages = MutableStateFlow<List<PendingChatMessage>>(emptyList())
    val pendingMessages = _pendingMessages.asStateFlow()

    private val _currentChannel = MutableStateFlow<ChatChannel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _partnerAvatarUrl = MutableStateFlow<String?>(null)
    val partnerAvatarUrl = _partnerAvatarUrl.asStateFlow()

    val currentUserId = auth.currentUser?.uid ?: ""

    fun loadMessages(channelId: String) {
        viewModelScope.launch {
            chatRepo.getMessages(channelId).collect { listMsg ->
                _messages.value = listMsg.filter { !it.deletedBy.contains(currentUserId) }
            }
        }

        viewModelScope.launch {
            firestore.collection("channels").document(channelId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        _currentChannel.value = snapshot.toObject(ChatChannel::class.java)

                        try {
                            val unreadMap = snapshot.get("unreadCounts") as? Map<*, *>
                            val myUnread = (unreadMap?.get(currentUserId) as? Number)?.toLong() ?: 0L
                            if (myUnread > 0L) {
                                firestore.collection("channels").document(channelId)
                                    .update("unreadCounts.$currentUserId", 0)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    fun fetchPartnerInfo(partnerId: String) {
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(partnerId).get().await()
                if (userDoc.exists()) {
                    val avatar = userDoc.getString("avatarUrl") ?: userDoc.getString("userAvatar")
                    _partnerAvatarUrl.value = avatar
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(channelId: String, content: String, replyToId: String? = null) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return

        val pending = PendingChatMessage(
            id = newPendingId(),
            channelId = channelId,
            senderId = currentUserId,
            content = trimmed,
            timestamp = System.currentTimeMillis(),
            replyToId = replyToId
        )

        addPending(pending)
        sendPendingMessage(pending)
    }

    fun sendMedia(channelId: String, uri: Uri, isVideo: Boolean) {
        val pending = PendingChatMessage(
            id = newPendingId(),
            channelId = channelId,
            senderId = currentUserId,
            content = if (isVideo) "[Đã gửi video]" else "[Đã gửi ảnh]",
            timestamp = System.currentTimeMillis(),
            type = if (isVideo) "VIDEO" else "IMAGE",
            localUri = uri,
            isVideo = isVideo
        )

        addPending(pending)
        sendPendingMessage(pending)
    }

    fun retryPendingMessage(pendingId: String) {
        val pending = _pendingMessages.value.firstOrNull { it.id == pendingId } ?: return
        val retrying = pending.copy(
            status = PendingMessageStatus.SENDING,
            errorMessage = "",
            timestamp = System.currentTimeMillis()
        )
        replacePending(retrying)
        sendPendingMessage(retrying)
    }

    fun removePendingMessage(pendingId: String) {
        _pendingMessages.value = _pendingMessages.value.filterNot { it.id == pendingId }
    }

    private fun sendPendingMessage(pending: PendingChatMessage) {
        viewModelScope.launch {
            try {
                if (!hasNetworkConnection()) {
                    markPendingFailed(pending.id, "Không có kết nối mạng. Nhấn để gửi lại.")
                    return@launch
                }

                val mediaUrl = if (pending.type == "IMAGE" || pending.type == "VIDEO") {
                    val localUri = pending.localUri ?: throw IllegalStateException("Không tìm thấy file cần gửi.")
                    val url = withTimeout(CHAT_MEDIA_UPLOAD_TIMEOUT_MS) {
                        uploadMediaToCloudinary(localUri, pending.isVideo)
                    }
                    if (url.isNullOrBlank()) throw IllegalStateException("Upload media thất bại.")
                    url
                } else {
                    pending.mediaUrl
                }

                val messageId = pending.id

                val newMessage = hashMapOf(
                    "id" to messageId,
                    "channelId" to pending.channelId,
                    "senderId" to currentUserId,
                    "content" to pending.content,
                    "type" to pending.type,
                    "mediaUrl" to mediaUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "replyToId" to pending.replyToId,
                    "deletedBy" to emptyList<String>()
                )

                withTimeout(CHAT_SEND_TIMEOUT_MS) {
                    firestore.collection("channels")
                        .document(pending.channelId)
                        .collection("messages")
                        .document(messageId)
                        .set(newMessage)
                        .await()
                }

                removePendingMessage(pending.id)

                try {
                    withTimeout(CHAT_SEND_TIMEOUT_MS) {
                        updateChannelAfterSend(pending.channelId, pending.content)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: TimeoutCancellationException) {
                markPendingFailed(pending.id, "Mạng chậm, chưa gửi được. Nhấn để thử lại.")
            } catch (e: Exception) {
                markPendingFailed(pending.id, "Gửi lỗi. Nhấn để thử lại.")
            }
        }
    }

    private fun hasNetworkConnection(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun addPending(pending: PendingChatMessage) {
        _pendingMessages.value = _pendingMessages.value + pending
    }

    private fun replacePending(pending: PendingChatMessage) {
        _pendingMessages.value = _pendingMessages.value.map { if (it.id == pending.id) pending else it }
    }

    private fun markPendingFailed(pendingId: String, message: String) {
        _pendingMessages.value = _pendingMessages.value.map {
            if (it.id == pendingId) it.copy(status = PendingMessageStatus.FAILED, errorMessage = message) else it
        }
    }

    private suspend fun updateChannelAfterSend(channelId: String, lastMessage: String) {
        val channel = _currentChannel.value
        val partnerId = if (channel?.userId == currentUserId) channel.receiverId else channel?.userId

        val updateData = mutableMapOf<String, Any>(
            "lastMessage" to lastMessage,
            "lastUpdated" to System.currentTimeMillis(),
            "lastSenderId" to currentUserId
        )

        if (!partnerId.isNullOrBlank()) {
            updateData["unreadCounts.$partnerId"] = FieldValue.increment(1)
        }

        firestore.collection("channels").document(channelId).update(updateData).await()

        if (!partnerId.isNullOrBlank()) {
            val senderName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Một người dùng"
            NotificationHelper.sendChatPushNotification(
                receiverId = partnerId,
                senderName = senderName,
                messageContent = lastMessage,
                channelId = channelId
            )
        }
    }

    fun revokeMessage(channelId: String, messageId: String) {
        viewModelScope.launch {
            firestore.collection("channels").document(channelId).collection("messages").document(messageId)
                .update("content", "Tin nhắn đã bị thu hồi", "type", "TEXT", "mediaUrl", "")
        }
    }

    fun deleteMessageForMe(channelId: String, messageId: String) {
        viewModelScope.launch {
            firestore.collection("channels").document(channelId).collection("messages").document(messageId)
                .update("deletedBy", FieldValue.arrayUnion(currentUserId))
        }
    }

    fun blockUser(channelId: String) {
        viewModelScope.launch {
            firestore.collection("channels").document(channelId)
                .update("blockedBy", FieldValue.arrayUnion(currentUserId))
        }
    }

    fun unblockUser(channelId: String) {
        viewModelScope.launch {
            firestore.collection("channels").document(channelId)
                .update("blockedBy", FieldValue.arrayRemove(currentUserId))
        }
    }

    private suspend fun uploadMediaToCloudinary(uri: Uri, isVideo: Boolean): String? =
        suspendCancellableCoroutine { cont ->
            val type = if (isVideo) "video" else "image"
            try {
                MediaManager.get().upload(uri)
                    .unsigned("gundame-storepromax")
                    .option("resource_type", type)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) = Unit
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) = Unit
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            if (cont.isActive) cont.resume(resultData["secure_url"] as? String)
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            if (cont.isActive) cont.resume(null)
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) = Unit
                    })
                    .dispatch()
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private fun newPendingId(): String = "local-${UUID.randomUUID()}"

    companion object {
        private const val CHAT_SEND_TIMEOUT_MS = 15_000L
        private const val CHAT_MEDIA_UPLOAD_TIMEOUT_MS = 45_000L
    }
}
