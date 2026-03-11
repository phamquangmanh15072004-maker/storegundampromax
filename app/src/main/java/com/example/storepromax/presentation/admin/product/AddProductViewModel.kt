package com.example.storepromax.presentation.admin.product

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    var priceError = mutableStateOf<String?>(null)
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
    init {
        productId?.let { loadProductById(it) }
    }
    private fun validateInputs(): Boolean {
        var isValid = true
        if (name.value.isBlank()) {
            nameError.value = "Tên không được để trống"
            isValid = false
        } else nameError.value = null

        val p = price.value.toLongOrNull() ?: 0
        val op = originalPrice.value.toLongOrNull() ?: 0
        if (p <= 0) {
            priceError.value = "Giá bán phải lớn hơn 0"
            isValid = false
        } else if (p > op && op > 0) {
            priceError.value = "Giá bán không nên lớn hơn giá gốc"
            isValid = false
        } else priceError.value = null

        return isValid
    }
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
        if (productIdState.value == id) return
        viewModelScope.launch {
            isLoading.value = true
            productRepository.getProductById(id).onSuccess { product ->
                productIdState.value = product.id
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
        if (!validateInputs()) return
        viewModelScope.launch {
            isLoading.value = true
            try {
                val finalUrls = withContext(Dispatchers.IO) {
                    selectedImages.value.map { uri ->
                        async {
                            if (uri.toString().startsWith("http")) uri.toString()
                            else uploadOneImage(uri)
                        }
                    }.awaitAll().filterNotNull()
                }

                if (finalUrls.isEmpty() && selectedImages.value.isNotEmpty()) {
                    _uiEvent.send("Không upload được ảnh!")
                    return@launch
                }
                val productToSave = Product(
                    id = productId ?: "",
                    name = name.value,
                    description = description.value,
                    price = price.value.toLongOrNull() ?: 0,
                    originalPrice = originalPrice.value.toLongOrNull() ?: 0,
                    stock = stock.value.toIntOrNull() ?: 0,
                    category = category.value,
                    isNew = isNew.value,
                    isActive = isActive.value,
                    imageUrl = finalUrls.firstOrNull() ?: "",
                    images = finalUrls,
                    model3DUrl = model3DUrl.value.ifBlank { null },
                    sizes = sizesInput.value.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    colors = colorsInput.value.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    createdAt = if (productIdState.value == null) System.currentTimeMillis() else existingCreatedAt,
                    sold = if (productIdState.value == null) 0 else existingSold,
                    rating = if (productIdState.value == null) 0.0 else existingRating
                )
                val result = if (isEditMode) {
                    productRepository.updateProduct(productToSave)
                } else {
                    productRepository.addProduct(productToSave)
                }

                if (result.isSuccess) _uiEvent.send("Success")
                else _uiEvent.send("Lỗi: ${result.exceptionOrNull()?.message}")
            } catch (e: Exception) {
                _uiEvent.send("Lỗi: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

}