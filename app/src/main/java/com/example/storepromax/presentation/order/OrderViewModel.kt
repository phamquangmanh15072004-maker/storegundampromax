package com.example.storepromax.presentation.order

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.data.api.BackendRetrofit
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.PaymentRequest
import com.example.storepromax.domain.model.VietQRBank
import com.example.storepromax.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _banks = MutableStateFlow<List<VietQRBank>>(emptyList())
    val banks = _banks.asStateFlow()

    private var paymentListenerJob: Job? = null
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()
    private val _processingOrderId = MutableStateFlow<String?>(null)
    val processingOrderId = _processingOrderId.asStateFlow()
    private val _returnUploadProgress = MutableStateFlow<Float?>(null)
    val returnUploadProgress = _returnUploadProgress.asStateFlow()

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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun getPaymentDetails(order: Order, onResult: (Boolean, String, String, String, String) -> Unit) { // Thêm 1 String ở đây
        if (_processingOrderId.value != null) return

        viewModelScope.launch {
            _processingOrderId.value = order.id
            try {
                val response = BackendRetrofit.api.createPaymentLink(
                    PaymentRequest(order.id, order.totalPrice, "Thanh toan ${order.id.takeLast(6).uppercase()}")
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    onResult(true, body.bin ?: "", body.accountNumber ?: "", body.checkoutUrl ?: "", body.description ?: "")

                    listenToOrderPaymentStatus(order.id)

                } else {
                    onResult(false, "", "", "", "")
                }
            } catch (e: Exception) {
                Log.e("OrderVM", "Lỗi lấy link thanh toán: ${e.message}")
                onResult(false, "", "", "", "")
            } finally {
                _processingOrderId.value = null
            }
        }
    }
    fun listenToOrderPaymentStatus(orderId: String) {
        paymentListenerJob?.cancel()
        paymentListenerJob = viewModelScope.launch {
            orderRepository.getOrderById(orderId).collect { order ->
                if (order?.paymentStatus == "PAID") {
                    _uiEvent.send("PAYMENT_SUCCESS")
                    paymentListenerJob?.cancel()
                }
            }
        }
    }
    fun cancelOrder(orderId: String, reason: String, isPaid: Boolean = false, bankBin: String? = null, bankShortName: String? = null, accountNumber: String? = null, accountName: String? = null) {
        viewModelScope.launch {
            try {
                orderRepository.cancelOrder(orderId, reason, isPaid, bankBin, bankShortName, accountNumber, accountName)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun requestReturnRefund(orderId: String, reason: String, description: String, localMediaUris: List<String>, bankBin: String?, bankShortName: String?, accountNumber: String?, accountName: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _processingOrderId.value = orderId
            _returnUploadProgress.value = 0f
            try {
                localMediaUris.forEach { uriString ->
                    validateReturnMediaSize(Uri.parse(uriString))
                }

                val cloudUrls = withContext(Dispatchers.IO) {
                    val uploadResults = localMediaUris.mapIndexed { index, uriString ->
                        uploadOneMedia(uriString) { fileProgress ->
                            val totalFiles = localMediaUris.size.coerceAtLeast(1)
                            _returnUploadProgress.value = ((index + fileProgress.coerceIn(0f, 1f)) / totalFiles)
                                .coerceIn(0f, 0.98f)
                        }
                    }
                    if (uploadResults.any { it.isNullOrBlank() }) {
                        throw IllegalStateException("Không tải được bằng chứng lên Cloudinary. Vui lòng thử lại.")
                    }
                    uploadResults.filterNotNull()
                }
                _returnUploadProgress.value = 1f

                if (cloudUrls.isEmpty() && localMediaUris.isNotEmpty()) {
                    onResult(false, "Lỗi tải bằng chứng lên Cloudinary!")
                    return@launch
                }

                orderRepository.requestReturnRefund(orderId, reason, description, cloudUrls, bankBin, bankShortName, accountNumber, accountName)
                onResult(true, "Gửi yêu cầu thành công!")
            } catch (e: Exception) {
                onResult(false, e.message?.takeIf { it.isNotBlank() } ?: "Không gửi được yêu cầu trả hàng. Vui lòng thử lại.")
            } finally {
                _processingOrderId.value = null
                _returnUploadProgress.value = null
            }
        }
    }

    private suspend fun uploadOneMedia(uriString: String, onProgress: (Float) -> Unit): String? {
        val uri = Uri.parse(uriString)
        val mimeType = appContext.contentResolver.getType(uri).orEmpty()
        val isVideo = mimeType.startsWith("video/")
        val resourceType = if (isVideo) "video" else "image"

        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(uri)
                .unsigned("gundame-storepromax")
                .option("resource_type", resourceType)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        if (totalBytes > 0L) {
                            onProgress(bytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        onProgress(1f)
                        val url = resultData["secure_url"] as? String
                        if (continuation.isActive) continuation.resumeWith(Result.success(url))
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(null))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        }
    }

    private fun validateReturnMediaSize(uri: Uri) {
        val sizeBytes = try {
            appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0L }
            }
        } catch (e: Exception) {
            null
        }

        if (sizeBytes != null && sizeBytes > MAX_RETURN_MEDIA_SIZE_BYTES) {
            throw IllegalArgumentException("Bằng chứng không được vượt quá 10MB. Vui lòng chọn ảnh hoặc video dung lượng nhỏ hơn.")
        }
    }

    fun submitReturnTrackingCode(orderId: String, trackingCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = orderRepository.submitReturnTrackingCode(orderId, trackingCode)
            if (result.isSuccess) onResult(true, "Cập nhật mã vận đơn thành công!")
            else onResult(false, "Lỗi cập nhật!")
        }
    }
}

private const val MAX_RETURN_MEDIA_SIZE_BYTES = 10L * 1024L * 1024L
