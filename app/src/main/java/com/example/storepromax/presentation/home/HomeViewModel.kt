package com.example.storepromax.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.example.storepromax.domain.repository.VoucherRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val chatRepo: ChatRepository,
    private val cartRepository: CartRepository,
    private val voucherRepository: VoucherRepository
) : ViewModel() {

    private var _allProducts = mutableListOf<Product>()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _newArrivals = MutableStateFlow<List<Product>>(emptyList())
    val newArrivals = _newArrivals.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating = _isPaginating.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private var lastDocument: DocumentSnapshot? = null
    var isLastPage = false
    private val pageSize = 4L

    private val _currentSortBy = MutableStateFlow("createdAt")
    val currentSortBy = _currentSortBy.asStateFlow()

    private val _currentIsAscending = MutableStateFlow(false)
    val currentIsAscending = _currentIsAscending.asStateFlow()

    private val _currentMinPrice = MutableStateFlow<Long?>(null)
    val currentMinPrice = _currentMinPrice.asStateFlow()

    private val _currentMaxPrice = MutableStateFlow<Long?>(null)
    val currentMaxPrice = _currentMaxPrice.asStateFlow()

    private val _voucherOnHome = MutableStateFlow<List<com.example.storepromax.domain.model.Voucher>>(emptyList())
    val voucherOnHome = _voucherOnHome.asStateFlow()

    private val _userVoucherIds = MutableStateFlow<List<String>>(emptyList())
    val userVoucherIds = _userVoucherIds.asStateFlow()

    init {
        loadGlobalNewArrivals()
        loadInitialProducts()
        loadVouchers()
    }

    fun loadInitialProducts(category: String = _selectedCategory.value) {
        viewModelScope.launch {
            _isLoading.value = true
            lastDocument = null
            isLastPage = false
            _isPaginating.value = false
            _allProducts = mutableListOf()

            productRepository.getProductsPaginated(
                pageSize, null, category, sortBy = _currentSortBy.value,
                isAscending = _currentIsAscending.value, minPrice = currentMinPrice.value, maxPrice = currentMaxPrice.value
            ).onSuccess { (list, lastDoc) ->
                _allProducts.addAll(list)
                lastDocument = lastDoc
                if (list.size < pageSize) isLastPage = true
                _products.value = _allProducts.toList()
                _isLoading.value = false
            }.onFailure {
                _isLoading.value = false
            }
        }
    }
    fun refreshHomeData() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            productRepository.getProductsPaginated(
                pageSize, null, _selectedCategory.value, sortBy = _currentSortBy.value,
                isAscending = _currentIsAscending.value, minPrice = currentMinPrice.value, maxPrice = currentMaxPrice.value
            ).onSuccess { (list, lastDoc) ->
                _allProducts.clear()
                _allProducts.addAll(list)
                lastDocument = lastDoc
                isLastPage = list.size < pageSize
                _isPaginating.value = false
                _products.value = _allProducts.toList()
                _isRefreshing.value = false
            }.onFailure {
                _isRefreshing.value = false
            }
            loadGlobalNewArrivals()
            loadVouchers()
        }
    }
    fun silentSyncProducts() {
        if (_allProducts.isEmpty()) return
        viewModelScope.launch {
            val currentLoadedSize = _allProducts.size.toLong()
            productRepository.getProductsPaginated(
                limit = currentLoadedSize, lastDocument = null, category = _selectedCategory.value,
                sortBy = _currentSortBy.value, isAscending = _currentIsAscending.value,
                minPrice = currentMinPrice.value, maxPrice = currentMaxPrice.value
            ).onSuccess { (list, lastDoc) ->
                _allProducts.clear()
                _allProducts.addAll(list)
                _products.value = _allProducts.toList()
                lastDocument = lastDoc
            }
        }
    }

    fun loadMoreProducts() {
        if (isLastPage || _isPaginating.value || _isLoading.value || _isRefreshing.value) return

        viewModelScope.launch {
            _isPaginating.value = true
            productRepository.getProductsPaginated(
                pageSize, lastDocument, _selectedCategory.value, sortBy = currentSortBy.value,
                isAscending = currentIsAscending.value, minPrice = currentMinPrice.value, maxPrice = currentMaxPrice.value
            ).onSuccess { (list, lastDoc) ->
                if (list.isNotEmpty()) {
                    val uniqueNewItems = list.filter { newItem -> _allProducts.none { existingItem -> existingItem.id == newItem.id } }
                    _allProducts.addAll(uniqueNewItems)
                    lastDocument = lastDoc
                    _products.value = _allProducts.toList()
                }
                if (list.size < pageSize) isLastPage = true
                _isPaginating.value = false
            }.onFailure {
                _isPaginating.value = false
            }
        }
    }

    private fun loadGlobalNewArrivals() {
        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)

            productRepository.getProductsPaginated(
                limit = 20,
                lastDocument = null,
                category = "All",
                sortBy = "createdAt",
                isAscending = false
            ).onSuccess { (list, _) ->
                _newArrivals.value = list.filter { it.isNewProduct() }
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadInitialProducts(category)
    }

    fun getOrCreateSupportChat(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            chatRepo.getOrCreateSupportChannel().onSuccess { channelId -> onSuccess(channelId) }
        }
    }

    fun addToCart(product: Product, quantity: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = cartRepository.addToCart(product, quantity)
            if (result.isSuccess) onResult(true, "Đã thêm vào giỏ hàng!") else onResult(false, result.exceptionOrNull()?.message ?: "Có lỗi xảy ra")
        }
    }

    fun applyFilterAndSort(sortBy: String, isAsc: Boolean, min: Long?, max: Long?) {
        _currentSortBy.value = sortBy; _currentIsAscending.value = isAsc; _currentMinPrice.value = min; _currentMaxPrice.value = max
        loadInitialProducts(_selectedCategory.value)
    }

    fun clearPriceFilter() { _currentMinPrice.value = null; _currentMaxPrice.value = null; loadInitialProducts(_selectedCategory.value) }
    fun clearSortFilter() { _currentSortBy.value = "createdAt"; _currentIsAscending.value = false; loadInitialProducts(_selectedCategory.value) }

    fun loadVouchers() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        viewModelScope.launch {
            voucherRepository.getAvailableVouchers().onSuccess { list ->
                _voucherOnHome.value = list.filter { it.isPublic }
            }
            if (uid.isNotEmpty()) {
                voucherRepository.getUserVouchers(uid).onSuccess { list ->
                    _userVoucherIds.value = list.map { it.voucherId }
                }
            }
        }
    }

    fun claimVoucher(voucher: com.example.storepromax.domain.model.Voucher) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isEmpty()) return
        viewModelScope.launch { voucherRepository.claimVoucher(uid, voucher).onSuccess { loadVouchers() } }
    }
}