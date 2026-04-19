package com.example.storepromax.presentation.admin.product

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    var productIdState = mutableStateOf<String?>(null)
    val productId: String? = savedStateHandle["productId"]
    val isEditMode = productId != null

    var nameError = mutableStateOf<String?>(null)
    var skuError = mutableStateOf<String?>(null)
    var priceError = mutableStateOf<String?>(null)

    // 🌟 THÊM CÁC TRƯỜNG CHUẨN MỚI
    var sku = mutableStateOf("")
    var name = mutableStateOf("")
    var description = mutableStateOf("")
    var price = mutableStateOf("")
    var originalPrice = mutableStateOf("")
    var costPrice = mutableStateOf("") // Giá vốn
    var stock = mutableStateOf("")
    var weight = mutableStateOf("") // Trọng lượng
    var category = mutableStateOf("HG")

    // Đổi isNew thành isFeatured (Hàng HOT thủ công)
    var isFeatured = mutableStateOf(false)
    var isActive = mutableStateOf(true)

    var model3DUrl = mutableStateOf("")
    var sizesInput = mutableStateOf("")
    var colorsInput = mutableStateOf("")
    var selectedImages = mutableStateOf<List<Uri>>(emptyList())

    var isLoading = mutableStateOf(false)

    private var existingCreatedAt: Long = System.currentTimeMillis()
    private var existingSold: Int = 0
    private var existingRating: Double = 0.0

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init { productId?.let { loadProductById(it) } }

    private fun validateInputs(): Boolean {
        var isValid = true
        if (name.value.isBlank()) { nameError.value = "Tên không được để trống"; isValid = false } else nameError.value = null
        if (sku.value.isBlank()) { skuError.value = "Mã SKU bắt buộc nhập"; isValid = false } else skuError.value = null

        val p = price.value.toLongOrNull() ?: 0
        if (p <= 0) { priceError.value = "Giá bán phải lớn hơn 0"; isValid = false } else priceError.value = null

        return isValid
    }

    fun addImages(newUris: List<Uri>) { selectedImages.value = (selectedImages.value + newUris).distinct() }
    fun removeImage(uri: Uri) { selectedImages.value = selectedImages.value.filter { it != uri } }

    fun loadProductById(id: String) {
        if (productIdState.value == id) return
        viewModelScope.launch {
            isLoading.value = true
            productRepository.getProductById(id).onSuccess { product ->
                productIdState.value = product.id
                sku.value = product.sku
                name.value = product.name
                description.value = product.description
                price.value = product.price.toString()
                originalPrice.value = product.originalPrice.toString()
                costPrice.value = product.costPrice.toString()
                stock.value = product.stock.toString()
                weight.value = product.weight.toString()
                category.value = product.category

                isFeatured.value = product.isFeatured
                isActive.value = product.isActive
                model3DUrl.value = product.model3DUrl ?: ""
                selectedImages.value = product.images.map { Uri.parse(it) }

                existingCreatedAt = product.createdAt
                existingSold = product.sold
                existingRating = product.rating
            }
            isLoading.value = false
        }
    }

    private suspend fun uploadOneImage(uri: Uri): String? {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(uri).unsigned("gundame-storepromax").callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) { continuation.resume(resultData["secure_url"] as? String) }
                override fun onError(requestId: String, error: ErrorInfo) { continuation.resume(null) }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
        }
    }

    fun saveProduct() {
        if (!validateInputs()) return
        viewModelScope.launch {
            isLoading.value = true
            try {
                val finalUrls = withContext(Dispatchers.IO) {
                    selectedImages.value.map { uri ->
                        async { if (uri.toString().startsWith("http")) uri.toString() else uploadOneImage(uri) }
                    }.awaitAll().filterNotNull()
                }

                val productToSave = Product(
                    id = productId ?: "",
                    sku = sku.value.uppercase(),
                    name = name.value,
                    description = description.value,
                    price = price.value.toLongOrNull() ?: 0,
                    originalPrice = originalPrice.value.toLongOrNull() ?: 0,
                    costPrice = costPrice.value.toLongOrNull() ?: 0,
                    stock = stock.value.toIntOrNull() ?: 0,
                    weight = weight.value.toIntOrNull() ?: 0,
                    category = category.value,
                    isFeatured = isFeatured.value,
                    isActive = isActive.value,
                    imageUrl = finalUrls.firstOrNull() ?: "",
                    images = finalUrls,
                    model3DUrl = model3DUrl.value.ifBlank { null },
                    createdAt = if (productIdState.value == null) System.currentTimeMillis() else existingCreatedAt,
                    updatedAt = System.currentTimeMillis(),
                    sold = if (productIdState.value == null) 0 else existingSold,
                    rating = if (productIdState.value == null) 0.0 else existingRating
                )

                val result = if (isEditMode) productRepository.updateProduct(productToSave) else productRepository.addProduct(productToSave)
                if (result.isSuccess) _uiEvent.send("Success") else _uiEvent.send("Lỗi: ${result.exceptionOrNull()?.message}")
            } catch (e: Exception) {
                _uiEvent.send("Lỗi: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }
}