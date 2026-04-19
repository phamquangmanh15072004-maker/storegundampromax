package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.UserNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {

    fun getUserNotifications(userId: String): Flow<List<UserNotification>>

    suspend fun markAsRead(userId: String, notificationId: String): Result<Unit>

    suspend fun markAllAsRead(userId: String): Result<Unit>

    suspend fun deleteAllNotifications(userId: String): Result<Unit>

    suspend fun sendOrderNotification(userToken: String, userId: String, orderId: String, status: String, cancelReason: String = "")

    suspend fun sendOrderNotificationToAdmin(orderId: String, totalAmount: Double)

    suspend fun sendChatNotification(receiverToken: String, senderName: String, messageContent: String, channelId: String)

    suspend fun sendChatNotificationToAdmin(senderName: String, messageContent: String, channelId: String)

    suspend fun sendCommentNotification(receiverToken: String, title: String, body: String, postId: String, receiverUserId: String)

    suspend fun sendLikeNotification(receiverToken: String, receiverUserId: String, senderName: String, postTitle: String, postId: String)
}