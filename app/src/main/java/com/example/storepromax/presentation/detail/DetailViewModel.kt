package com.example.storepromax.presentation.detail

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.example.storepromax.domain.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val savedStateHandle: SavedStateHandle,
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _state = mutableStateOf<Product?>(null)
    val state: State<Product?> = _state
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    var reviews = mutableStateOf<List<UserReview>>(emptyList())
        private set
    var averageRating = mutableStateOf(0.0)
        private set
    var totalRatingsCount = mutableIntStateOf(0)
        private set
    var userRating = mutableIntStateOf(0)
        private set
    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    private val _relatedProducts = mutableStateOf<List<Product>>(emptyList())
    val relatedProducts: State<List<Product>> = _relatedProducts
    private val currentProductId: String
        get() = savedStateHandle.get<String>("productId") ?: "unknown_id"
    init {
        savedStateHandle.get<String>("productId")?.let { id ->
            getProductDetail(id)
            loadReviews(id)
        }
    }

    private fun getProductDetail(productId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            productRepository.getProductById(productId)
                .onSuccess { product ->
                    _state.value = product
                    _isLoading.value = false
                    saveToHistory(product)
                    loadRelatedProducts(product.category, product.id)
                }
                .onFailure {
                    _isLoading.value = false
                    Log.e("DetailViewModel", "Lỗi tải sản phẩm: ${it.message}")
                }
        }
    }

    fun addToCart(quantity: Int, onResult: (Boolean, String) -> Unit) {
        val currentProduct = _state.value
        if (currentProduct != null) {
            viewModelScope.launch {
                val result = cartRepository.addToCart(currentProduct, quantity)
                if (result.isSuccess) {
                    onResult(true, "Đã thêm vào giỏ hàng!")
                } else {
                    onResult(false, result.exceptionOrNull()?.message ?: "Lỗi thêm giỏ hàng")
                }
            }
        }
    }
    private fun loadRelatedProducts(category: String, currentProductId: String) {
        viewModelScope.launch {
            Log.d("DetailViewModel", "Đang tìm SP tương tự cho category: $category")

            productRepository.getProductsPaginated(10, null, category, "createdAt", false, null, null)
                .onSuccess { pair ->
                    val filteredList = pair.first.filter { it.id != currentProductId }
                    _relatedProducts.value = filteredList
                    Log.d("DetailViewModel", "Tìm thấy ${filteredList.size} SP tương tự!")
                }
                .onFailure {
                    Log.e("DetailViewModel", "LỖI tải SP tương tự: ${it.message}")
                }
        }
    }
    private fun saveToHistory(product: Product) {
        viewModelScope.launch {
            productRepository.addToViewHistory(product)
        }
    }
    fun loadReviews(productId: String) {
        viewModelScope.launch {
            val reviewList = reviewRepository.getReviews(productId)
            reviews.value = reviewList
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val reviewWithRating = reviewList.find { it.userId == userId && it.rating > 0 }
                userRating.intValue = reviewWithRating?.rating ?: 0
            }
            calculateAverageRating(reviewList)
        }
    }
    private fun calculateAverageRating(reviewList: List<UserReview>) {
        val validRatings = reviewList.filter { it.rating > 0 }

        totalRatingsCount.intValue = validRatings.size

        if (validRatings.isNotEmpty()) {
            val sum = validRatings.sumOf { it.rating }
            averageRating.value = sum.toDouble() / validRatings.size

        } else {
            averageRating.value = 0.0
        }
    }

    fun submitComment(content: String, parentId: String?, rating: Int) {
        viewModelScope.launch {
            try {
                Log.d("ReviewDebug", "=== BẮT ĐẦU GỬI BÌNH LUẬN ===")
                Log.d("ReviewDebug", "Content: $content, Rating: $rating, ParentId: $parentId")
                Log.d("ReviewDebug", "Current Product ID: $currentProductId")
                val finalRating = if (userRating.intValue > 0 && parentId == null) {
                    0
                } else {
                    rating.takeIf { it > 0 } ?: 0
                }
                Log.d("ReviewDebug", "Đang lưu vào Firestore...")
                reviewRepository.submitComment(currentProductId, content, parentId, finalRating)
                Log.d("ReviewDebug", "Lưu thành công! Đang tải lại danh sách...")
                loadReviews(currentProductId)

            } catch (e: Exception) {
                Log.e("ReviewDebug", "LỖI RỒI: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    fun submitRating(rating: Int) {
        viewModelScope.launch {
            if (rating > 0) {
                userRating.intValue = rating
                reviewRepository.submitRating(currentProductId, rating)
                loadReviews(currentProductId)
            }
        }
    }

    fun deleteComment(reviewId: String) {
        viewModelScope.launch {
            reviewRepository.deleteReview(currentProductId, reviewId)
            loadReviews(currentProductId)
        }
    }

    fun editComment(reviewId: String, newContent: String) {
        viewModelScope.launch {
            reviewRepository.updateReview(currentProductId, reviewId, newContent)
            loadReviews(currentProductId)
        }
    }
}