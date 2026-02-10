package com.example.storepromax.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat
import javax.inject.Inject

data class DashboardStats(
    val newOrdersCount: Int = 0,
    val totalRevenue: Long = 0,
    val totalUsers: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _stats = MutableStateFlow(DashboardStats())
    val stats = _stats.asStateFlow()
    private val _adminName = MutableStateFlow("Chỉ huy")
    val adminName = _adminName.asStateFlow()
    init {
        fetchDashboardStats()
        fetchAdminProfile()
    }
    private fun fetchAdminProfile() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                try {
                    val document = firestore.collection("users").document(userId).get().await()
                    val name = document.getString("name")

                    if (!name.isNullOrEmpty()) {
                        _adminName.value = name
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun fetchDashboardStats() {
        viewModelScope.launch {
            try {
                val ordersSnapshot = firestore.collection("orders").get().await()
                val newOrders = ordersSnapshot.documents.count {
                    it.getString("status") == "PENDING"
                }
                val revenue = ordersSnapshot.documents
                    .filter { it.getString("status") == "DELIVERED" }
                    .sumOf { it.getLong("totalPrice") ?: 0L }
                val usersSnapshot = firestore.collection("users").get().await()
                val usersCount = usersSnapshot.size() // Đếm tất cả user

                _stats.value = DashboardStats(
                    newOrdersCount = newOrders,
                    totalRevenue = revenue,
                    totalUsers = usersCount
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun formatCurrency(amount: Long): String {
        val formatter = DecimalFormat("#,###")
        return "${formatter.format(amount)}đ"
    }
}