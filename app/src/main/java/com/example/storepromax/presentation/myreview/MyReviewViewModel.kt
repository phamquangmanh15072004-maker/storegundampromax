package com.example.storepromax.presentation.myreview

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.domain.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ReviewUiModel(
    val review: UserReview,
    val productName: String,
    val productImageUrl: String
)

data class UnreviewedItemUiModel(
    val orderId: String,
    val productId: String,
    val productName: String,
    val productImageUrl: String,
    val orderDate: Long
)

@HiltViewModel
class MyReviewViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    var myReviews = mutableStateOf<List<ReviewUiModel>>(emptyList())
        private set
    var isLoadingReviews = mutableStateOf(true)
        private set

    var unreviewedItems = mutableStateOf<List<UnreviewedItemUiModel>>(emptyList())
        private set
    var isLoadingUnreviewed = mutableStateOf(true)
        private set

    init {
        loadMyReviews()
        loadUnreviewedItems()
    }

    fun loadMyReviews() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                isLoadingReviews.value = true
                val snapshot = firestore.collectionGroup("reviews")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserReview::class.java)?.copy(id = doc.id)
                }

                val productIds = list.map { it.productId }.distinct()
                val productInfoMap = mutableMapOf<String, Pair<String, String>>()

                withContext(Dispatchers.IO) {
                    productIds.map { pId ->
                        async {
                            try {
                                val pSnap = firestore.collection("products").document(pId).get().await()
                                if (pSnap.exists()) {
                                    val name = pSnap.getString("name") ?: "Sản phẩm không rõ"
                                    val img = pSnap.getString("imageUrl") ?: ""
                                    productInfoMap[pId] = Pair(name, img)
                                }
                            } catch (e: Exception) { }
                        }
                    }.awaitAll()
                }

                val uiModels = list.map { review ->
                    val info = productInfoMap[review.productId]
                    ReviewUiModel(
                        review = review,
                        productName = info?.first ?: "Sản phẩm không rõ",
                        productImageUrl = info?.second ?: ""
                    )
                }
                myReviews.value = uiModels
            } catch (e: Exception) {
                Log.e("MyReviewViewModel", "Lỗi tải đã đánh giá: ${e.message}")
            } finally {
                isLoadingReviews.value = false
            }
        }
    }

    fun loadUnreviewedItems() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                isLoadingUnreviewed.value = true
                val snapshot = firestore.collection("orders")
                    .whereEqualTo("userId", userId)
                    .whereIn("status", listOf("COMPLETED", "DELIVERED", "RETURN_REJECTED"))
                    .get()
                    .await()

                val unreviewedList = mutableListOf<UnreviewedItemUiModel>()

                for (doc in snapshot.documents) {
                    val orderId = doc.id
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    val reviewedProducts = doc.get("reviewedProducts") as? List<String> ?: emptyList()
                    val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList()

                    for (item in items) {
                        val productMap = item["product"] as? Map<String, Any>
                        val productId = productMap?.get("id") as? String ?: continue
                        val productName = productMap["name"] as? String ?: "Sản phẩm"
                        val productImageUrl = productMap["imageUrl"] as? String ?: ""
                        if (!reviewedProducts.contains(productId)) {
                            unreviewedList.add(
                                UnreviewedItemUiModel(
                                    orderId = orderId,
                                    productId = productId,
                                    productName = productName,
                                    productImageUrl = productImageUrl,
                                    orderDate = createdAt
                                )
                            )
                        }
                    }
                }
                unreviewedItems.value = unreviewedList.sortedByDescending { it.orderDate }
            } catch (e: Exception) {
                Log.e("MyReviewViewModel", "Lỗi tải chưa đánh giá: ${e.message}")
            } finally {
                isLoadingUnreviewed.value = false
            }
        }
    }

    fun editReview(productId: String, reviewId: String, newContent: String) {
        viewModelScope.launch {
            try {
                reviewRepository.updateReview(productId, reviewId, newContent)
                loadMyReviews()
            } catch (e: Exception) {
                Log.e("MyReviewViewModel", "Lỗi sửa đánh giá: ${e.message}")
            }
        }
    }
}
