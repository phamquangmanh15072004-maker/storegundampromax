package com.example.storepromax.presentation.cart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.VoucherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val voucherRepository: VoucherRepository
) : ViewModel() {

    val cartItems: StateFlow<List<CartItem>> = cartRepository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _availableVouchers = MutableStateFlow<List<Voucher>>(emptyList())
    val availableVouchers = _availableVouchers.asStateFlow()

    private val _selectedDiscountVoucher = MutableStateFlow<Voucher?>(null)
    val selectedDiscountVoucher = _selectedDiscountVoucher.asStateFlow()

    private val _selectedFreeshipVoucher = MutableStateFlow<Voucher?>(null)
    val selectedFreeshipVoucher = _selectedFreeshipVoucher.asStateFlow()

    val subTotal: StateFlow<Long> = cartItems.map { list ->
        list.filter { it.isSelected && it.product.isActive }.sumOf { it.liveTotalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun toggleSelection(item: CartItem) {
        if (!item.product.isActive) return

        viewModelScope.launch {
            cartRepository.updateSelection(item.product.id, !item.isSelected)
            checkVoucherValidity()
        }
    }

    val shippingFee: StateFlow<Long> = subTotal.map { if (it > 0) 30000L else 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val productDiscountAmount: StateFlow<Long> =
        combine(subTotal, selectedDiscountVoucher) { currentSub, voucher ->
            if (voucher == null || currentSub < voucher.minOrderValue) return@combine 0L
            if (voucher.discountType == "FIXED") {
                voucher.discountValue
            } else {
                val calc = (currentSub * voucher.discountValue) / 100
                if (voucher.maxDiscount != null && calc > voucher.maxDiscount) voucher.maxDiscount else calc
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val freeshipAmount: StateFlow<Long> =
        combine(shippingFee, selectedFreeshipVoucher) { fee, voucher ->
            if (voucher == null || fee == 0L) return@combine 0L
            minOf(fee, voucher.discountValue)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalPrice: StateFlow<Long> = combine(
        subTotal, shippingFee, productDiscountAmount, freeshipAmount
    ) { sub, ship, prodDisc, shipDisc ->
        ((sub + ship) - prodDisc - shipDisc).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        fetchAvailableVouchers()
    }

    fun fetchAvailableVouchers() {
        val currentUserId =
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            voucherRepository.getUserVouchers(currentUserId).onSuccess { savedList ->
                val availableSaved = savedList.filter { it.status == "AVAILABLE" }
                val voucherIds = availableSaved.mapNotNull { it.voucherId }.filter { it.isNotBlank() }.distinct()
                if (voucherIds.isEmpty()) {
                    _availableVouchers.value = emptyList()
                    return@onSuccess
                }
                voucherRepository.getVouchersByIds(voucherIds).onSuccess { systemVouchers ->
                    _availableVouchers.value = systemVouchers
                    checkVoucherValidity()
                }
            }
        }
    }

    fun applyVoucher(voucher: Voucher, onResult: (Boolean, String) -> Unit) {
        val currentTime = System.currentTimeMillis()

        if (voucher.startDate > currentTime) {
            onResult(false, "Voucher này chưa đến giờ sử dụng!")
            return
        }
        if (voucher.expirationDate < currentTime && voucher.expirationDate > 0L) {
            onResult(false, "Voucher này đã hết hạn!")
            return
        }
        if (!voucher.isActive) {
            onResult(false, "Mã này đã bị vô hiệu hóa!")
            return
        }
        if (subTotal.value < voucher.minOrderValue) {
            onResult(false, "Cần mua thêm để đạt tối thiểu ₫${voucher.minOrderValue}")
            return
        }
        if (voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit) {
            onResult(false, "Mã này đã hết lượt!")
            return
        }

        if (voucher.type == "FREESHIP") {
            _selectedFreeshipVoucher.value = voucher
        } else {
            _selectedDiscountVoucher.value = voucher
        }
        onResult(true, "Áp dụng mã thành công!")
    }

    fun applyVoucherByCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            voucherRepository.getVoucherByCode(code.uppercase().trim()).onSuccess { voucher ->
                applyVoucher(voucher, onResult)
            }.onFailure {
                onResult(false, it.message ?: "Mã không hợp lệ")
            }
        }
    }

    fun removeVoucher(type: String) {
        if (type == "FREESHIP") _selectedFreeshipVoucher.value = null
        else _selectedDiscountVoucher.value = null
    }

    fun increaseQuantity(item: CartItem) {
        if (item.quantity < item.product.stock) {
            viewModelScope.launch {
                cartRepository.updateQuantity(item.product.id, item.quantity + 1)
                checkVoucherValidity()
            }
        }
    }

    fun decreaseQuantity(item: CartItem) {
        if (item.quantity > 1) {
            viewModelScope.launch {
                cartRepository.updateQuantity(item.product.id, item.quantity - 1)
                checkVoucherValidity()
            }
        }
    }

    fun updateQuantity(item: CartItem, newQuantity: Int) {
        val valid = newQuantity.coerceIn(1, item.product.stock)
        viewModelScope.launch {
            cartRepository.updateQuantity(item.product.id, valid)
            checkVoucherValidity()
        }
    }

    fun removeItem(productId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(productId)
            checkVoucherValidity()
        }
    }

    private fun checkVoucherValidity() {
        val currentSub = subTotal.value
        if (_selectedDiscountVoucher.value != null && currentSub < _selectedDiscountVoucher.value!!.minOrderValue) {
            _selectedDiscountVoucher.value = null
        }
        if (_selectedFreeshipVoucher.value != null && currentSub < _selectedFreeshipVoucher.value!!.minOrderValue) {
            _selectedFreeshipVoucher.value = null
        }
    }
}
