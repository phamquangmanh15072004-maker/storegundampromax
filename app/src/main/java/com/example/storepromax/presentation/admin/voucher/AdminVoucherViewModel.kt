package com.example.storepromax.presentation.admin.voucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.domain.repository.VoucherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminVoucherViewModel @Inject constructor(
    private val voucherRepository: VoucherRepository
) : ViewModel() {

    private val _allVouchers = MutableStateFlow<List<Voucher>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredVouchers = combine(_allVouchers, _searchQuery) { vouchers, query ->
        if (query.isBlank()) {
            vouchers
        } else {
            vouchers.filter {
                it.code.contains(query, ignoreCase = true) ||
                        it.title.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchAllVouchersForAdmin()
    }

    fun fetchAllVouchersForAdmin() {
        viewModelScope.launch {
            _isLoading.value = true
            voucherRepository.getAllVouchersForAdmin().onSuccess { list ->
                _allVouchers.value = list.sortedByDescending { it.expirationDate }
            }
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun toggleVoucherStatus(voucherId: String, currentStatus: Boolean) {
        val newStatus = !currentStatus
        _allVouchers.value = _allVouchers.value.map {
            if (it.id == voucherId) it.copy(isActive = newStatus) else it
        }
        viewModelScope.launch {
            voucherRepository.updateVoucherStatus(voucherId, newStatus)
        }
    }

    fun saveVoucher(voucher: Voucher, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            voucherRepository.saveVoucher(voucher).onSuccess {
                fetchAllVouchersForAdmin()
                onComplete(true, "Lưu Voucher thành công!")
            }.onFailure {
                onComplete(false, "Lỗi: ${it.message}")
            }
        }
    }

    fun getVoucherById(id: String, onResult: (Voucher?) -> Unit) {
        viewModelScope.launch {
            try {
                val list = _allVouchers.first { it.isNotEmpty() }
                val voucher = list.find { it.id == id }
                onResult(voucher)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun extendVoucher(voucher: Voucher, newExpiryDate: Long, newLimit: Long, onComplete: (Boolean, String) -> Unit) {
        val extendedVoucher = voucher.copy(
            expirationDate = newExpiryDate,
            usageLimit = newLimit,
            usedCount = 0L,
            isActive = true
        )
        saveVoucher(extendedVoucher, onComplete)
    }

    fun editVoucherInfo(voucher: Voucher, newTitle: String, newDiscount: Long, onComplete: (Boolean, String) -> Unit) {
        val editedVoucher = voucher.copy(
            title = newTitle,
            discountValue = newDiscount
        )
        saveVoucher(editedVoucher, onComplete)
    }
}