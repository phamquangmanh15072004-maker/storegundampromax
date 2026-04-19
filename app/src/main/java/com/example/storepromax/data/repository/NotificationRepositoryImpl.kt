package com.example.storepromax.data.repository

import android.util.Log
import com.example.storepromax.data.api.GunplaBackendApi
import com.example.storepromax.domain.model.FcmRequest
import com.example.storepromax.domain.model.UserNotification
import com.example.storepromax.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val backendApi: GunplaBackendApi
) : NotificationRepository {


    override fun getUserNotifications(userId: String): Flow<List<UserNotification>> = callbackFlow {
        val query = firestore.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val notifList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserNotification::class.java)?.copy(id = doc.id)
                }
                trySend(notifList).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("notifications").whereEqualTo("isRead", false).get().await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllNotifications(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("notifications").get().await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private suspend fun saveToFirestore(
        userId: String, title: String, body: String, type: String,
        orderId: String? = null, postId: String? = null, action: String = ""
    ) {
        try {
            val notifRef =
                firestore.collection("users").document(userId).collection("notifications")
                    .document()
            val notificationData = hashMapOf<String, Any>(
                "id" to notifRef.id,
                "userId" to userId,
                "title" to title,
                "body" to body,
                "type" to type,
                "isRead" to false,
                "timestamp" to System.currentTimeMillis()
            )
            orderId?.let { notificationData["orderId"] = it }
            postId?.let { notificationData["postId"] = it }
            if (action.isNotEmpty()) {
                notificationData["action"] = action
            }

            notifRef.set(notificationData).await()
            Log.d("NOTIFICATION", "Đã lưu DB cho user: $userId")
        } catch (e: Exception) {
            Log.e("NOTIFICATION", "Lỗi lưu DB: ${e.message}")
        }
    }


    override suspend fun sendOrderNotification(
        userToken: String, userId: String, orderId: String, status: String, cancelReason: String
    ) = withContext(Dispatchers.IO) {
        val (title, body, action) = when (status) {
            "CONFIRMED" -> Triple(
                "Đơn hàng đã được xác nhận ✅",
                "Shop đã nhận đơn #${orderId} và đang chuẩn bị hàng.",
                ""
            )

            "SHIPPING" -> Triple(
                "Đơn hàng đang được vận chuyển 🚚",
                "Shipper đang trên đường giao đơn #${orderId} đến bạn.",
                ""
            )

            "DELIVERED" -> Triple(
                "Giao hàng thành công 🎉",
                "Bạn đã nhận được đơn hàng #${orderId}. Hãy đánh giá nhé!",
                "NAVIGATE_TO_REVIEW"
            )

            "CANCELLED" -> Triple(
                "Đơn hàng đã bị hủy ❌",
                "Đơn #${orderId} đã bị hủy. Lý do: $cancelReason",
                ""
            )

            else -> return@withContext
        }

        saveToFirestore(userId, title, body, "ORDER_UPDATE", orderId, null, action)

        if (userToken.isNotEmpty()) {
            try {
                val request = FcmRequest(
                    targetToken = userToken,
                    title = title,
                    body = body,
                    type = "ORDER_UPDATE",
                    orderId = orderId,
                    action = action.takeIf { it.isNotEmpty() }
                )
                backendApi.sendFcmNotification(request)
            } catch (e: Exception) {
                Log.e("FCM_API", "Lỗi gửi FCM Đơn hàng: ${e.message}")
            }
        }
    }

    override suspend fun sendOrderNotificationToAdmin(orderId: String, totalAmount: Double) {
        withContext(Dispatchers.IO) {
            val title = "Có đơn hàng mới! 🤑"
            val body = "Đơn hàng #$orderId trị giá ${totalAmount.toLong()}đ đang chờ duyệt."
            try {
                val webNotifData = hashMapOf(
                    "title" to title,
                    "message" to body,
                    "type" to "ORDER",
                    "targetId" to orderId,
                    "targetRoles" to listOf("ADMIN", "STAFF"),
                    "readBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("notifications").add(webNotifData).await()
            } catch (e: Exception) {
                Log.e("WEB_NOTIF", "Lỗi gửi Web Admin: ${e.message}")
            }

            try {
                val request = FcmRequest(
                    topic = "admin_notifications", title = title, body = body,
                    type = "NEW_ORDER", orderId = orderId
                )
                backendApi.sendFcmNotification(request)
            } catch (e: Exception) {
                Log.e("FCM_API", "Lỗi gửi FCM Admin: ${e.message}")
            }
        }
    }

    override suspend fun sendChatNotification(
        receiverToken: String,
        senderName: String,
        messageContent: String,
        channelId: String
    ) {withContext(Dispatchers.IO) {
        if (receiverToken.isEmpty()) return@withContext
        try {
            val request = FcmRequest(
                targetToken = receiverToken,
                title = "Tin nhắn từ $senderName",
                body = messageContent,
                type = "CHAT",
                channelId = channelId
            )
            backendApi.sendFcmNotification(request)
        } catch (e: Exception) {
            Log.e("FCM_API", "Lỗi gửi Chat Khách: ${e.message}")
        }
    }}

    override suspend fun sendChatNotificationToAdmin(
        senderName: String,
        messageContent: String,
        channelId: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val request = FcmRequest(
                    topic = "admin_notifications",
                    title = "Tin nhắn mới từ $senderName 💬",
                    body = messageContent,
                    type = "CHAT_ADMIN",
                    channelId = channelId
                )
                Log.d("DEBUG_NOTIF", "Đã chui vào trong Repository thành công!")
                backendApi.sendFcmNotification(request)
            } catch (e: Exception) {
                Log.e("FCM_API", "Lỗi gửi Chat Admin: ${e.message}")
            }
        }
    }

    override suspend fun sendCommentNotification(
        receiverToken: String,
        title: String,
        body: String,
        postId: String,
        receiverUserId: String
    ) {
        withContext(Dispatchers.IO) {
            saveToFirestore(
                userId = receiverUserId,
                title = title,
                body = body,
                type = "COMMENT",
                postId = postId
            )

            if (receiverToken.isNotEmpty()) {
                try {
                    val request = FcmRequest(
                        targetToken = receiverToken, title = title, body = body,
                        type = "COMMENT", postId = postId
                    )
                    backendApi.sendFcmNotification(request)
                } catch (e: Exception) {
                    Log.e("FCM_API", "Lỗi gửi FCM Comment: ${e.message}")
                }
            }
        }
    }

    override suspend fun sendLikeNotification(
        receiverToken: String,
        receiverUserId: String,
        senderName: String,
        postTitle: String,
        postId: String
    ) {
        withContext(Dispatchers.IO) {
            val title = "$senderName đã thích bài viết của bạn ❤️"
            val body = "Bài viết: $postTitle"

            saveToFirestore(
                userId = receiverUserId,
                title = title,
                body = body,
                type = "LIKE",
                postId = postId
            )

            if (receiverToken.isNotEmpty()) {
                try {
                    val request = FcmRequest(
                        targetToken = receiverToken, title = title, body = body,
                        type = "LIKE", postId = postId
                    )
                    backendApi.sendFcmNotification(request)
                } catch (e: Exception) {
                    Log.e("FCM_API", "Lỗi gửi FCM Like: ${e.message}")
                }
            }
        }
    }
}