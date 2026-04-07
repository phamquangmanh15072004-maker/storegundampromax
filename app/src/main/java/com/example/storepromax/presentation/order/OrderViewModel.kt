package com.example.storepromax.presentation.order

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.VietQRBank
import com.example.storepromax.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {
    private val _banks = MutableStateFlow<List<VietQRBank>>(emptyList())
    val banks = _banks.asStateFlow()
    val orders: StateFlow<List<Order>> = orderRepository.getOrders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelOrder(
        orderId: String,
        reason: String,
        isPaid: Boolean = false,
        bankBin: String? = null,
        bankShortName: String? = null,
        accountNumber: String? = null,
        accountName: String? = null
    ) {
        viewModelScope.launch {
            try {
                orderRepository.cancelOrder(
                    orderId, reason, isPaid,
                    bankBin, bankShortName, accountNumber, accountName
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        fetchBanks()
    }
    private fun fetchBanks() {
        viewModelScope.launch {
            try {
                val response = com.example.storepromax.data.api.VietQRRetrofit.api.getBanks()
                if (response.isSuccessful && response.body() != null) {
                    _banks.value = response.body()!!.data
                } else {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}