package com.example.storepromax.presentation.checkout

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper // 👈 QUAN TRỌNG: Import cái này
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.District
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.Province
import com.example.storepromax.domain.model.Ward
import com.example.storepromax.domain.repository.AuthRepository
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.OrderRepository
import com.example.storepromax.domain.repository.ProductRepository
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
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var isBuyNowMode = false
    private val _displayItems = MutableStateFlow<List<CartItem>>(emptyList())
    val selectedItems: StateFlow<List<CartItem>> = _displayItems.asStateFlow()
    private val _totalPrice = MutableStateFlow(0L)
    val totalPrice: StateFlow<Long> = _totalPrice.asStateFlow()
    val name = MutableStateFlow("")
    val phone = MutableStateFlow("")
    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces = _provinces.asStateFlow()
    private val _districts = MutableStateFlow<List<District>>(emptyList())
    val districts = _districts.asStateFlow()
    private val _wards = MutableStateFlow<List<Ward>>(emptyList())
    val wards = _wards.asStateFlow()
    val selectedProvince = MutableStateFlow<Province?>(null)
    val selectedDistrict = MutableStateFlow<District?>(null)
    val selectedWard = MutableStateFlow<Ward?>(null)
    val specificAddress = MutableStateFlow("")
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()
    val paymentMethod = MutableStateFlow("COD")

    init {
        viewModelScope.launch {
            val provinceList = AddressUtils(context).getProvinces()
            _provinces.value = provinceList
            loadUserProfile(provinceList)
        }
    }
    fun submitOrder(onSuccess: () -> Unit) {
        val currentUserId = auth.currentUser?.uid
        val fullAddress = getFullAddress()

        if (name.value.isBlank() || phone.value.isBlank() || fullAddress.isBlank()) {
            viewModelScope.launch { _uiEvent.send("Vui lòng nhập đầy đủ thông tin nhận hàng!") }
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true

            if (currentUserId != null) {
                userRepository.updateUserShippingInfo(
                    currentUserId, name.value, phone.value, fullAddress
                )
            }

            val currentPaymentMethod = paymentMethod.value
            val paymentStatus = if (currentPaymentMethod == "BANKING") "PAID" else "UNPAID"
            val finalTotalAmount = totalPrice.value + 30000

            val newOrder = Order(
                userId = currentUserId ?: "",
                items = _displayItems.value,
                totalPrice = totalPrice.value,
                receiverName = name.value,
                receiverPhone = phone.value,
                address = fullAddress,
                status = "PENDING",
                paymentMethod = currentPaymentMethod,
                paymentStatus = paymentStatus,
                createdAt = System.currentTimeMillis()
            )
            val result = orderRepository.createOrder(newOrder)

            if (result.isSuccess) {
                if (!isBuyNowMode) {
                    _displayItems.value.forEach { cartRepository.removeFromCart(it.product.id) }
                }

                val newOrderId = result.getOrNull().toString()

                NotificationHelper.sendOrderNotificationToAdmin(
                    context = context,
                    orderId = newOrderId,
                    totalAmount = finalTotalAmount.toDouble()
                )

                onSuccess()
            } else {
                _uiEvent.send("Lỗi: ${result.exceptionOrNull()?.message}")
            }
            _isProcessing.value = false
        }
    }

    private fun getFullAddress(): String {
        val p = selectedProvince.value?.name ?: ""
        val d = selectedDistrict.value?.name ?: ""
        val w = selectedWard.value?.name ?: ""
        val s = specificAddress.value
        return if (p.isNotBlank() && d.isNotBlank() && w.isNotBlank()) "$s, $w, $d, $p" else s
    }
    private fun loadUserProfile(provinceList: List<Province>) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val user = userRepository.getUserProfile(userId)
                if (user != null) {
                    name.value = user.name
                    phone.value = user.phone
                    if (user.shippingAddress.isNotBlank()) {
                        parseAddressToDropdown(user.shippingAddress, provinceList)
                    }
                }
            }
        }
    }

    private fun parseAddressToDropdown(fullAddress: String, provinceList: List<Province>) {
        try {
            val parts = fullAddress.split(",").map { it.trim() }
            if (parts.size >= 3) {
                val pName = parts.last()
                val dName = parts[parts.size - 2]
                val wName = parts[parts.size - 3]
                val specific = parts.take(parts.size - 3).joinToString(", ")
                specificAddress.value = specific

                val foundProvince = provinceList.find { it.name.equals(pName, ignoreCase = true) }
                if (foundProvince != null) {
                    selectedProvince.value = foundProvince
                    val districtList = foundProvince.getDistrictList()
                    _districts.value = districtList

                    val foundDistrict = districtList.find { it.name.equals(dName, ignoreCase = true) }
                    if (foundDistrict != null) {
                        selectedDistrict.value = foundDistrict
                        val wardList = foundDistrict.getWardList()
                        _wards.value = wardList

                        val foundWard = wardList.find { it.name.equals(wName, ignoreCase = true) }
                        if (foundWard != null) {
                            selectedWard.value = foundWard
                        }
                    }
                }
            } else {
                specificAddress.value = fullAddress
            }
        } catch (e: Exception) {
            specificAddress.value = fullAddress
        }
    }
    fun loadSelectedCartItems() {
        isBuyNowMode = false
        viewModelScope.launch {
            cartRepository.getCartItems().collect { list ->
                val filtered = list.filter { it.isSelected }
                _displayItems.value = filtered
                _totalPrice.value = filtered.sumOf { it.totalPrice }
            }
        }
    }

    fun loadSingleProductForCheckout(productId: String, quantity: Int) {
        isBuyNowMode = true
        viewModelScope.launch {
            val result = productRepository.getProductById(productId)
            val product = result.getOrNull()

            if (product != null) {
                val dummyItem = CartItem(
                    id = "temp_${System.currentTimeMillis()}",
                    product = product,
                    quantity = quantity,
                    isSelected = true
                )
                _displayItems.value = listOf(dummyItem)
                _totalPrice.value = (product.price * quantity).toLong()
            } else {
                _uiEvent.send("Không tìm thấy thông tin sản phẩm!")
            }
        }
    }

    fun onNameChange(newValue: String) { name.value = newValue }
    fun onPhoneChange(newValue: String) { phone.value = newValue }
    fun onPaymentMethodChange(method: String) { paymentMethod.value = method }
    fun onSpecificAddressChange(newValue: String) { specificAddress.value = newValue }

    fun onProvinceSelected(province: Province) {
        selectedProvince.value = province
        selectedDistrict.value = null
        selectedWard.value = null
        _districts.value = province.getDistrictList()
        _wards.value = emptyList()
    }

    fun onDistrictSelected(district: District) {
        selectedDistrict.value = district
        selectedWard.value = null
        _wards.value = district.getWardList()
    }

    fun onWardSelected(ward: Ward) {
        selectedWard.value = ward
    }
}