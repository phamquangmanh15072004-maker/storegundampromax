package com.example.storepromax.presentation.checkout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.District
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.Province
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.domain.model.Ward
import com.example.storepromax.domain.repository.AuthRepository
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.OrderRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.example.storepromax.domain.repository.VoucherRepository
import com.example.storepromax.utils.AddressUtils
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val userRepository: AuthRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val voucherRepository: VoucherRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var isBuyNowMode = false
    private val _displayItems = MutableStateFlow<List<CartItem>>(emptyList())
    val selectedItems: StateFlow<List<CartItem>> = _displayItems.asStateFlow()

    private val _availableVouchers = MutableStateFlow<List<Voucher>>(emptyList())
    val availableVouchers = _availableVouchers.asStateFlow()

    private val _selectedDiscountVoucher = MutableStateFlow<Voucher?>(null)
    val selectedDiscountVoucher = _selectedDiscountVoucher.asStateFlow()

    private val _selectedFreeshipVoucher = MutableStateFlow<Voucher?>(null)
    val selectedFreeshipVoucher = _selectedFreeshipVoucher.asStateFlow()

    private var initialDiscountCode: String? = null
    private var initialFreeshipCode: String? = null

    val totalPrice: StateFlow<Long> = _displayItems.map { list -> list.sumOf { it.totalPrice } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val shippingFee: StateFlow<Long> = totalPrice.map { if (it > 0) 30000L else 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val productDiscountAmount: StateFlow<Long> = combine(totalPrice, _selectedDiscountVoucher) { total, v ->
        if (v == null || total < v.minOrderValue) 0L
        else {
            val discount = if (v.discountType == "FIXED") v.discountValue else (total * v.discountValue) / 100
            val maxDisc = v.maxDiscount ?: 0L
            val finalDiscount = if (v.discountType == "PERCENT" && maxDisc > 0 && discount > maxDisc) maxDisc else discount
            if (finalDiscount > total) total else finalDiscount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val freeshipAmount: StateFlow<Long> = combine(shippingFee, _selectedFreeshipVoucher, totalPrice) { fee, v, total ->
        if (v == null || fee == 0L || total < v.minOrderValue) 0L
        else minOf(fee, v.discountValue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val finalTotalPrice: StateFlow<Long> = combine(
        totalPrice, shippingFee, productDiscountAmount, freeshipAmount
    ) { sub, ship, prodDisc, shipDisc ->
        ((sub - prodDisc) + (ship - shipDisc)).coerceAtLeast(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val name = MutableStateFlow("")
    val phone = MutableStateFlow("")
    val specificAddress = MutableStateFlow("")
    val paymentMethod = MutableStateFlow("COD")

    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces = _provinces.asStateFlow()
    private val _districts = MutableStateFlow<List<District>>(emptyList())
    val districts = _districts.asStateFlow()
    private val _wards = MutableStateFlow<List<Ward>>(emptyList())
    val wards = _wards.asStateFlow()

    val selectedProvince = MutableStateFlow<Province?>(null)
    val selectedDistrict = MutableStateFlow<District?>(null)
    val selectedWard = MutableStateFlow<Ward?>(null)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val provinceList = AddressUtils(context).getProvinces()
            _provinces.value = provinceList
            loadUserProfile(provinceList)
            refreshVouchers()
        }
    }

    fun refreshVouchers() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            voucherRepository.getUserVouchers(userId).onSuccess { list ->
                _availableVouchers.value = list.filter { it.status == "AVAILABLE" }.map { it.voucher }
                applyPendingVouchers()
            }
        }
    }
    fun setInitialVouchers(dCode: String?, fCode: String?) {
        if (!dCode.isNullOrBlank() && dCode != "null" && dCode != "{discountCode}") initialDiscountCode = dCode
        if (!fCode.isNullOrBlank() && fCode != "null" && fCode != "{freeshipCode}") initialFreeshipCode = fCode
        applyPendingVouchers()
    }
    private fun applyPendingVouchers() {
        val list = _availableVouchers.value
        if (list.isEmpty()) return

        initialDiscountCode?.let { code ->
            list.find { it.code == code }?.let { _selectedDiscountVoucher.value = it }
        }
        initialFreeshipCode?.let { code ->
            list.find { it.code == code }?.let { _selectedFreeshipVoucher.value = it }
        }
    }

    fun toggleVoucher(voucher: Voucher) {
        if (voucher.type == "FREESHIP") {
            _selectedFreeshipVoucher.value = if (_selectedFreeshipVoucher.value?.code == voucher.code) null else voucher
        } else {
            _selectedDiscountVoucher.value = if (_selectedDiscountVoucher.value?.code == voucher.code) null else voucher
        }
    }

    fun applyVoucherByCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            voucherRepository.getVoucherByCode(code.uppercase().trim()).onSuccess { voucher ->
                if (totalPrice.value < voucher.minOrderValue) {
                    onResult(false, "Đơn hàng chưa đạt mức tối thiểu")
                } else if (voucher.usedCount >= voucher.usageLimit) {
                    onResult(false, "Mã đã hết lượt")
                } else {
                    toggleVoucher(voucher)
                    onResult(true, "Áp dụng thành công!")
                }
            }.onFailure {
                onResult(false, "Mã không hợp lệ")
            }
        }
    }

    fun submitOrder(onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: ""
        val address = getFullAddress()
        if (name.value.isBlank() || phone.value.isBlank() || address.isBlank()) {
            viewModelScope.launch { _uiEvent.send("Vui lòng nhập đủ thông tin giao hàng!") }
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val finalTotal = finalTotalPrice.value

            val newOrder = Order(
                userId = userId, items = _displayItems.value, totalPrice = finalTotal,
                receiverName = name.value, receiverPhone = phone.value, address = address,
                status = "PENDING", paymentMethod = paymentMethod.value,
                paymentStatus = if (paymentMethod.value == "BANKING") "PAID" else "UNPAID",
                createdAt = System.currentTimeMillis()
            )
            val dCode = _selectedDiscountVoucher.value?.code?.takeIf { it.isNotBlank() }
            val fCode = _selectedFreeshipVoucher.value?.code?.takeIf { it.isNotBlank() }

            orderRepository.createOrder(newOrder, dCode, fCode).onSuccess { orderId ->
                if (!isBuyNowMode) _displayItems.value.forEach { cartRepository.removeFromCart(it.product.id) }
                NotificationHelper.sendOrderNotificationToAdmin(context, orderId, finalTotal.toDouble())
                onSuccess()
            }.onFailure { error ->
                val errorMessage = error.message ?: "Có lỗi xảy ra, vui lòng thử lại!"
                _uiEvent.send(errorMessage)
            }

            _isProcessing.value = false
        }
    }

    private fun getFullAddress(): String {
        val p = selectedProvince.value?.name ?: ""
        val d = selectedDistrict.value?.name ?: ""
        val w = selectedWard.value?.name ?: ""
        return if (p.isNotBlank()) "${specificAddress.value}, $w, $d, $p" else specificAddress.value
    }

    private fun loadUserProfile(provinceList: List<Province>) {
        viewModelScope.launch {
            auth.currentUser?.uid?.let { id ->
                userRepository.getUserProfile(id)?.let { user ->
                    name.value = user.name
                    phone.value = user.phone
                    if (user.shippingAddress.isNotBlank()) parseAddress(user.shippingAddress, provinceList)
                }
            }
        }
    }

    private fun parseAddress(full: String, list: List<Province>) {
        try {
            val parts = full.split(",").map { it.trim() }
            if (parts.size >= 3) {
                specificAddress.value = parts.dropLast(3).joinToString(", ")
                list.find { it.name.equals(parts.last(), true) }?.let { p ->
                    selectedProvince.value = p; _districts.value = p.getDistrictList()
                    _districts.value.find { it.name.equals(parts[parts.size-2], true) }?.let { d ->
                        selectedDistrict.value = d; _wards.value = d.getWardList()
                        selectedWard.value = _wards.value.find { it.name.equals(parts[parts.size-3], true) }
                    }
                }
            } else specificAddress.value = full
        } catch (e: Exception) { specificAddress.value = full }
    }

    fun loadSelectedCartItems() {
        isBuyNowMode = false
        viewModelScope.launch {
            val list = cartRepository.getCartItems().first()
            _displayItems.value = list.filter { it.isSelected }
        }
    }

    fun loadSingleProductForCheckout(id: String, q: Int) {
        isBuyNowMode = true
        viewModelScope.launch { productRepository.getProductById(id).onSuccess { p -> _displayItems.value = listOf(CartItem("temp", p!!, q, true)) } }
    }

    fun onNameChange(v: String) { name.value = v }
    fun onPhoneChange(v: String) { phone.value = v }
    fun onPaymentMethodChange(v: String) { paymentMethod.value = v }
    fun onSpecificAddressChange(v: String) { specificAddress.value = v }
    fun onProvinceSelected(p: Province) { selectedProvince.value = p; selectedDistrict.value = null; _districts.value = p.getDistrictList() }
    fun onDistrictSelected(d: District) { selectedDistrict.value = d; selectedWard.value = null; _wards.value = d.getWardList() }
    fun onWardSelected(w: Ward) { selectedWard.value = w }
}