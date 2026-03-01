package com.example.storepromax.admin.utils

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.example.storepromax.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Collections

object NotificationHelper {

    private const val PROJECT_ID = "gundam-shop-app"
    private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    private fun getAccessToken(context: Context): String {
        val inputStream = context.resources.openRawResource(R.raw.service_account)
        val googleCredentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"))

        googleCredentials.refreshIfExpired()
        return googleCredentials.accessToken.tokenValue
    }

    suspend fun sendOrderNotification(
        context: Context,
        userToken: String,
        orderId: String,
        status: String,
        cancelReason: String = ""
    ) = withContext(Dispatchers.IO) {

        var title = ""
        var body = ""
        when (status) {
            "CONFIRMED" -> {
                title = "Đơn hàng đã được xác nhận ✅"
                body = "Shop đã nhận đơn #${orderId} và đang chuẩn bị hàng."
            }
            "SHIPPING" -> {
                title = "Đơn hàng đang được vận chuyển 🚚"
                body = "Shipper đang trên đường giao đơn #${orderId} đến bạn."
            }
            "DELIVERED" -> {
                title = "Giao hàng thành công 🎉"
                body = "Bạn đã nhận được đơn hàng #${orderId}. Hãy đánh giá nhé!"
            }
            "CANCELLED" -> {
                title = "Đơn hàng đã bị hủy ❌"
                body = "Đơn #${orderId} đã bị hủy. Lý do: $cancelReason"
            }
            else -> return@withContext
        }

        try {
            val accessToken = getAccessToken(context)
            val message = JSONObject()
            val notification = JSONObject()
            val data = JSONObject()

            notification.put("title", title)
            notification.put("body", body)

            data.put("type", "ORDER_UPDATE")
            data.put("orderId", orderId)

            message.put("token", userToken)
            message.put("notification", notification)
            message.put("data", data)

            val finalJson = JSONObject()
            finalJson.put("message", message)

            val client = OkHttpClient()
            val requestBody = finalJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FCM_URL)
                .addHeader("Authorization", "Bearer $accessToken") // Dùng Bearer Token
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            println("FCM v1 Response: ${response.body?.string()}")

        } catch (e: Exception) {
            e.printStackTrace()
            println("Lỗi gửi FCM v1: ${e.message}")
        }
    }
    suspend fun sendOrderNotificationToAdmin(
        context: Context,
        orderId: String,
        totalAmount: Double
    ) = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken(context)

            val message = JSONObject()
            val notification = JSONObject()
            val data = JSONObject()

            notification.put("title", "Có đơn hàng mới! 🤑")
            notification.put("body", "Đơn hàng #$orderId trị giá ${totalAmount.toLong()}đ đang chờ duyệt.")

            data.put("type", "NEW_ORDER")
            data.put("orderId", orderId)

            message.put("topic", "admin_notifications")
            message.put("notification", notification)
            message.put("data", data)

            val finalJson = JSONObject()
            finalJson.put("message", message)
            val client = OkHttpClient()
            val requestBody = finalJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FCM_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            println("Gửi Admin thành công: ${response.body?.string()}")

        } catch (e: Exception) {
            e.printStackTrace()
            println("Lỗi gửi Admin: ${e.message}")
        }
    }
    suspend fun sendChatNotification(
        context: Context,
        receiverToken: String,
        senderName: String,
        messageContent: String,
        channelId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken(context)

            val message = JSONObject()
            val data = JSONObject()

            data.put("title", "Tin nhắn từ $senderName")
            data.put("body", messageContent)
            data.put("type", "CHAT")
            data.put("channelId", channelId)

            message.put("token", receiverToken)
            message.put("data", data)

            val finalJson = JSONObject().apply { put("message", message) }

            val client = OkHttpClient()
            val requestBody = finalJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(FCM_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            Log.d("FCM_CHECK", "4. FIREBASE TRẢ VỀ: ${response.body?.string()}")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FCM_CHECK", "❌ LỖI API GOOGLE: ${e.message}")
        }
    }
    suspend fun sendChatNotificationToAdmin(
        context: Context,
        senderName: String,
        messageContent: String,
        channelId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken(context)
            val message = JSONObject()
            val data = JSONObject() // 👉 CHỈ DÙNG DATA

            data.put("title", "Tin nhắn mới từ $senderName 💬")
            data.put("body", messageContent)
            data.put("type", "CHAT_ADMIN")
            data.put("channelId", channelId)

            message.put("topic", "admin_notifications")
            message.put("data", data)
            // 🔥 ĐÃ BỎ message.put("notification", notification)

            val finalJson = JSONObject().apply { put("message", message) }
            val client = OkHttpClient()
            val requestBody = finalJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(FCM_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            Log.d("FCM_CHECK", "4. FIREBASE TRẢ VỀ ADMIN: ${response.body?.string()}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FCM_CHECK", "❌ LỖI API GOOGLE (ADMIN): ${e.message}")
        }
    }
}