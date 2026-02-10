package com.example.storepromax.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val chatRepo: ChatRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private var _allProducts = listOf<Product>()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _newArrivals = MutableStateFlow<List<Product>>(emptyList())
    val newArrivals = _newArrivals.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            productRepository.getProducts().onSuccess { list ->
                _allProducts = list

                _newArrivals.value = list.filter { it.isNew }

                filterProducts(_selectedCategory.value)

                _isLoading.value = false
            }.onFailure {
                _isLoading.value = false
            }
        }
    }
    fun selectCategory(category: String) {
        _selectedCategory.value = category
        filterProducts(category)
    }

    private fun filterProducts(category: String) {
        val filteredList = when (category) {
            "All" -> {
                _allProducts
            }
            "3D Model" -> {
                _allProducts.filter { !it.model3DUrl.isNullOrBlank() }
            }
            else -> {

                _allProducts.filter { it.category.equals(category, ignoreCase = true) }
            }
        }
        _products.value = filteredList
    }

    fun contactSupport(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = chatRepo.getOrCreateSupportChannel()
            result.onSuccess { channelId ->
                onSuccess(channelId)
            }
        }
    }
    fun addToCart(product: Product, quantity: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            cartRepository.addToCart(product, quantity)
            onSuccess()
        }
    }
}