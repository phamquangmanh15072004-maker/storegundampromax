package com.example.storepromax.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.domain.model.ChatChannel
import com.example.storepromax.domain.model.ChatMessage
import com.example.storepromax.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UploadingMedia(val uri: Uri, val isVideo: Boolean)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentChannel = MutableStateFlow<ChatChannel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _uploadingMedia = MutableStateFlow<UploadingMedia?>(null)
    val uploadingMedia = _uploadingMedia.asStateFlow()

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
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        _currentChannel.value = snapshot.toObject(ChatChannel::class.java)
                    }
                }

            try {
                firestore.collection("channels").document(channelId)
                    .update("unreadCounts.$currentUserId", 0)
            } catch (e: Exception) { e.printStackTrace() }
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
        if (content.isBlank()) return

        val messageId = firestore.collection("channels").document(channelId).collection("messages").document().id
        val newMessage = hashMapOf(
            "id" to messageId, "senderId" to currentUserId, "content" to content,
            "type" to "TEXT", "mediaUrl" to "", "timestamp" to System.currentTimeMillis(),
            "replyToId" to replyToId, "deletedBy" to emptyList<String>()
        )

        viewModelScope.launch {
            firestore.collection("channels").document(channelId).collection("messages").document(messageId).set(newMessage)
            val channel = _currentChannel.value
            val partnerId = if (channel?.userId == currentUserId) channel.receiverId else channel?.userId

            val updateData = mutableMapOf<String, Any>(
                "lastMessage" to content,
                "lastUpdated" to System.currentTimeMillis(),
                "lastSenderId" to currentUserId
            )
            if (!partnerId.isNullOrBlank()) {
                updateData["unreadCounts.$partnerId"] = FieldValue.increment(1)
            }

            firestore.collection("channels").document(channelId).update(updateData)
        }
    }

    fun sendMedia(channelId: String, uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            _uploadingMedia.value = UploadingMedia(uri, isVideo)
            val url = uploadMediaToCloudinary(uri, isVideo)

            if (url != null) {
                val messageId = firestore.collection("channels").document(channelId).collection("messages").document().id
                val content = if (isVideo) "[Đã gửi video]" else "[Đã gửi ảnh]"
                val type = if (isVideo) "VIDEO" else "IMAGE"

                val newMessage = hashMapOf(
                    "id" to messageId, "senderId" to currentUserId, "content" to content,
                    "type" to type, "mediaUrl" to url, "timestamp" to System.currentTimeMillis(),
                    "replyToId" to null, "deletedBy" to emptyList<String>()
                )

                firestore.collection("channels").document(channelId).collection("messages").document(messageId).set(newMessage)

                // 🌟 TƯƠNG TỰ: TĂNG SỐ ĐẾM CỦA ĐỐI PHƯƠNG
                val channel = _currentChannel.value
                val partnerId = if (channel?.userId == currentUserId) channel.receiverId else channel?.userId

                val updateData = mutableMapOf<String, Any>(
                    "lastMessage" to content,
                    "lastUpdated" to System.currentTimeMillis(),
                    "lastSenderId" to currentUserId
                )
                if (!partnerId.isNullOrBlank()) {
                    updateData["unreadCounts.$partnerId"] = FieldValue.increment(1)
                }

                firestore.collection("channels").document(channelId).update(updateData)
            }
            _uploadingMedia.value = null
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

    private suspend fun uploadMediaToCloudinary(uri: Uri, isVideo: Boolean): String? = suspendCancellableCoroutine { cont ->
        val type = if (isVideo) "video" else "image"
        try {
            MediaManager.get().upload(uri).unsigned("gundame-storepromax").option("resource_type", type)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) { cont.resumeWith(Result.success(resultData["secure_url"] as? String)) }
                    override fun onError(requestId: String, error: ErrorInfo) { cont.resumeWith(Result.success(null)) }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        } catch (e: Exception) {
            cont.resumeWith(Result.success(null))
        }
    }
}