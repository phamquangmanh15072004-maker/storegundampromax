package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.ChatChannel
import com.example.storepromax.domain.model.ChatMessage
import com.example.storepromax.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ChatRepository {
    override fun getSupportChannels(): Flow<List<ChatChannel>> = callbackFlow {
        val subscription = firestore.collection("channels")
            .whereEqualTo("type", "SUPPORT")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val channels = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatChannel::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(channels)
            }
        awaitClose { subscription.remove() }
    }
    override fun getMessages(channelId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = firestore.collection("channels").document(channelId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Tin cũ ở trên, mới ở dưới
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }
    override suspend fun sendMessage(
        channelId: String,
        content: String,
        type: String,
        mediaUrl: String,
        isAdmin: Boolean
    ): Result<Boolean> {
        return try {
            val senderId = auth.currentUser?.uid ?: return Result.failure(Exception("No User"))
            val message = ChatMessage(
                id = "",
                channelId = channelId,
                senderId = senderId,
                content = content,
                timestamp = System.currentTimeMillis(),
                isAdmin = isAdmin,
                type = type,
                mediaUrl = mediaUrl
            )
            firestore.collection("channels")
                .document(channelId)
                .collection("messages")
                .add(message)
                .await()

            val lastMessagePreview = if (type == "TEXT") content else if (type == "IMAGE") "[Hình ảnh]" else "[Video]"

            firestore.collection("channels").document(channelId).update(
                mapOf(
                    "lastMessage" to lastMessagePreview,
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    override suspend fun updateChannelStatus(channelId: String, newStatus: String): Result<Boolean> {
        return try {
            firestore.collection("channels").document(channelId)
                .update("status", newStatus).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override fun getUserChannels(): Flow<List<ChatChannel>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run { close(); return@callbackFlow }

        val subscription = firestore.collection("channels")
            .whereArrayContains("participants", currentUserId) // 🔥 Lấy chat mà tôi có tham gia
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val channels = snapshot?.toObjects(ChatChannel::class.java)?.mapIndexed { index, item ->
                    item.copy(id = snapshot.documents[index].id)
                } ?: emptyList()
                trySend(channels)
            }
        awaitClose { subscription.remove() }
    }
    override suspend fun createTradeChannel(
        sellerId: String,
        product: com.example.storepromax.domain.model.Product
    ): Result<String> {
        return try {
            val myId = auth.currentUser?.uid ?: return Result.failure(Exception("Chưa đăng nhập"))
            val newChannel = ChatChannel(
                participants = listOf(myId, sellerId),
                type = "TRADE",
                userId = myId,
                userName = auth.currentUser?.displayName ?: "Người mua",
                productId = product.id,
                productName = product.name,
                productImage = product.imageUrl,
                lastMessage = "Bắt đầu trao đổi...",
                lastUpdated = System.currentTimeMillis()
            )

            val ref = firestore.collection("channels").add(newChannel).await()
            sendMessage(ref.id, "Chào bạn, mình muốn hỏi về sản phẩm ${product.name}", isAdmin = false)

            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getOrCreateSupportChannel(): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Chưa đăng nhập"))

            val existingChat = firestore.collection("channels")
                .whereArrayContains("participants", userId)
                .whereEqualTo("type", "SUPPORT")
                .get().await()

            if (!existingChat.isEmpty) {
                return Result.success(existingChat.documents[0].id)
            }

            val currentUser = auth.currentUser
            val displayName = if (!currentUser?.displayName.isNullOrBlank()) {
                currentUser?.displayName!!
            } else {
                currentUser?.email ?: "Khách hàng"
            }

            val newChannel = ChatChannel(
                participants = listOf(userId, "ADMIN_ID"),
                type = "SUPPORT",
                userId = userId,
                userName = displayName,
                userAvatar = currentUser?.photoUrl?.toString() ?: "",

                lastMessage = "Yêu cầu hỗ trợ mới",
                lastUpdated = System.currentTimeMillis(),
                status = "PENDING"
            )

            val ref = firestore.collection("channels").add(newChannel).await()

            val welcomeMsg = ChatMessage(
                senderId = "ADMIN_ID",
                content = "Chào bạn! StorePro có thể giúp gì cho bạn?",
                isAdmin = true,
                timestamp = System.currentTimeMillis()
            )

            firestore.collection("channels").document(ref.id)
                .collection("messages").add(welcomeMsg).await()

            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getOrCreateUserChat(
        targetUserId: String,
        targetUserName: String,
        initialContent: String
    ): Result<String> {
        return try {
            val myId = auth.currentUser?.uid ?: return Result.failure(Exception("Chưa đăng nhập"))
            val querySnapshot = firestore.collection("channels")
                .whereArrayContains("participants", myId)
                .whereEqualTo("type", "private")
                .get().await()

            val existingChat = querySnapshot.documents.find { doc ->
                val participants = doc.get("participants") as? List<String> ?: emptyList()
                participants.contains(targetUserId)
            }

            if (existingChat != null) {
                return Result.success(existingChat.id)
            }

            val currentUser = auth.currentUser
            val myName = currentUser?.displayName ?: "Người dùng"
            val myAvatar = currentUser?.photoUrl?.toString() ?: ""

            val newChannel = ChatChannel(
                participants = listOf(myId, targetUserId),
                type = "private",
                userId = myId,
                userName = myName,
                userAvatar = myAvatar,
                receiverId = targetUserId,
                receiverName = targetUserName,
                lastMessage = initialContent,
                lastUpdated = System.currentTimeMillis()
            )

            val ref = firestore.collection("channels").add(newChannel).await()
            sendMessage(ref.id, initialContent, isAdmin = false)
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}