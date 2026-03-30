package com.example.storepromax.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val chatRepo: ChatRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private var _allProducts = mutableListOf<Product>()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _newArrivals = MutableStateFlow<List<Product>>(emptyList())
    val newArrivals = _newArrivals.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private var lastDocument: DocumentSnapshot? = null
    var isLastPage = false
    var isPaginating = false
    private val pageSize = 4L

    private val _currentSortBy = MutableStateFlow("createdAt")
    val currentSortBy = _currentSortBy.asStateFlow()

    private val _currentIsAscending = MutableStateFlow(false)
    val currentIsAscending = _currentIsAscending.asStateFlow()

    private val _currentMinPrice = MutableStateFlow<Long?>(null)
    val currentMinPrice = _currentMinPrice.asStateFlow()

    private val _currentMaxPrice = MutableStateFlow<Long?>(null)
    val currentMaxPrice = _currentMaxPrice.asStateFlow()

    init {
        loadGlobalNewArrivals()
        loadInitialProducts()
    }

    fun loadInitialProducts(category: String = _selectedCategory.value) {
        viewModelScope.launch {
            _isLoading.value = true
            lastDocument = null
            isLastPage = false
            isPaginating = false
            _allProducts = mutableListOf()

            productRepository.getProductsPaginated(
                pageSize, null, category, sortBy = _currentSortBy.value,
                isAscending = _currentIsAscending.value,
                minPrice = currentMinPrice.value,
                maxPrice = currentMaxPrice.value
            )
                .onSuccess { (list, lastDoc) ->
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

    fun loadMoreProducts() {
        if (isLastPage || isPaginating || _isLoading.value) return

        viewModelScope.launch {
            isPaginating = true

            productRepository.getProductsPaginated(
                pageSize, lastDocument, _selectedCategory.value, sortBy = currentSortBy.value,
                isAscending = currentIsAscending.value,
                minPrice = currentMinPrice.value,
                maxPrice = currentMaxPrice.value
            )
                .onSuccess { (list, lastDoc) ->
                    if (list.isNotEmpty()) {
                        val uniqueNewItems = list.filter { newItem ->
                            _allProducts.none { existingItem -> existingItem.id == newItem.id }
                        }
                        _allProducts.addAll(uniqueNewItems)
                        lastDocument = lastDoc
                        _products.value = _allProducts.toList()
                    }
                    if (list.size < pageSize) {
                        isLastPage = true
                    }
                    isPaginating = false
                }.onFailure {
                    isPaginating = false
                }
        }
    }

    private fun loadGlobalNewArrivals() {
        viewModelScope.launch {
            productRepository.getProductsPaginated(20, null, "All").onSuccess { (list, _) ->
                _newArrivals.value = list.filter { it.isNew }
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadInitialProducts(category)
    }

    fun getOrCreateSupportChat(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = chatRepo.getOrCreateSupportChannel()
            result.onSuccess { channelId ->
                onSuccess(channelId)
            }
        }
    }

    fun addToCart(product: Product, quantity: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = cartRepository.addToCart(product, quantity)
            if (result.isSuccess) {
                onResult(true, "Đã thêm vào giỏ hàng!")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Có lỗi xảy ra"
                onResult(false, errorMsg)
            }
        }
    }

    fun applyFilterAndSort(sortBy: String, isAsc: Boolean, min: Long?, max: Long?) {
        _currentSortBy.value = sortBy
        _currentIsAscending.value = isAsc
        _currentMinPrice.value = min
        _currentMaxPrice.value = max
        loadInitialProducts(_selectedCategory.value)
    }

    fun clearPriceFilter() {
        _currentMinPrice.value = null
        _currentMaxPrice.value = null
        loadInitialProducts(_selectedCategory.value)
    }

    fun clearSortFilter() {
        _currentSortBy.value = "createdAt"
        _currentIsAscending.value = false
        loadInitialProducts(_selectedCategory.value)
    }
}