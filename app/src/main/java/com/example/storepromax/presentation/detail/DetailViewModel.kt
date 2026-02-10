package com.example.storepromax.presentation.detail

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.data.repository.FakeReviewRepository
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.ProductRepository
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
    private val productRepository: ProductRepository
) : ViewModel() {
    private val _state = mutableStateOf<Product?>(null)
    val state: State<Product?> = _state
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    private val reviewRepository = FakeReviewRepository()

    var reviews = mutableStateOf<List<UserReview>>(emptyList())
        private set
    var userRating = mutableIntStateOf(0)
        private set
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
            firestore.collection("products").document(productId)
                .get()
                .addOnSuccessListener { document ->
                    val product = document.toObject<Product>()
                    _state.value = product
                    _isLoading.value = false
                    if (product != null) {
                        saveToHistory(product)
                    }
                }
                .addOnFailureListener {
                    _isLoading.value = false
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
            val list = reviewRepository.getReviews(productId)
            reviews.value = list
            val myReview = list.find { it.userId == "me" && it.rating != null && it.rating > 0 }

            if (myReview != null) {
                userRating.intValue = myReview.rating!!
            } else {
                userRating.intValue = 0
            }
        }
    }

    fun submitRating(star: Int) {
        viewModelScope.launch {
            reviewRepository.submitRating(currentProductId, star)
            loadReviews(currentProductId)
        }
    }

    fun submitComment(content: String, parentId: String?, rating: Int) {
        viewModelScope.launch {
            val ratingToSave = if (parentId == null) rating else null
            reviewRepository.submitComment(currentProductId, content, parentId, ratingToSave)

            if (parentId == null && rating > 0) {
                userRating.intValue = rating
                reviewRepository.submitRating(currentProductId, rating)
            }
            loadReviews(currentProductId)
        }
    }

    fun deleteComment(reviewId: String) {
        viewModelScope.launch {
            reviewRepository.deleteReview(reviewId)
            loadReviews(currentProductId)
        }
    }

    fun editComment(reviewId: String, newContent: String) {
        viewModelScope.launch {
            reviewRepository.updateReview(reviewId, newContent)
            loadReviews(currentProductId)
        }
    }

    fun addToCart(quantity: Int) {
        val currentProduct = _state.value
        if (currentProduct != null) {
            viewModelScope.launch {
                cartRepository.addToCart(currentProduct,quantity)
            }
        }
    }
}