package com.example.storepromax

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.storepromax.presentation.chat.ChatStateManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_TEST", "Đã nhận tin nhắn từ: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "StoreProMax"
        val body = data["body"] ?: remoteMessage.notification?.body ?: "Bạn có thông báo mới"
        val type = data["type"] ?: ""
        val channelId = data["channelId"] ?: ""

        if (type == "CHAT_MESSAGE") {
            if (channelId.isNotEmpty() && channelId == ChatStateManager.activeChannelId) {
                Log.d("FCM_TEST", "Đang mở chat detail -> Chặn Push!")
                return
            }
            if (ChatStateManager.isChatListOpen) {
                Log.d("FCM_TEST", "Đang mở chat list -> Chặn Push!")
                return
            }
        }
        showNotification(title, body, type, data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Google vừa cấp Token mới: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "Đã âm thầm cập nhật Token mới lên Firestore thành công!")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Lỗi cập nhật Token: ${e.message}")
                }
        } else {
            Log.d("FCM_TOKEN", "App vừa cài, User chưa đăng nhập nên không cần lưu Token.")
        }
    }

    private fun showNotification(title: String, body: String, type: String, data: Map<String, String>) {
        Log.d("NOTIFICATION_SERVICE", "Hiển thị thông báo! Type = $type")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val isChat = type == "CHAT_MESSAGE"
        val notifChannelId = if (isChat) "storepromax_chats" else "storepromax_orders"
        val channelName = if (isChat) "Tin nhắn" else "Thông báo Đơn hàng"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notifChannelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo của StoreProMax"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val requestCode = Random.nextInt()
        val pendingIntent = PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, notifChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        try {
            val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
            }
        } catch (e: Exception) {
            Log.e("FCM_TEST", "Không load được LargeIcon: ${e.message}")
        }

        notificationManager.notify(requestCode, notificationBuilder.build())
    }
}