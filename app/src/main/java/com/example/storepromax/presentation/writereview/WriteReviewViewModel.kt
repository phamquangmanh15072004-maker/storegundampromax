package com.example.storepromax.presentation.writereview

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WriteReviewViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val reviewRepository: ReviewRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _productsToReview = MutableStateFlow<List<Product>>(emptyList())
    val productsToReview = _productsToReview.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadProductsFromOrder()
    }

    private fun loadProductsFromOrder() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val orderSnapshot = firestore.collection("orders").document(orderId).get().await()
                val items = orderSnapshot.get("items") as? List<Map<String, Any>>
                if (items == null) {
                    _productsToReview.value = emptyList()
                    return@launch
                }
                val userReviewsSnapshot = firestore.collection("reviews")
                    .whereEqualTo("userId", userId)
                    .get().await()
                val alreadyReviewedProductIds = userReviewsSnapshot.documents.mapNotNull { it.getString("productId") }.toSet()
                val productList = mutableListOf<Product>()
                for (item in items) {
                    val productMap = item["product"] as? Map<String, Any>
                    val productId = productMap?.get("id") as? String ?: continue
                    if (alreadyReviewedProductIds.contains(productId)) {
                        continue
                    }

                    val productSnapshot = firestore.collection("products").document(productId).get().await()
                    val product = productSnapshot.toObject(Product::class.java)
                    if (product != null) {
                        productList.add(product.copy(id = productSnapshot.id))
                    }
                }
                if (productList.isEmpty()) {
                    firestore.collection("orders").document(orderId)
                        .update("status", "COMPLETED_REVIEWED")
                }

                _productsToReview.value = productList

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitReview(
        productId: String,
        rating: Int,
        text: String,
        localMediaUris: List<String>,
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
                    onResult(false, "Lỗi tải ảnh/video!")
                    return@launch
                }

                reviewRepository.submitComment(productId, text, null, rating, cloudUrls)

                firestore.collection("orders").document(orderId)
                    .update("reviewedProducts", FieldValue.arrayUnion(productId))
                    .await()

                onResult(true, "Đánh giá thành công!")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Lỗi gửi đánh giá!")
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
                        Log.e("Cloudinary", "Lỗi upload Đánh giá: ${error.description}")
                        if (continuation.isActive) continuation.resumeWith(Result.success(null))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        }
    }
}