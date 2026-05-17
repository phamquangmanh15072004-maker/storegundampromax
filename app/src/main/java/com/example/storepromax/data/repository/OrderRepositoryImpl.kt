package com.example.storepromax.data.repository

import android.util.Log
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
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
                    close(error)
                    return@addSnapshotListener
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
            val normalizedDiscountCode = discountCode?.takeIf { it.isNotBlank() }
            val normalizedFreeshipCode = freeshipCode?.takeIf { it.isNotBlank() }

            suspend fun getGlobalVoucherRef(code: String): DocumentReference? {
                return firestore.collection("vouchers")
                    .whereEqualTo("code", code)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.reference
            }

            suspend fun getUserVoucherRef(code: String, userId: String): DocumentReference? {
                return firestore.collection("user_vouchers")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("voucher.code", code)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.reference
            }

            val globalDiscountRef = normalizedDiscountCode?.let { getGlobalVoucherRef(it) }
            val globalFreeshipRef = normalizedFreeshipCode?.let { getGlobalVoucherRef(it) }
            val userDiscountRef = normalizedDiscountCode?.let { getUserVoucherRef(it, order.userId) }
            val userFreeshipRef = normalizedFreeshipCode?.let { getUserVoucherRef(it, order.userId) }
            val orderRef = if (order.id.isBlank()) {
                firestore.collection("orders").document()
            } else {
                firestore.collection("orders").document(order.id)
            }

            val savedOrderId = firestore.runTransaction { transaction ->
                val productRefs = order.items.map { item ->
                    val docRef = firestore.collection("products").document(item.product.id)
                    val snapshot = transaction.get(docRef)
                    Triple(item, docRef, snapshot)
                }
                val globalDiscountSnap = globalDiscountRef?.let { transaction.get(it) }
                val globalFreeshipSnap = globalFreeshipRef?.let { transaction.get(it) }
                val userDiscountSnap = userDiscountRef?.let { transaction.get(it) }
                val userFreeshipSnap = userFreeshipRef?.let { transaction.get(it) }

                var calculatedTotalCost = 0L
                var calculatedSubTotal = 0L
                val finalItems = mutableListOf<CartItem>()

                for ((item, _, snapshot) in productRefs) {
                    if (!snapshot.exists()) throw Exception("San pham '${item.product.name}' khong ton tai!")

                    val realPrice = snapshot.getLong("price") ?: 0L
                    val realCostPrice = snapshot.getLong("costPrice") ?: 0L
                    val stock = snapshot.getLong("stock") ?: 0L
                    val isActive = snapshot.getBoolean("isActive") ?: true

                    if (!isActive) throw Exception("San pham '${item.product.name}' da ngung ban!")
                    if (realPrice != item.product.price) throw Exception("Gia san pham '${item.product.name}' da doi, vui long tai lai!")
                    if (stock < item.quantity) throw Exception("San pham '${item.product.name}' khong du ton kho!")

                    calculatedTotalCost += realCostPrice * item.quantity
                    calculatedSubTotal += realPrice * item.quantity
                    finalItems.add(
                        item.copy(
                            purchasedPrice = realPrice,
                            costPriceAtPurchase = realCostPrice
                        )
                    )
                }

                fun validateGlobalVoucher(code: String?, snap: DocumentSnapshot?, label: String) {
                    if (code == null) return
                    if (snap == null || !snap.exists()) throw Exception("$label khong ton tai hoac da bi xoa!")

                    val now = System.currentTimeMillis()
                    val isActive = snap.getBoolean("isActive") ?: true
                    val startDate = snap.getLong("startDate") ?: 0L
                    val expirationDate = snap.getLong("expirationDate") ?: 0L
                    val minOrderValue = snap.getLong("minOrderValue") ?: 0L
                    val usageLimit = snap.getLong("usageLimit") ?: 0L
                    val usedCount = snap.getLong("usedCount") ?: 0L

                    if (!isActive) throw Exception("$label da bi vo hieu hoa!")
                    if (startDate > now) throw Exception("$label chua den gio su dung!")
                    if (expirationDate > 0L && expirationDate < now) throw Exception("$label da het han!")
                    if (calculatedSubTotal < minOrderValue) throw Exception("Don hang chua dat gia tri toi thieu de dung $label!")
                    if (usageLimit > 0L && usedCount >= usageLimit) throw Exception("$label da het luot su dung!")
                }

                fun validateUserVoucher(code: String?, snap: DocumentSnapshot?, label: String) {
                    if (code == null) return
                    if (snap == null || !snap.exists() || snap.getString("status") != "AVAILABLE") {
                        throw Exception("Ban da su dung $label nay hoac ma khong con hieu luc!")
                    }
                }

                validateGlobalVoucher(normalizedDiscountCode, globalDiscountSnap, "Ma giam gia")
                validateGlobalVoucher(normalizedFreeshipCode, globalFreeshipSnap, "Ma freeship")
                validateUserVoucher(normalizedDiscountCode, userDiscountSnap, "ma giam gia")
                validateUserVoucher(normalizedFreeshipCode, userFreeshipSnap, "ma freeship")

                for ((item, docRef, _) in productRefs) {
                    transaction.update(docRef, "stock", FieldValue.increment(-item.quantity.toLong()))
                    transaction.update(docRef, "sold", FieldValue.increment(item.quantity.toLong()))
                }
                globalDiscountRef?.let { transaction.update(it, "usedCount", FieldValue.increment(1)) }
                globalFreeshipRef?.let { transaction.update(it, "usedCount", FieldValue.increment(1)) }
                userDiscountRef?.let { transaction.update(it, "status", "USED") }
                userFreeshipRef?.let { transaction.update(it, "status", "USED") }

                val finalOrder = order.copy(
                    id = orderRef.id,
                    items = finalItems,
                    totalCostPrice = calculatedTotalCost,
                    totalProfit = order.totalPrice - calculatedTotalCost
                )
                transaction.set(orderRef, finalOrder)
                finalOrder.id
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
                val orderRef = firestore.collection("orders").document(orderId)
                val orderSnapshot = orderRef.get().await()
                if (!orderSnapshot.exists()) return@withContext

                val userId = orderSnapshot.getString("userId")
                val discountCode = orderSnapshot.getString("discountCode")
                val freeshipCode = orderSnapshot.getString("freeshipCode")

                fun itemProductRefs(snapshot: DocumentSnapshot): List<Pair<DocumentReference, Long>> {
                    val items = snapshot.get("items") as? List<Map<String, Any>> ?: emptyList()
                    return items.mapNotNull { item ->
                        val productMap = item["product"] as? Map<String, Any>
                        val productId = productMap?.get("id") as? String
                        val quantity = (item["quantity"] as? Number)?.toLong() ?: 1L
                        productId?.let { firestore.collection("products").document(it) to quantity }
                    }
                }

                suspend fun getVoucherRefs(code: String?): Pair<DocumentReference?, DocumentReference?> {
                    if (code.isNullOrBlank() || userId.isNullOrBlank()) return null to null
                    val globalRef = firestore.collection("vouchers")
                        .whereEqualTo("code", code)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                        ?.reference
                    val userRef = firestore.collection("user_vouchers")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("voucher.code", code)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                        ?.reference
                    return globalRef to userRef
                }

                val (globalDiscountRef, userDiscountRef) = getVoucherRefs(discountCode)
                val (globalFreeshipRef, userFreeshipRef) = getVoucherRefs(freeshipCode)
                val adminNotifRef = firestore.collection("notifications").document()

                firestore.runTransaction { transaction ->
                    val freshOrderSnapshot = transaction.get(orderRef)
                    if (!freshOrderSnapshot.exists()) return@runTransaction

                    val currentStatus = freshOrderSnapshot.getString("status") ?: ""
                    if (currentStatus in listOf("CANCELLED", "REFUNDING", "REFUNDED")) {
                        return@runTransaction
                    }

                    val productRefs = itemProductRefs(freshOrderSnapshot)
                    val globalDiscountSnap = globalDiscountRef?.let { transaction.get(it) }
                    val globalFreeshipSnap = globalFreeshipRef?.let { transaction.get(it) }
                    val userDiscountSnap = userDiscountRef?.let { transaction.get(it) }
                    val userFreeshipSnap = userFreeshipRef?.let { transaction.get(it) }

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

                    transaction.update(orderRef, updates)
                    for ((productRef, quantity) in productRefs) {
                        transaction.update(productRef, "stock", FieldValue.increment(quantity))
                        transaction.update(productRef, "sold", FieldValue.increment(-quantity))
                    }

                    fun restoreGlobalVoucher(ref: DocumentReference?, snap: DocumentSnapshot?) {
                        if (ref == null || snap == null || !snap.exists()) return
                        val usedCount = snap.getLong("usedCount") ?: 0L
                        if (usedCount > 0L) {
                            transaction.update(ref, "usedCount", FieldValue.increment(-1))
                        }
                    }

                    fun restoreUserVoucher(ref: DocumentReference?, snap: DocumentSnapshot?) {
                        if (ref == null || snap == null || !snap.exists()) return
                        if (snap.getString("status") == "USED") {
                            transaction.update(ref, "status", "AVAILABLE")
                        }
                    }

                    restoreGlobalVoucher(globalDiscountRef, globalDiscountSnap)
                    restoreGlobalVoucher(globalFreeshipRef, globalFreeshipSnap)
                    restoreUserVoucher(userDiscountRef, userDiscountSnap)
                    restoreUserVoucher(userFreeshipRef, userFreeshipSnap)

                    transaction.set(
                        adminNotifRef,
                        hashMapOf(
                            "title" to "Khach hang da huy don",
                            "message" to "Don hang #${orderId.takeLast(6).uppercase()} vua bi huy. Ly do: $reason",
                            "type" to "ORDER",
                            "targetId" to orderId,
                            "targetRoles" to listOf("ADMIN", "INVENTORY"),
                            "readBy" to emptyList<String>(),
                            "createdAt" to System.currentTimeMillis()
                        )
                    )
                }.await()

                Log.d("OrderRepository", "Da huy don, hoan kho va hoan voucher thanh cong.")
            } catch (e: Exception) {
                Log.e("OrderRepository", "Loi huy don: ${e.message}")
                throw e
            }
        }
    }

    override fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val subscription = firestore.collection("orders")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
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
                close(error)
                return@addSnapshotListener
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

    override suspend fun requestReturnRefund(
        orderId: String,
        reason: String,
        description: String,
        images: List<String>,
        bankBin: String?,
        bankShortName: String?,
        accountNumber: String?,
        accountName: String?
    ) {
        withContext(Dispatchers.IO) {
            val orderRef = firestore.collection("orders").document(orderId)
            val notifRef = firestore.collection("notifications").document()

            firestore.runTransaction { transaction ->
                val orderSnap = transaction.get(orderRef)
                if (!orderSnap.exists()) {
                    throw IllegalStateException("Không tìm thấy thông tin đơn hàng!")
                }
                val status = orderSnap.getString("status").orEmpty()
                if (!isCompletedOrderStatus(status)) {
                    throw IllegalStateException("Chỉ đơn hàng đã hoàn thành mới được yêu cầu trả hàng.")
                }
                val reviewedProducts = orderSnap.get("reviewedProducts") as? List<String> ?: emptyList()
                if (reviewedProducts.isNotEmpty()) {
                    throw IllegalStateException("Đơn hàng đã có đánh giá nên không thể yêu cầu trả hàng/hoàn tiền.")
                }
                val updatedAt = normalizeTimestampMillis(orderSnap.getLong("updatedAt") ?: 0L)
                if (updatedAt <= 0L || System.currentTimeMillis() - updatedAt > RETURN_REQUEST_WINDOW_MILLIS) {
                    throw IllegalStateException("Đơn hàng đã quá thời hạn 3 ngày để yêu cầu trả hàng/hoàn tiền.")
                }

                val now = System.currentTimeMillis()
                val updates = mutableMapOf<String, Any>(
                    "status" to "RETURN_PENDING",
                    "returnReason" to reason,
                    "returnDescription" to description,
                    "returnImages" to images,
                    "updatedAt" to now
                )
                bankBin?.let { updates["refundBankBin"] = it }
                bankShortName?.let { updates["refundBankShortName"] = it }
                accountNumber?.let { updates["refundAccountNumber"] = it }
                accountName?.let { updates["refundAccountName"] = it }

                val notifData = hashMapOf(
                    "title" to "Yeu cau tra hang moi",
                    "message" to "Don hang #${orderId.takeLast(6).uppercase()} vua co yeu cau tra hang. Ly do: $reason",
                    "type" to "RETURN_REQUEST",
                    "targetId" to orderId,
                    "targetRoles" to listOf("ADMIN"),
                    "readBy" to emptyList<String>(),
                    "createdAt" to now
                )

                transaction.update(orderRef, updates)
                transaction.set(notifRef, notifData)
            }.await()
        }
    }

    override suspend fun submitReturnTrackingCode(orderId: String, trackingCode: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val batch = firestore.batch()
                val orderRef = firestore.collection("orders").document(orderId)
                batch.update(
                    orderRef,
                    mapOf(
                        "status" to "RETURNING",
                        "returnTrackingCode" to trackingCode,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                val notifRef = firestore.collection("notifications").document()
                val notifData = hashMapOf(
                    "title" to "Khach da gui hang tra",
                    "message" to "Don hang #${orderId.takeLast(6).uppercase()} da duoc khach gui buu dien. Ma van don: $trackingCode",
                    "type" to "RETURN_TRACKING",
                    "targetId" to orderId,
                    "targetRoles" to listOf("ADMIN"),
                    "readBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(notifRef, notifData)

                batch.commit().await()
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

private const val RETURN_REQUEST_WINDOW_MILLIS = 3L * 24L * 60L * 60L * 1000L

private fun normalizeTimestampMillis(raw: Long): Long {
    return if (raw in 1..9_999_999_999L) raw * 1000L else raw
}

private fun isCompletedOrderStatus(status: String): Boolean {
    return status == "COMPLETED" || status == "DELIVERED"
}
