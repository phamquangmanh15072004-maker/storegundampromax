package com.example.storepromax

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build

object NotificationChannels {
    const val CHAT_CHANNEL_ID = "storepromax_chats_high_v2"
    const val ORDER_CHANNEL_ID = "storepromax_orders_high_v2"
    const val DEFAULT_CHANNEL_ID = ORDER_CHANNEL_ID

    private val vibrationPattern = longArrayOf(0L, 250L, 120L, 250L)

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chatChannel = NotificationChannel(
            CHAT_CHANNEL_ID,
            "Tin nhắn",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo tin nhắn mới từ Gunpla Hub"
            enableLights(true)
            lightColor = Color.rgb(0, 122, 255)
            enableVibration(true)
            vibrationPattern = NotificationChannels.vibrationPattern
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }

        val orderChannel = NotificationChannel(
            ORDER_CHANNEL_ID,
            "Đơn hàng và hệ thống",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo đơn hàng, thanh toán và hoạt động quan trọng"
            enableLights(true)
            lightColor = Color.rgb(0, 150, 136)
            enableVibration(true)
            vibrationPattern = NotificationChannels.vibrationPattern
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }

        notificationManager.createNotificationChannels(listOf(chatChannel, orderChannel))
    }
}
