package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
                if (error != null) { close(error); return@addSnapshotListener }
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
            var newOrderId = ""
            suspend fun getGlobalVoucherRef(code: String): com.google.firebase.firestore.DocumentReference? {
                return firestore.collection("vouchers").whereEqualTo("code", code).get().await()
                    .documents.firstOrNull()?.reference
            }
            suspend fun getUserVoucherRef(code: String, userId: String): com.google.firebase.firestore.DocumentReference? {
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

            firestore.runTransaction { transaction ->
                val productRefs = order.items.map { item ->
                    val docRef = firestore.collection("products").document(item.product.id)
                    val snapshot = transaction.get(docRef)
                    Triple(item, docRef, snapshot)
                }

                val globalDiscountSnap = globalDiscountRef?.let { transaction.get(it) }
                val globalFreeshipSnap = globalFreeshipRef?.let { transaction.get(it) }

                for ((item, _, snapshot) in productRefs) {
                    val realPrice = snapshot.getLong("price") ?: 0L
                    if (realPrice != item.product.price) throw Exception("Giá sản phẩm đã đổi, vui lòng load lại!")
                    val stock = snapshot.getLong("stock") ?: 0L
                    if (stock < item.quantity) throw Exception("Sản phẩm '${item.product.name}' đã hết hàng!")
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

                val orderRef = firestore.collection("orders").document()
                newOrderId = orderRef.id
                transaction.set(orderRef, order.copy(id = newOrderId))

            }.await()

            Result.success(newOrderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelOrder(orderId: String,reason:String) {
        try {
            firestore.runTransaction { transaction ->
                val orderRef = firestore.collection("orders").document(orderId)
                val snapshot = transaction.get(orderRef)
                val order = snapshot.toObject(Order::class.java)

                if (order != null && order.status != "CANCELLED") {
                    transaction.update(orderRef, "status", "CANCELLED")
                    transaction.update(orderRef, "cancelReason", reason)

                    for (item in order.items) {
                        val productRef = firestore.collection("products").document(item.product.id)
                        transaction.update(productRef, "stock", FieldValue.increment(item.quantity.toLong()))
                        transaction.update(productRef, "sold", FieldValue.increment(-item.quantity.toLong()))
                    }
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val subscription = firestore.collection("orders")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
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
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null && snapshot.exists()) {
                val order = snapshot.toObject(Order::class.java)?.copy(id = snapshot.id)
                trySend(order)
            } else { trySend(null) }
        }
        awaitClose { subscription.remove() }
    }
}