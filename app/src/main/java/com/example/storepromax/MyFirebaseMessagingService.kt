package com.example.storepromax

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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

        if (type == "CHAT_MESSAGE" || type == "CHAT") {
            if (channelId.isNotEmpty() && channelId == ChatStateManager.activeChannelId) {
                Log.d("FCM_TEST", "Đang mở chat detail -> chặn push")
                return
            }
            if (ChatStateManager.isChatListOpen) {
                Log.d("FCM_TEST", "Đang mở chat list -> chặn push")
                return
            }
        }

        showNotification(title, body, type, data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Google vừa cấp token mới: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "Đã cập nhật token mới lên Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN", "Lỗi cập nhật token: ${e.message}")
                }
        } else {
            Log.d("FCM_TOKEN", "User chưa đăng nhập nên chưa lưu token")
        }
    }

    private fun showNotification(title: String, body: String, type: String, data: Map<String, String>) {
        if (!canPostNotifications()) {
            Log.w("NOTIFICATION_SERVICE", "Chưa được cấp quyền POST_NOTIFICATIONS, bỏ qua notification type=$type")
            return
        }

        Log.d("NOTIFICATION_SERVICE", "Hiển thị thông báo type=$type")
        NotificationChannels.create(this)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val isChat = type == "CHAT_MESSAGE" || type == "CHAT" || type == "CHAT_ADMIN"
        val notificationChannelId = if (isChat) {
            NotificationChannels.CHAT_CHANNEL_ID
        } else {
            NotificationChannels.ORDER_CHANNEL_ID
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val requestCode = Random.nextInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val vibrationPattern = longArrayOf(0L, 250L, 120L, 250L)

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(vibrationPattern)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (isChat) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        try {
            val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
            }
        } catch (e: Exception) {
            Log.e("FCM_TEST", "Không load được large icon: ${e.message}")
        }

        notificationManager.notify(requestCode, notificationBuilder.build())
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
