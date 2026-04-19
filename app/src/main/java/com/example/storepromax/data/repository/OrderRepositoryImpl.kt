package com.example.storepromax.data.repository

import android.util.Log
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : OrderRepository {

    override fun getOrders(): Flow<List<Order>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val subscription = firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(orders)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createOrder(
        order: Order,
        discountCode: String?,
        freeshipCode: String?
    ): Result<String> {
        return try {
            suspend fun getGlobalVoucherRef(code: String): com.google.firebase.firestore.DocumentReference? {
                return firestore.collection("vouchers").whereEqualTo("code", code).get().await()
                    .documents.firstOrNull()?.reference
            }

            suspend fun getUserVoucherRef(
                code: String,
                userId: String
            ): com.google.firebase.firestore.DocumentReference? {
                return firestore.collection("user_vouchers")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("voucher.code", code)
                    .whereEqualTo("status", "AVAILABLE")
                    .get().await()
                    .documents.firstOrNull()?.reference
            }

            val globalDiscountRef = if (!discountCode.isNullOrBlank()) getGlobalVoucherRef(discountCode) else null
            val userDiscountRef = if (!discountCode.isNullOrBlank()) getUserVoucherRef(discountCode, order.userId) else null

            val globalFreeshipRef = if (!freeshipCode.isNullOrBlank()) getGlobalVoucherRef(freeshipCode) else null
            val userFreeshipRef = if (!freeshipCode.isNullOrBlank()) getUserVoucherRef(freeshipCode, order.userId) else null
            val savedOrderId = firestore.runTransaction { transaction ->
                val productRefs = order.items.map { item ->
                    val docRef = firestore.collection("products").document(item.product.id)
                    val snapshot = transaction.get(docRef)
                    Triple(item, docRef, snapshot)
                }

                val globalDiscountSnap = globalDiscountRef?.let { transaction.get(it) }
                val globalFreeshipSnap = globalFreeshipRef?.let { transaction.get(it) }

                var calculatedTotalCost = 0L
                val finalItems = mutableListOf<CartItem>()

                for ((item, _, snapshot) in productRefs) {
                    val realPrice = snapshot.getLong("price") ?: 0L
                    val realCostPrice = snapshot.getLong("costPrice") ?: 0L
                    val stock = snapshot.getLong("stock") ?: 0L

                    if (realPrice != item.product.price) throw Exception("Giá sản phẩm '${item.product.name}' đã đổi, vui lòng load lại!")
                    if (stock < item.quantity) throw Exception("Sản phẩm '${item.product.name}' đã hết hàng!")

                    calculatedTotalCost += (realCostPrice * item.quantity)

                    val frozenItem = item.copy(
                        purchasedPrice = realPrice,
                        costPriceAtPurchase = realCostPrice
                    )
                    finalItems.add(frozenItem)
                }

                globalDiscountSnap?.let { snap ->
                    val limit = snap.getLong("usageLimit") ?: 0L
                    val used = snap.getLong("usedCount") ?: 0L
                    if (limit > 0 && used >= limit) throw Exception("Mã giảm giá đã hết lượt sử dụng!")
                }

                globalFreeshipSnap?.let { snap ->
                    val limit = snap.getLong("usageLimit") ?: 0L
                    val used = snap.getLong("usedCount") ?: 0L
                    if (limit > 0 && used >= limit) throw Exception("Mã Freeship đã hết lượt sử dụng!")
                }

                if (discountCode != null && userDiscountRef == null) {
                    throw Exception("Bạn đã sử dụng mã giảm giá này hoặc mã không còn hiệu lực!")
                }
                if (freeshipCode != null && userFreeshipRef == null) {
                    throw Exception("Bạn đã sử dụng mã Freeship này hoặc mã không còn hiệu lực!")
                }

                for ((item, docRef, _) in productRefs) {
                    transaction.update(docRef, "stock", FieldValue.increment(-item.quantity.toLong()))
                    transaction.update(docRef, "sold", FieldValue.increment(item.quantity.toLong()))
                }

                globalDiscountRef?.let { transaction.update(it, "usedCount", FieldValue.increment(1)) }
                globalFreeshipRef?.let { transaction.update(it, "usedCount", FieldValue.increment(1)) }
                userDiscountRef?.let { transaction.update(it, "status", "USED") }
                userFreeshipRef?.let { transaction.update(it, "status", "USED") }

                val finalOrder = order.copy(
                    items = finalItems,
                    totalCostPrice = calculatedTotalCost,
                    totalProfit = order.totalPrice - calculatedTotalCost
                )

                val orderRef = firestore.collection("orders").document(order.id)
                transaction.set(orderRef, finalOrder)

                return@runTransaction finalOrder.id
            }.await()

            Result.success(savedOrderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelOrder(
        orderId: String,
        reason: String,
        isPaid: Boolean,
        bankBin: String?,
        bankShortName: String?,
        accountNumber: String?,
        accountName: String?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val orderSnapshot = firestore.collection("orders").document(orderId).get().await()
                if (!orderSnapshot.exists()) return@withContext

                val items = orderSnapshot.get("items") as? List<Map<String, Any>> ?: emptyList()

                val batch = firestore.batch()
                val orderRef = firestore.collection("orders").document(orderId)
                val updates = mutableMapOf<String, Any>(
                    "status" to if (isPaid) "REFUNDING" else "CANCELLED",
                    "cancelReason" to reason,
                    "cancelledBy" to "USER",
                    "updatedAt" to System.currentTimeMillis()
                )

                if (isPaid) {
                    bankBin?.let { updates["refundBankBin"] = it }
                    bankShortName?.let { updates["refundBankShortName"] = it }
                    accountNumber?.let { updates["refundAccountNumber"] = it }
                    accountName?.let { updates["refundAccountName"] = it }
                }
                batch.update(orderRef, updates)

                // Trả lại kho
                for (item in items) {
                    val productMap = item["product"] as? Map<String, Any>
                    val productId = productMap?.get("id") as? String
                    val quantity = (item["quantity"] as? Number)?.toLong() ?: 1L

                    if (productId != null) {
                        val productRef = firestore.collection("products").document(productId)
                        batch.update(productRef, "stock", FieldValue.increment(quantity))
                    }
                }
                val adminNotifRef = firestore.collection("notifications").document()
                val adminNotifData = hashMapOf(
                    "title" to "Khách hàng đã hủy đơn ❌",
                    "message" to "Đơn hàng #${orderId.takeLast(6).uppercase()} vừa bị khách tự hủy. Lý do: $reason",
                    "type" to "ORDER",
                    "targetId" to orderId,
                    "targetRoles" to listOf("ADMIN", "INVENTORY"),
                    "readBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(adminNotifRef, adminNotifData)

                batch.commit().await()
                Log.d("OrderRepository", "Đã hủy đơn, hoàn kho và ghi chuông Admin thành công!")

            } catch (e: Exception) {
                Log.e("OrderRepository", "Lỗi hủy đơn: ${e.message}")
                throw e
            }
        }
    }

    override fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val subscription = firestore.collection("orders")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(orders)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Boolean> {
        return try {
            firestore.collection("orders").document(orderId).update("status", newStatus).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getOrderById(orderId: String): Flow<Order?> = callbackFlow {
        val docRef = firestore.collection("orders").document(orderId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val order = snapshot.toObject(Order::class.java)?.copy(id = snapshot.id)
                trySend(order)
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun confirmRefundWithReceipt(
        orderId: String,
        receiptUrl: String
    ): Result<Boolean> {
        return try {
            firestore.collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to "REFUNDED",
                        "refundReceiptUrl" to receiptUrl
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}