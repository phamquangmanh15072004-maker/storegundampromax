package com.example.storepromax.data.repository

import com.example.storepromax.data.local.dao.HistoryDao
import com.example.storepromax.data.local.entity.HistoryEntity
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val historyDao: HistoryDao
) : ProductRepository {

    override suspend fun getProducts(): Result<List<Product>> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val products = snapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val document = firestore.collection("products").document(productId).get().await()
            val product = document.toObject(Product::class.java)
            if (product != null) {
                Result.success(product)
            } else {
                Result.failure(Exception("Product not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val snapshot = firestore.collection("products")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get()
                .await()

            val products = snapshot.toObjects(Product::class.java)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun addProduct(product: Product): Result<Boolean> {
        return try {
            val docRef = firestore.collection("products").document()

            val finalProduct = product.copy(id = docRef.id)

            docRef.set(finalProduct).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun deleteProduct(productId: String): Result<Boolean> {
        return try {
            firestore.collection("products").document(productId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun updateProduct(product: Product): Result<Boolean> {
        return try {
            firestore.collection("products").document(product.id).set(product).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun updateProductStock(productId: String, quantityChange: Int) {
        try {
            firestore.runTransaction { transaction ->
                val productRef = firestore.collection("products").document(productId)
                val snapshot = transaction.get(productRef)
                val currentStock = snapshot.getLong("stock") ?: 0
                val newStock = currentStock + quantityChange
                if (newStock >= 0) {
                    transaction.update(productRef, "stock", newStock)
                    if (quantityChange < 0) {
                        val currentSold = snapshot.getLong("sold") ?: 0
                        transaction.update(productRef, "sold", currentSold + Math.abs(quantityChange))
                    }
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override suspend fun deleteAllProducts() {
        try {
            val snapshot = firestore.collection("products").get().await()
            val batch = firestore.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    override suspend fun addToViewHistory(product: Product) {
        val entity = HistoryEntity(
            id = product.id,
            title = product.name,
            price = product.price,
            images = product.images,
            userId = "system",
            userName = "StoreProMax",
            userAvatar = "",
            content = product.description,
            condition = "NEW",
            grade = "N/A",
            likeCount = 0,
            commentCount = 0,
            status = "AVAILABLE",

            createdAt = System.currentTimeMillis(),
            viewedAt = System.currentTimeMillis()
        )
        historyDao.insert(entity)
    }

    override fun getViewHistory(): Flow<List<Product>> {
        return historyDao.getViewHistory().map { entities ->
            entities.map { entity ->
                Product(
                    id = entity.id,
                    name = entity.title ,
                    price = entity.price,
                    images = entity.images,
                    description = entity.content,
                    stock = 0,
                    category = "",
                    sold = 0,
                    rating = 0.0
                )
            }
        }
    }

    override suspend fun clearViewHistory() {
        historyDao.clearHistory()
    }
}