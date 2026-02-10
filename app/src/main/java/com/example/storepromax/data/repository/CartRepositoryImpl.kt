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
                // 1. Kiểm tra lỗi snapshot trước
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        // Lấy dữ liệu cơ bản
                        val itemId = doc.id
                        val quantity = doc.getLong("quantity")?.toInt() ?: 1
                        val isSelected = doc.getBoolean("isSelected") ?: false

                        // Lấy map product an toàn
                        val productMap = doc.get("product") as? Map<String, Any>

                        if (productMap != null) {
                            // 🔥 QUAN TRỌNG: Hãy chắc chắn các tên trường (key) khớp với Model Product
                            // Và kiểu dữ liệu (Long/Double) phải chuẩn.
                            val product = Product(
                                id = productMap["id"] as? String ?: "",
                                name = productMap["name"] as? String ?: "",
                                description = productMap["description"] as? String ?: "",
                                // Lưu ý: Firebase số nguyên là Long, số thực là Double.
                                // Dùng 'as? Number' rồi toLong()/toDouble() là an toàn nhất.
                                price = (productMap["price"] as? Number)?.toLong() ?: 0L,
                                originalPrice = (productMap["originalPrice"] as? Number)?.toLong() ?: 0L,
                                stock = (productMap["stock"] as? Number)?.toInt() ?: 0,
                                isNew = productMap["isNew"] as? Boolean ?: false,
                                isActive = productMap["isActive"] as? Boolean ?: true,
                                imageUrl = productMap["imageUrl"] as? String ?: "",
                                category = productMap["category"] as? String ?: "",
                                rating = (productMap["rating"] as? Number)?.toDouble() ?: 0.0,
                                sold = (productMap["sold"] as? Number)?.toInt() ?: 0,
                                // 🔥 Nếu class Product của bạn có thêm trường 'model3DUrl'
                                // thì nhớ thêm dòng này vào, nếu không sẽ lỗi thiếu tham số:
                                model3DUrl = productMap["model3DUrl"] as? String ?: ""
                            )

                            // Trả về CartItem
                            CartItem(
                                id = itemId,
                                product = product,
                                quantity = quantity,
                                isSelected = isSelected
                            )
                        } else {
                            null // Bỏ qua nếu không có product map
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null // Bỏ qua item lỗi để không crash app
                    }
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun addToCart(product: Product, quantity: Int) {
        if (userId.isEmpty()) return

        val cartRef = firestore.collection("carts").document(userId).collection("items").document(product.id)

        // Kiểm tra xem hàng đã có chưa để cộng dồn
        val doc = cartRef.get().await()
        if (doc.exists()) {
            val currentQty = doc.getLong("quantity")?.toInt() ?: 0
            cartRef.update("quantity", currentQty + quantity).await()
        } else {
            // Lưu toàn bộ object CartItem (Bao gồm cả Product data) lên Firebase
            // Để lúc lấy về không cần query bảng Product nữa
            val cartItemMap = hashMapOf(
                "product" to product,
                "quantity" to quantity,
                "isSelected" to false
            )
            cartRef.set(cartItemMap).await()
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

    override suspend fun decreaseStock(productId: String, quantity: Int) {
        try {
            firestore.collection("products").document(productId)
                .update("stock", FieldValue.increment(-quantity.toLong()))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}