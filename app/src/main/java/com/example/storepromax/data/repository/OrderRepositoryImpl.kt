package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
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
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt }
                    ?: emptyList()

                trySend(orders)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun createOrder(order: Order): Result<Boolean> {
        return try {
            val newDocRef = firestore.collection("orders").document()

            val finalOrder = order.copy(id = newDocRef.id)

            newDocRef.set(finalOrder).await()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun cancelOrder(orderId: String) {
        try {
            firestore.collection("orders").document(orderId)
                .update("status", "CANCELLED")
                .await()
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
            firestore.collection("orders").document(orderId)
                .update("status", newStatus)
                .await()
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
}