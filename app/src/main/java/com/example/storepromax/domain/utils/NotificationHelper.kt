package com.example.storepromax.domain.utils



import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {

    /**
     * Bắn Push Notification khi User A nhắn tin cho User B
     * @param receiverId ID của người nhận tin nhắn (User B)
     * @param senderName Tên của người gửi (User A)
     * @param messageContent Nội dung tin nhắn
     * @param channelId ID của phòng chat
     */
    fun sendChatPushNotification(
        receiverId: String,
        senderName: String,
        messageContent: String,
        channelId: String
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(receiverId).get()
            .addOnSuccessListener { doc ->
                val fcmToken = doc.getString("fcmToken")

                if (!fcmToken.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val url = URL("https://gunpla-backend-ht5n.onrender.com/api/send-fcm")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            conn.setRequestProperty("Accept", "application/json")
                            conn.doOutput = true
                            val shortMessage = if (messageContent.length > 60) {
                                messageContent.take(57) + "..."
                            } else {
                                messageContent
                            }
                            val jsonParam = JSONObject().apply {
                                put("targetToken", fcmToken)
                                put("title", "Tin nhắn mới từ $senderName")
                                put("body", shortMessage)
                                put("type", "CHAT_MESSAGE")
                                put("action", "VIEW_CHAT")
                                put("channelId", channelId)
                                put("orderId", channelId)
                                put("imageUrl", "")
                            }

                            conn.outputStream.use { os ->
                                val input = jsonParam.toString().toByteArray(Charsets.UTF_8)
                                os.write(input, 0, input.size)
                            }

                            val responseCode = conn.responseCode
                            Log.d("FCM_PUSH", "Đã bắn Push cho $receiverId. Response Code: $responseCode")
                            conn.disconnect()

                        } catch (e: Exception) {
                            Log.e("FCM_PUSH", "Lỗi khi gọi API bắn Push: ${e.message}")
                        }
                    }
                } else {
                    Log.w("FCM_PUSH", "Người nhận $receiverId chưa có FCM Token (Chưa cài app hoặc chưa đăng nhập).")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM_PUSH", "Lỗi khi lấy thông tin người nhận: ${e.message}")
            }
    }
}