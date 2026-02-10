package com.example.storepromax.presentation.admin.product

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // State UI
    var name = mutableStateOf("")
    var description = mutableStateOf("")
    var price = mutableStateOf("")
    var originalPrice = mutableStateOf("")
    var stock = mutableStateOf("")
    var category = mutableStateOf("HG")
    var isNew = mutableStateOf(true)
    var isActive = mutableStateOf(true)
    var model3DUrl = mutableStateOf("")
    var sizesInput = mutableStateOf("")
    var colorsInput = mutableStateOf("")

    var selectedImages = mutableStateOf<List<Uri>>(emptyList())

    var isLoading = mutableStateOf(false)
    var currentProductId: String? = null

    private var existingCreatedAt: Long = System.currentTimeMillis()
    private var existingSold: Int = 0
    private var existingRating: Double = 0.0

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun addImages(newUris: List<Uri>) {
        val current = selectedImages.value.toMutableList()
        current.addAll(newUris)
        selectedImages.value = current.distinct()
    }

    fun removeImage(uri: Uri) {
        val current = selectedImages.value.toMutableList()
        current.remove(uri)
        selectedImages.value = current
    }

    fun loadProductById(id: String) {
        if (currentProductId == id) return
        viewModelScope.launch {
            isLoading.value = true
            productRepository.getProductById(id).onSuccess { product ->
                currentProductId = product.id
                name.value = product.name
                description.value = product.description
                price.value = product.price.toString()
                originalPrice.value = product.originalPrice.toString()
                stock.value = product.stock.toString()
                category.value = product.category

                isNew.value = product.isNew
                isActive.value = product.isActive

                model3DUrl.value = product.model3DUrl ?: ""
                sizesInput.value = product.sizes.joinToString(", ")
                colorsInput.value = product.colors.joinToString(", ")
                selectedImages.value = product.images.map { Uri.parse(it) }

                existingCreatedAt = product.createdAt
                existingSold = product.sold
                existingRating = product.rating
            }.onFailure {
                _uiEvent.send("Lỗi tải data: ${it.message}")
            }
            isLoading.value = false
        }
    }

    private suspend fun uploadOneImage(uri: Uri): String? {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(uri)
                .unsigned("gundame-storepromax")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (continuation.isActive) continuation.resume(url)
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("Cloudinary", "Lỗi upload: ${error.description}")
                        if (continuation.isActive) continuation.resume(null)
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        }
    }

    fun saveProduct() {
        if (name.value.isBlank() || price.value.isBlank()) {
            viewModelScope.launch { _uiEvent.send("Thiếu thông tin cơ bản!") }
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            try {
                val finalUrls = mutableListOf<String>()
                withContext(Dispatchers.IO) {
                    selectedImages.value.forEach { uri ->
                        val stringUri = uri.toString()
                        if (stringUri.startsWith("http")) {
                            finalUrls.add(stringUri)
                        } else {
                            val url = uploadOneImage(uri)
                            if (url != null) {
                                finalUrls.add(url)
                            }
                        }
                    }
                }

                if (finalUrls.isEmpty() && selectedImages.value.isNotEmpty()) {
                    _uiEvent.send("Lỗi upload ảnh! Kiểm tra mạng.")
                    isLoading.value = false
                    return@launch
                }

                // 2. Tạo object Product
                val newProduct = Product(
                    id = currentProductId ?: "",
                    name = name.value,
                    description = description.value,
                    price = price.value.toLongOrNull() ?: 0,
                    originalPrice = originalPrice.value.toLongOrNull() ?: 0,
                    stock = stock.value.toIntOrNull() ?: 0,
                    category = category.value,

                    // 🔥 Lấy giá trị chính xác từ State
                    isNew = isNew.value,
                    isActive = isActive.value,

                    imageUrl = finalUrls.firstOrNull() ?: "", // Ảnh đầu tiên làm avatar
                    images = finalUrls,

                    model3DUrl = if (model3DUrl.value.isBlank()) null else model3DUrl.value,
                    sizes = sizesInput.value.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    colors = colorsInput.value.split(",").map { it.trim() }.filter { it.isNotEmpty() },

                    // Giữ lại data cũ
                    createdAt = if (currentProductId == null) System.currentTimeMillis() else existingCreatedAt,
                    sold = if (currentProductId == null) 0 else existingSold,
                    rating = if (currentProductId == null) 0.0 else existingRating
                )

                // 3. Lưu vào DB
                val result = if (currentProductId == null) {
                    productRepository.addProduct(newProduct)
                } else {
                    productRepository.updateProduct(newProduct)
                }

                if (result.isSuccess) {
                    _uiEvent.send("Success")
                } else {
                    _uiEvent.send("Lỗi lưu DB: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                _uiEvent.send("Lỗi: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }

}