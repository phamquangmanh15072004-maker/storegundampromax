package com.example.storepromax.presentation.checkout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.data.api.GHNRetrofit
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.DistrictGHN
import com.example.storepromax.domain.model.GHNFeeRequest
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.ProvinceGHN
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.domain.model.WardGHN
import com.example.storepromax.domain.repository.AuthRepository
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.OrderRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.example.storepromax.domain.repository.VoucherRepository
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
    private val _shippingFee = MutableStateFlow(0L)
    val shippingFee = _shippingFee.asStateFlow()

    private val MY_SHOP_ID = 6359956
    val totalPrice: StateFlow<Long> = _displayItems.map { list -> list.sumOf { it.totalPrice } }
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
    private val _provinces = MutableStateFlow<List<ProvinceGHN>>(emptyList())
    val provinces = _provinces.asStateFlow()

    private val _districts = MutableStateFlow<List<DistrictGHN>>(emptyList())
    val districts = _districts.asStateFlow()

    private val _wards = MutableStateFlow<List<WardGHN>>(emptyList())
    val wards = _wards.asStateFlow()

    val selectedProvince = MutableStateFlow<ProvinceGHN?>(null)
    val selectedDistrict = MutableStateFlow<DistrictGHN?>(null)
    val selectedWard = MutableStateFlow<WardGHN?>(null)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            refreshVouchers()
            fetchProvincesFromGHN()
            viewModelScope.launch {
                _provinces.collect { provinceList ->
                    if (provinceList.isNotEmpty()) {
                        loadUserProfile()
                    }
                }
            }
        }
    }

    private suspend fun fetchProvincesFromGHN() {
        try {
            val response = GHNRetrofit.api.getProvinces()
            if (response.code == 200 && response.data != null) {
                _provinces.value = response.data
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun onProvinceSelected(p: ProvinceGHN) {
        selectedProvince.value = p
        selectedDistrict.value = null
        selectedWard.value = null
        _districts.value = emptyList()
        _wards.value = emptyList()

        viewModelScope.launch {
            try {
                val response = GHNRetrofit.api.getDistricts(p.provinceID)
                if (response.code == 200 && response.data != null) {
                    _districts.value = response.data
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun onDistrictSelected(d: DistrictGHN) {
        selectedDistrict.value = d
        selectedWard.value = null
        _wards.value = emptyList()

        viewModelScope.launch {
            try {
                val response = GHNRetrofit.api.getWards(d.districtID)
                if (response.code == 200 && response.data != null) {
                    _wards.value = response.data
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun onWardSelected(w: WardGHN) {
        selectedWard.value = w
        calculateShippingFee()
    }

    private fun calculateShippingFee() {
        val dId = selectedDistrict.value?.districtID ?: return
        val wCode = selectedWard.value?.wardCode ?: return

        val totalWeight = 500
        val orderValue = totalPrice.value

        viewModelScope.launch {
            try {
                _shippingFee.value = 0L

                val request = GHNFeeRequest(
                    to_district_id = dId,
                    to_ward_code = wCode,
                    weight = totalWeight,
                    insurance_value = orderValue
                )

                val response = GHNRetrofit.api.calculateFee(MY_SHOP_ID, request)

                if (response.code == 200 && response.data != null) {
                    _shippingFee.value = response.data.total
                } else {
                    _shippingFee.value = 30000L
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _shippingFee.value = 30000L
            }
        }
    }

    private fun getFullAddress(): String {
        val p = selectedProvince.value?.provinceName ?: ""
        val d = selectedDistrict.value?.districtName ?: ""
        val w = selectedWard.value?.wardName ?: ""
        return if (p.isNotBlank()) "${specificAddress.value}, $w, $d, $p" else specificAddress.value
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

        initialDiscountCode?.let { code -> list.find { it.code == code }?.let { _selectedDiscountVoucher.value = it } }
        initialFreeshipCode?.let { code -> list.find { it.code == code }?.let { _selectedFreeshipVoucher.value = it } }
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
                if (totalPrice.value < voucher.minOrderValue) onResult(false, "Đơn hàng chưa đạt mức tối thiểu")
                else if (voucher.usedCount >= voucher.usageLimit) onResult(false, "Mã đã hết lượt")
                else { toggleVoucher(voucher); onResult(true, "Áp dụng thành công!") }
            }.onFailure { onResult(false, "Mã không hợp lệ") }
        }
    }

    fun submitOrder(onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: ""
        val address = getFullAddress()
        if (name.value.isBlank() || phone.value.isBlank() || selectedProvince.value == null || selectedDistrict.value == null || selectedWard.value == null || specificAddress.value.isBlank()) {
            viewModelScope.launch { _uiEvent.send("Vui lòng chọn đầy đủ địa chỉ giao hàng!") }
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
                _uiEvent.send(error.message ?: "Có lỗi xảy ra, vui lòng thử lại!")
            }

            _isProcessing.value = false
        }
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
    private suspend fun loadUserProfile() {
        auth.currentUser?.uid?.let { id ->
            userRepository.getUserProfile(id)?.let { user ->
                name.value = user.name
                phone.value = user.phone
                specificAddress.value = user.specificAddress

                if (user.provinceId != 0) {
                    selectedProvince.value = _provinces.value.find { it.provinceID == user.provinceId }

                    val dRes = GHNRetrofit.api.getDistricts(user.provinceId)
                    if (dRes.code == 200) {
                        _districts.value = dRes.data!!
                        selectedDistrict.value = dRes.data.find { it.districtID == user.districtId }
                        val wRes = GHNRetrofit.api.getWards(user.districtId)
                        if (wRes.code == 200) {
                            _wards.value = wRes.data!!
                            selectedWard.value = wRes.data.find { it.wardCode == user.wardCode }
                            calculateShippingFee()
                        }
                    }
                } else if (user.shippingAddress.isNotBlank()) {
                }
            }
        }
    }
    private suspend fun parseAndSetAddress(fullAddress: String) {
        var remainingAddress = fullAddress
        val pList = _provinces.value

        val matchedProvince = pList.find { fullAddress.contains(it.provinceName, ignoreCase = true) }

        if (matchedProvince != null) {
            selectedProvince.value = matchedProvince
            remainingAddress = remainingAddress.replace(matchedProvince.provinceName, "", ignoreCase = true)

            try {
                val dRes = GHNRetrofit.api.getDistricts(matchedProvince.provinceID)
                if (dRes.code == 200 && dRes.data != null) {
                    _districts.value = dRes.data

                    val matchedDistrict = dRes.data.find { fullAddress.contains(it.districtName, ignoreCase = true) }

                    if (matchedDistrict != null) {
                        selectedDistrict.value = matchedDistrict
                        remainingAddress = remainingAddress.replace(matchedDistrict.districtName, "", ignoreCase = true)

                        val wRes = GHNRetrofit.api.getWards(matchedDistrict.districtID)
                        if (wRes.code == 200 && wRes.data != null) {
                            _wards.value = wRes.data

                            val matchedWard = wRes.data.find { fullAddress.contains(it.wardName, ignoreCase = true) }

                            if (matchedWard != null) {
                                selectedWard.value = matchedWard
                                remainingAddress = remainingAddress.replace(matchedWard.wardName, "", ignoreCase = true)
                                calculateShippingFee()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val cleanAddress = remainingAddress
            .replace("Tỉnh", "", ignoreCase = true)
            .replace("Thành phố", "", ignoreCase = true)
            .replace("Huyện", "", ignoreCase = true)
            .replace("Quận", "", ignoreCase = true)
            .replace("Xã", "", ignoreCase = true)
            .replace("Phường", "", ignoreCase = true)
            .replace(",", " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        specificAddress.value = cleanAddress
    }
}