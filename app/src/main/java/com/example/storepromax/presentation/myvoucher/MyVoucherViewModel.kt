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
        viewModelScope.launch {
            _isLoading.value = true

            voucherRepository.getUserVouchers(currentUserId).onSuccess { mySavedVouchers ->
                val voucherIds = mySavedVouchers.map { it.voucherId }.distinct()
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
        viewModelScope.launch {
            voucherRepository.getVoucherByCode(code.uppercase().trim()).onSuccess { voucher ->
                voucherRepository.claimVoucher(currentUserId, voucher).onSuccess {
                    fetchMyVouchers()
                    onResult(true, "Lưu mã thành công!")
                }.onFailure {
                    onResult(false, it.message ?: "Lỗi khi lưu mã")
                }
            }.onFailure {
                onResult(false, "Mã không tồn tại hoặc đã hết hạn")
            }
        }
    }
}