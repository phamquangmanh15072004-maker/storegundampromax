package com.example.storepromax.presentation.order

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.VietQRBank
import com.example.storepromax.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _banks = MutableStateFlow<List<VietQRBank>>(emptyList())
    val banks = _banks.asStateFlow()

    val orders: StateFlow<List<Order>> = orderRepository.getOrders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        fetchBanks()
    }

    private fun fetchBanks() {
        viewModelScope.launch {
            try {
                val response = com.example.storepromax.data.api.VietQRRetrofit.api.getBanks()
                if (response.isSuccessful && response.body() != null) {
                    _banks.value = response.body()!!.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelOrder(
        orderId: String,
        reason: String,
        isPaid: Boolean = false,
        bankBin: String? = null,
        bankShortName: String? = null,
        accountNumber: String? = null,
        accountName: String? = null
    ) {
        viewModelScope.launch {
            try {
                orderRepository.cancelOrder(
                    orderId, reason, isPaid,
                    bankBin, bankShortName, accountNumber, accountName
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestReturnRefund(
        orderId: String,
        reason: String,
        description: String,
        localMediaUris: List<String>,
        bankBin: String?,
        bankShortName: String?,
        accountNumber: String?,
        accountName: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cloudUrls = withContext(Dispatchers.IO) {
                    localMediaUris.map { uriString ->
                        async { uploadOneMedia(uriString) }
                    }.awaitAll().filterNotNull()
                }

                if (cloudUrls.isEmpty() && localMediaUris.isNotEmpty()) {
                    onResult(false, "Lỗi tải ảnh/video bằng chứng!")
                    return@launch
                }
                orderRepository.requestReturnRefund(
                    orderId = orderId,
                    reason = reason,
                    description = description,
                    images = cloudUrls,
                    bankBin = bankBin,
                    bankShortName = bankShortName,
                    accountNumber = accountNumber,
                    accountName = accountName
                )

                onResult(true, "Gửi yêu cầu thành công. Shop sẽ phản hồi sớm!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Có lỗi xảy ra, vui lòng thử lại!")
            }
        }
    }
    private suspend fun uploadOneMedia(uriString: String): String? {
        val uri = Uri.parse(uriString)

        val isVideo = uriString.contains("video", ignoreCase = true) ||
                uriString.endsWith(".mp4") ||
                uriString.endsWith(".mov")

        val resourceType = if (isVideo) "video" else "image"

        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(uri)
                .unsigned("gundame-storepromax")
                .option("resource_type", resourceType)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (continuation.isActive) continuation.resumeWith(Result.success(url))
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("Cloudinary", "Lỗi upload bằng chứng: ${error.description}")
                        if (continuation.isActive) continuation.resumeWith(Result.success(null))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        }
    }
    fun submitReturnTrackingCode(orderId: String, trackingCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = orderRepository.submitReturnTrackingCode(orderId, trackingCode)
            if (result.isSuccess) {
                onResult(true, "Cập nhật mã vận đơn thành công!")
            } else {
                onResult(false, "Lỗi cập nhật. Vui lòng thử lại.")
            }
        }
    }
}