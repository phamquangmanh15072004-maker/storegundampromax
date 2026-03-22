package com.example.storepromax.data.repository

import com.example.storepromax.data.local.dao.HistoryDao
import com.example.storepromax.data.local.entity.HistoryEntity
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.model.ProductReview
import com.example.storepromax.domain.repository.ProductRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    override suspend fun addProduct(product: Product): Result<Boolean> {
        return try {
            val docRef = firestore.collection("products").document()

            val productMap = hashMapOf(
                "id" to docRef.id,
                "name" to product.name,
                "name_lowercase" to product.name.lowercase(),
                "price" to product.price,
                "originalPrice" to product.originalPrice,
                "description" to product.description,
                "images" to product.images,
                "imageUrl" to (product.images.firstOrNull() ?: ""),
                "stock" to product.stock,
                "category" to product.category,
                "isNew" to product.isNew,
                "isActive" to product.isActive,
                "model3DUrl" to product.model3DUrl,
                "has3D" to !product.model3DUrl.isNullOrBlank(),
                "sold" to 0,
                "rating" to 0.0,
                "createdAt" to System.currentTimeMillis()
            )

            docRef.set(productMap).await()
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
            val docRef = firestore.collection("products").document(product.id)
            val productMap = hashMapOf(
                "id" to product.id,
                "name" to product.name,
                "name_lowercase" to product.name.lowercase(),
                "price" to product.price,
                "originalPrice" to product.originalPrice,
                "description" to product.description,
                "images" to product.images,
                "imageUrl" to (product.images.firstOrNull() ?: ""),
                "stock" to product.stock,
                "category" to product.category,
                "isNew" to product.isNew,
                "isActive" to product.isActive,
                "model3DUrl" to product.model3DUrl,
                "has3D" to !product.model3DUrl.isNullOrBlank(),
                "sold" to product.sold,
                "rating" to product.rating,
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(productMap).await()
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
                        transaction.update(
                            productRef,
                            "sold",
                            currentSold + Math.abs(quantityChange)
                        )
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
                    name = entity.title,
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

    override suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val allProducts = snapshot.toObjects(Product::class.java)

            val filteredProducts = allProducts.filter { product ->
                product.name.contains(query, ignoreCase = true)
            }

            Result.success(filteredProducts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncLowercaseNames(): Result<Boolean> {
        return try {
            val snapshot = firestore.collection("products").get().await()
            val batch = firestore.batch()

            for (document in snapshot.documents) {
                val name = document.getString("name") ?: ""
                batch.update(document.reference, "name_lowercase", name.lowercase())
            }

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearViewHistory() {
        historyDao.clearHistory()
    }
    override suspend fun getProductsPaginated(
        limit: Long,
        lastDocument: DocumentSnapshot?,
        category: String,
        sortBy: String,
        isAscending: Boolean,
        minPrice: Long?,
        maxPrice: Long?
    ): Result<Pair<List<Product>, DocumentSnapshot?>> {
        return try {
            var query: Query = firestore.collection("products")
            if (category == "3D Model") {
                query = query.whereEqualTo("has3D", true)
            } else if (category != "All") {
                query = query.whereEqualTo("category", category)
            }
            if (minPrice != null) {
                query = query.whereGreaterThanOrEqualTo("price", minPrice)
            }
            if (maxPrice != null) {
                query = query.whereLessThanOrEqualTo("price", maxPrice)
            }
            val direction = if (isAscending) Query.Direction.ASCENDING else Query.Direction.DESCENDING

            if (minPrice != null || maxPrice != null) {
                query = query.orderBy("price", direction)
            } else {
                query = query.orderBy(sortBy, direction)
            }

            query = query.limit(limit)
            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }
            val snapshot = query.get().await()
            val products = snapshot.toObjects(Product::class.java)
            val newLastDoc = if (snapshot.documents.isNotEmpty()) snapshot.documents.last() else null

            Result.success(Pair(products, newLastDoc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getProductReviews(productId: String): Result<List<ProductReview>> {
        return try {
            val snapshot = firestore.collection("products")
                .document(productId)
                .collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { doc ->
                ProductReview(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "Khách hàng ẩn danh",
                    rating = doc.getLong("rating")?.toInt() ?: 5,
                    comment = doc.getString("comment") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            }
            Result.success(reviews)
        } catch (e: Exception) {
            android.util.Log.e(
                "FirebaseError",
                "Lỗi lấy đánh giá cho sản phẩm $productId: ${e.message}"
            )
            Result.failure(e)
        }
    }
}