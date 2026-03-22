package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.model.ProductReview
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getProducts(): Result<List<Product>>

    suspend fun getProductById(productId: String): Result<Product>

    suspend fun addProduct(product: Product): Result<Boolean>
    suspend fun deleteProduct(productId: String): Result<Boolean>
    suspend fun updateProduct(product: Product): Result<Boolean>
    suspend fun updateProductStock(productId: String, quantityChange: Int)
    suspend fun deleteAllProducts()
    suspend fun addToViewHistory(product: Product)
    fun getViewHistory(): Flow<List<Product>>
    suspend fun clearViewHistory()
    suspend fun searchProducts(query: String): Result<List<Product>>

    suspend fun syncLowercaseNames(): Result<Boolean>
    suspend fun getProductsPaginated(
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null,
        category: String,
        sortBy: String = "createdAt",
        isAscending: Boolean = false,
        minPrice: Long? = null,
        maxPrice: Long? = null
    ): Result<Pair<List<Product>, DocumentSnapshot?>>
    suspend fun getProductReviews(productId: String): Result<List<ProductReview>>
}