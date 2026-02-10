package com.example.storepromax.presentation.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.ProductRepository // Giả sử bạn có repo này
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _wishlistProducts = MutableStateFlow<List<Product>>(emptyList())
    val wishlistProducts = _wishlistProducts.asStateFlow()
    private val _wishlistIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistIds = _wishlistIds.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        getWishlist()
        getWishlistIds()
    }
    private fun getWishlistIds() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("wishlist")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _wishlistIds.value = snapshot.documents.map { it.id }.toSet()
                }
            }
    }

    fun toggleFavorite(productId: String) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = firestore.collection("users").document(uid)
            .collection("wishlist").document(productId)

        if (_wishlistIds.value.contains(productId)) {
            docRef.delete()
        } else {
            docRef.set(mapOf("timestamp" to System.currentTimeMillis()))
        }
    }
    private fun getWishlist() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("wishlist")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    val productIds = snapshot.documents.map { it.id }
                    fetchProductsByIds(productIds)
                } else {
                    _wishlistProducts.value = emptyList()
                    _isLoading.value = false
                }
            }
    }

    private fun fetchProductsByIds(ids: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            val products = mutableListOf<Product>()
            ids.forEach { id ->
                firestore.collection("products").document(id).get()
                    .addOnSuccessListener { doc ->
                        val product = doc.toObject(Product::class.java)
                        if (product != null) {
                            products.add(product.copy(id = doc.id))
                        }
                        _wishlistProducts.value = products.toList()
                    }
            }
            _isLoading.value = false
        }
    }

    fun removeFromWishlist(productId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .collection("wishlist").document(productId)
            .delete()
    }
}