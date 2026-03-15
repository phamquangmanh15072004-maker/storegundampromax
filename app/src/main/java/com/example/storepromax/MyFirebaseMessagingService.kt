package com.example.storepromax

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
        val action = data["action"] ?: ""
        showNotification(title, body, type, data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToServer(token)
    }

    private fun showNotification(title: String, body: String, type: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val isChat = type.contains("CHAT")
        val channelId = if (isChat) "storepromax_chats" else "storepromax_orders"
        val channelName = if (isChat) "Tin nhắn " else "Thông báo Đơn hàng"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, Random.nextInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(ContextCompat.getColor(this, R.color.teal_200))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        try {
            val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
            }
        } catch (e: Exception) {
            Log.e("FCM_TEST", "Không load được LargeIcon: ${e.message}")
        }

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    private fun sendTokenToServer(token: String) {
        println("FCM TOKEN CỦA TÔI: $token")
    }
}