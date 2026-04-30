package com.example.storepromax.presentation.myvoucher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.UserVoucher
import com.example.storepromax.domain.repository.VoucherRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyVoucherViewModel @Inject constructor(
    private val voucherRepository: VoucherRepository
) : ViewModel() {

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _myVouchers = MutableStateFlow<List<UserVoucher>>(emptyList())
    val myVouchers = _myVouchers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        fetchMyVouchers()
    }

    fun fetchMyVouchers() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true

            voucherRepository.getUserVouchers(currentUserId).onSuccess { mySavedVouchers ->
                val voucherIds = mySavedVouchers
                    .mapNotNull { it.voucherId }
                    .filter { it.isNotBlank() }
                    .distinct()

                if (voucherIds.isEmpty()) {
                    _myVouchers.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                voucherRepository.getVouchersByIds(voucherIds).onSuccess { latestVouchers ->
                    val systemVoucherMap = latestVouchers.associateBy { it.id }

                    val synchronizedVouchers = mySavedVouchers.mapNotNull { savedItem ->
                        val latestVoucher = systemVoucherMap[savedItem.voucherId]
                        if (latestVoucher == null) {
                            null
                        } else {
                            savedItem.copy(voucher = latestVoucher)
                        }
                    }
                    _myVouchers.value = synchronizedVouchers.sortedByDescending { it.claimedAt }
                }
            }
            _isLoading.value = false
        }
    }
    fun claimVoucherByCode(code: String, onResult: (Boolean, String) -> Unit) {
        if (code.isBlank()) {
            onResult(false, "Vui lòng nhập mã Voucher!")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            voucherRepository.getVoucherByCode(code.uppercase().trim()).onSuccess { voucher ->
                if (!voucher.isActive) {
                    onResult(false, "Mã giảm giá này đã bị vô hiệu hóa!")
                    _isLoading.value = false
                    return@launch
                }
                val currentTime = System.currentTimeMillis()
                if (voucher.startDate > currentTime) {
                    onResult(false, "Mã này chưa đến giờ sử dụng!")
                    _isLoading.value = false
                    return@launch
                }
                if (voucher.expirationDate in 1..<currentTime) {
                    onResult(false, "Mã giảm giá này đã hết hạn!")
                    _isLoading.value = false
                    return@launch
                }

                if (voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit) {
                    onResult(false, "Mã giảm giá này đã được nhập hết!")
                    _isLoading.value = false
                    return@launch
                }
                voucherRepository.claimVoucher(currentUserId, voucher).onSuccess {
                    fetchMyVouchers()
                    onResult(true, "🎉 Lưu mã thành công! Bạn có thể áp dụng trong Giỏ hàng.")
                }.onFailure {
                    onResult(false, it.message ?: "Mã này đã có trong ví của bạn!")
                }

            }.onFailure {
                onResult(false, "Mã không tồn tại hoặc đã bị xóa!")
            }
            _isLoading.value = false
        }
    }
}