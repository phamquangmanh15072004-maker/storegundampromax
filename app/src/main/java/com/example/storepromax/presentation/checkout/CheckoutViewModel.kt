package com.example.storepromax.presentation.checkout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.data.api.BackendRetrofit
import com.example.storepromax.data.api.GHNRetrofit
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.DistrictGHN
import com.example.storepromax.domain.model.GHNFeeRequest
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.PaymentRequest
import com.example.storepromax.domain.model.ProvinceGHN
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.domain.model.WardGHN
import com.example.storepromax.domain.repository.AuthRepository
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.NotificationRepository
import com.example.storepromax.domain.repository.OrderRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.example.storepromax.domain.repository.VoucherRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth
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
    val totalPrice: StateFlow<Long> = _displayItems.map { list -> list.sumOf { it.liveTotalPrice } }
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

    private var paymentListenerJob: Job? = null
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

    val shippingMethod = MutableStateFlow("STANDARD")

    fun onShippingMethodChange(method: String) {
        shippingMethod.value = method
        calculateShippingFee()
    }

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

    private fun listenToOrderPaymentStatus(orderId: String, totalAmount: Double) {
        paymentListenerJob?.cancel()
        paymentListenerJob = viewModelScope.launch {
            orderRepository.getOrderById(orderId).collect { order ->
                if (order != null && order.paymentStatus == "PAID") {
                    _uiEvent.send("PAYMENT_SUCCESS")
                    notificationRepository.sendOrderNotificationToAdmin(orderId, totalAmount)
                    paymentListenerJob?.cancel()
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

    fun cancelOrderFromPopup(orderId: String) {
        viewModelScope.launch {
            paymentListenerJob?.cancel()
            orderRepository.cancelOrder(
                orderId = orderId,
                reason = "Khách hàng hủy từ màn hình thanh toán",
                isPaid = false,
                bankBin = null,
                bankShortName = null,
                accountNumber = null,
                accountName = null
            )
            _uiEvent.send("Đã hủy đơn hàng để đặt lại!")
        }
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

        viewModelScope.launch {
            try {
                val request = GHNFeeRequest(
                    to_district_id = dId,
                    to_ward_code = wCode,
                    weight = 500,
                    service_type_id = 2,
                    insurance_value = totalPrice.value
                )
                val response = GHNRetrofit.api.calculateFee(MY_SHOP_ID, request)

                val baseFee = if (response.code == 200 && response.data != null) {
                    response.data.total
                } else {
                    30000L
                }

                if (shippingMethod.value == "EXPRESS") {
                    _shippingFee.value = baseFee + 15000L
                } else {
                    _shippingFee.value = baseFee
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _shippingFee.value = if (shippingMethod.value == "EXPRESS") 45000L else 30000L
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
            voucherRepository.getUserVouchers(userId).onSuccess { savedList ->
                val availableSaved = savedList.filter { it.status == "AVAILABLE" }
                val voucherIds = availableSaved.map { it.voucherId }.distinct()

                if (voucherIds.isEmpty()) {
                    _availableVouchers.value = emptyList()
                    return@onSuccess
                }

                voucherRepository.getVouchersByIds(voucherIds).onSuccess { systemVouchers ->
                    val currentTime = System.currentTimeMillis()

                    val validVouchers = systemVouchers.filter { v ->
                        val expTime = (v.expirationDate as? Number)?.toLong() ?: 0L
                        val isNotExpired = expTime == 0L || expTime > currentTime

                        v.isActive && isNotExpired && v.usedCount < v.usageLimit
                    }

                    _availableVouchers.value = validVouchers
                    applyPendingVouchers()
                }
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

    fun submitOrder(onSuccess: () -> Unit, onShowPaymentPopup: (String, String, String, String, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: ""
        val address = getFullAddress()

        if (name.value.isBlank() || phone.value.isBlank() || selectedProvince.value == null || selectedDistrict.value == null || selectedWard.value == null || specificAddress.value.isBlank()) {
            viewModelScope.launch { _uiEvent.send("Vui lòng chọn đầy đủ địa chỉ giao hàng!") }
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val finalTotal = finalTotalPrice.value
            val orderId = System.currentTimeMillis().toString().takeLast(9)
            val dCode = _selectedDiscountVoucher.value?.code?.takeIf { it.isNotBlank() }
            val fCode = _selectedFreeshipVoucher.value?.code?.takeIf { it.isNotBlank() }

            val newOrder = Order(
                id = orderId,
                userId = userId,
                items = _displayItems.value,
                totalPrice = finalTotal,
                receiverName = name.value,
                receiverPhone = phone.value,
                address = address,
                paymentMethod = paymentMethod.value,
                shippingMethod = shippingMethod.value,
                paymentStatus = "UNPAID",
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                discountCode = dCode,
                freeshipCode = fCode
            )

            orderRepository.createOrder(newOrder, dCode, fCode).onSuccess {
                if (!isBuyNowMode) {
                    _displayItems.value.forEach { cartRepository.removeFromCart(it.product.id) }
                }

                if (paymentMethod.value == "BANKING") {
                    try {
                        val response = BackendRetrofit.api.createPaymentLink(
                            PaymentRequest(orderId, finalTotal, "Thanh toan don $orderId")
                        )
                        if (response.isSuccessful && response.body()?.success == true) {
                            val body = response.body()!!
                            onShowPaymentPopup(
                                body.checkoutUrl ?: "",
                                body.bin ?: "",
                                body.accountNumber ?: "",
                                body.description ?: "",
                                orderId
                            )
                            listenToOrderPaymentStatus(orderId, finalTotal.toDouble())
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: response.message()
                            _uiEvent.send("Server PayOS từ chối: $errorMsg")
                        }
                    } catch (e: Exception){
                        _uiEvent.send("Lỗi kết nối Server thanh toán: ${e.message}")
                    }
                } else {
                    notificationRepository.sendOrderNotificationToAdmin(orderId, finalTotal.toDouble())
                    Log.d("DEBUG_NOTIF", "Bắt đầu gọi hàm gửi thông báo cho Admin!")
                    onSuccess()
                }

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
                }
            }
        }
    }
}