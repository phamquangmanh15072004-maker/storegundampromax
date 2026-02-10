package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getProducts(): Result<List<Product>>

    suspend fun getProductById(productId: String): Result<Product>

    suspend fun searchProducts(query: String): Result<List<Product>>
    suspend fun addProduct(product: Product): Result<Boolean>
    suspend fun deleteProduct(productId: String): Result<Boolean>
    suspend fun updateProduct(product: Product): Result<Boolean>
    suspend fun updateProductStock(productId: String, quantityChange: Int)
    suspend fun deleteAllProducts()
    suspend fun addToViewHistory(product: Product)
    fun getViewHistory(): Flow<List<Product>>
    suspend fun clearViewHistory()
}