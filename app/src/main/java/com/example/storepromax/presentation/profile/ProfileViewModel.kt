package com.example.storepromax.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.repository.AuthRepository
import com.example.storepromax.domain.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    init {
        checkAdminRole()
    }

    private fun checkAdminRole() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                val role = authRepository.getUserRole(userId)
                _isAdmin.value = role == "ADMIN"
            }
        }
    }

    private val allOrders = orderRepository.getOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = allOrders.map { orders ->
        orders.count { it.status == "PENDING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val shippingCount: StateFlow<Int> = allOrders.map { orders ->
        orders.count { it.status == "SHIPPING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

}