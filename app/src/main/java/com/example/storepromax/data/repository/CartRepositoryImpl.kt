package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.CartRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CartRepository {

    private val userId: String get() = auth.currentUser?.uid ?: ""

    override fun getCartItems(): Flow<List<CartItem>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = firestore.collection("carts").document(userId).collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val itemId = doc.id
                        val quantity = doc.getLong("quantity")?.toInt() ?: 1
                        val isSelected = doc.getBoolean("isSelected") ?: false

                        val productMap = doc.get("product") as? Map<String, Any>

                        if (productMap != null) {
                            val product = Product(
                                id = productMap["id"] as? String ?: "",
                                sku = productMap["sku"] as? String ?: "",
                                name = productMap["name"] as? String ?: "",
                                description = productMap["description"] as? String ?: "",
                                price = (productMap["price"] as? Number)?.toLong() ?: 0L,
                                originalPrice = (productMap["originalPrice"] as? Number)?.toLong() ?: 0L,
                                costPrice = (productMap["costPrice"] as? Number)?.toLong() ?: 0L,
                                stock = (productMap["stock"] as? Number)?.toInt() ?: 0,
                                weight = (productMap["weight"] as? Number)?.toInt() ?: 0,
                                isActive = productMap["isActive"] as? Boolean ?: true,
                                isFeatured = productMap["isFeatured"] as? Boolean ?: false,
                                imageUrl = productMap["imageUrl"] as? String ?: "",
                                category = productMap["category"] as? String ?: "",
                                rating = (productMap["rating"] as? Number)?.toDouble() ?: 0.0,
                                sold = (productMap["sold"] as? Number)?.toInt() ?: 0,
                                createdAt = (productMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(), // 🌟 MỚI
                                updatedAt = (productMap["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(), // 🌟 MỚI
                                model3DUrl = productMap["model3DUrl"] as? String ?: ""
                            )
                            CartItem(
                                id = itemId,
                                product = product,
                                quantity = quantity,
                                isSelected = isSelected
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun addToCart(product: Product, quantity: Int): Result<Unit> {
        if (userId.isEmpty()) return Result.failure(Exception("Bạn chưa đăng nhập!"))

        return try {
            val cartRef = firestore.collection("carts").document(userId).collection("items").document(product.id)
            val doc = cartRef.get().await()

            if (doc.exists()) {
                val currentQty = doc.getLong("quantity")?.toInt() ?: 0
                val expectedQty = currentQty + quantity

                if (expectedQty > product.stock) {
                    val remaining = product.stock - currentQty
                    val errorMessage = if (remaining > 0) {
                        "Bạn chỉ có thể thêm tối đa $remaining sản phẩm nữa!"
                    } else {
                        "Sản phẩm này trong giỏ hàng đã đạt mức tối đa!"
                    }
                    return Result.failure(Exception(errorMessage))
                }

                val updates = mapOf(
                    "quantity" to expectedQty,
                    "product" to product
                )
                cartRef.update(updates).await()

            } else {
                if (quantity > product.stock) {
                    return Result.failure(Exception("Số lượng yêu cầu vượt quá tồn kho!"))
                }

                val cartItemMap = hashMapOf(
                    "product" to product,
                    "quantity" to quantity,
                    "isSelected" to false
                )
                cartRef.set(cartItemMap).await()
            }
            Result.success(Unit)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun removeFromCart(productId: String) {
        if (userId.isEmpty()) return
        firestore.collection("carts").document(userId)
            .collection("items").document(productId)
            .delete().await()
    }

    override suspend fun updateQuantity(productId: String, newQuantity: Int) {
        if (userId.isEmpty()) return
        if (newQuantity <= 0) {
            removeFromCart(productId)
        } else {
            firestore.collection("carts").document(userId)
                .collection("items").document(productId)
                .update("quantity", newQuantity).await()
        }
    }

    override suspend fun updateSelection(productId: String, isSelected: Boolean) {
        if (userId.isEmpty()) return
        firestore.collection("carts").document(userId)
            .collection("items").document(productId)
            .update("isSelected", isSelected).await()
    }

    override suspend fun clearCart() {
        if (userId.isEmpty()) return
        val batch = firestore.batch()
        val snapshot = firestore.collection("carts").document(userId).collection("items").get().await()
        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}