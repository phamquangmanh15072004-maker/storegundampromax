package com.example.storepromax.presentation.admin.order

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class AdminOrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    val order = orderRepository.getOrderById(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isUploadingReceipt = MutableStateFlow(false)
    val isUploadingReceipt = _isUploadingReceipt.asStateFlow()

    fun confirmRefundWithReceipt(imageUri: Uri) {
        viewModelScope.launch {
            _isUploadingReceipt.value = true
            try {
                val receiptUrl = uploadImageToCloudinary(imageUri)

                if (receiptUrl != null) {
                    orderRepository.confirmRefundWithReceipt(orderId, receiptUrl).onSuccess {
                        val currentOrder = order.value
                        if (currentOrder != null) {
                            sendNotificationToUser(
                                userId = currentOrder.userId,
                                orderId = currentOrder.id,
                                status = OrderStatus.REFUNDED,
                                reason = "Tiền đã được hoàn về tài khoản ${currentOrder.refundBankShortName}. Vui lòng kiểm tra ứng dụng ngân hàng."
                            )
                        }
                    }
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUploadingReceipt.value = false
            }
        }
    }

    private suspend fun uploadImageToCloudinary(uri: Uri): String? = suspendCancellableCoroutine { continuation ->
        try {
            MediaManager.get().upload(uri)
                .option("folder", "store_promax/refund_receipts")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        continuation.resume(secureUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        android.util.Log.e("CLOUDINARY", "Lỗi upload biên lai: ${error.description}")
                        continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(null)
        }
    }

    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus).onSuccess {
                val currentOrder = order.value
                if (currentOrder != null) {
                    sendNotificationToUser(currentOrder.userId, currentOrder.id, newStatus)
                }
            }
        }
    }

    fun cancelOrder(reason: String) {
        viewModelScope.launch {
            val currentOrder = order.value ?: return@launch
            val isPaid = currentOrder.paymentStatus == "PAID"

            orderRepository.cancelOrder(
                orderId = orderId,
                reason = "Shop hủy: $reason",
                isPaid = isPaid,
                bankBin = null,
                bankShortName = null,
                accountNumber = null,
                accountName = null
            )

            val notifyStatus = if (isPaid) OrderStatus.REFUNDING else OrderStatus.CANCELLED
            sendNotificationToUser(currentOrder.userId, currentOrder.id, notifyStatus, reason)
        }
    }
    fun confirmRefund() {
        viewModelScope.launch {
            val currentOrder = order.value ?: return@launch
            if (currentOrder.status == OrderStatus.REFUNDING) {
                orderRepository.updateOrderStatus(orderId, OrderStatus.REFUNDED).onSuccess {
                    sendNotificationToUser(
                        userId = currentOrder.userId,
                        orderId = currentOrder.id,
                        status = OrderStatus.REFUNDED,
                        reason = "Tiền đã được hoàn về tài khoản ${currentOrder.refundBankShortName} của bạn."
                    )
                }
            }
        }
    }

    private fun sendNotificationToUser(userId: String, orderId: String, status: String, reason: String = "") {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                val token = document.getString("fcmToken") ?: ""
                if (token.isNotEmpty()) {
                    NotificationHelper.sendOrderNotification(
                        context = context,
                        userToken = token,
                        userId = userId,
                        orderId = orderId,
                        status = status,
                        cancelReason = reason
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}