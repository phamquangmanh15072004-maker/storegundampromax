package com.example.storepromax.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.PostRepository
import com.example.storepromax.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentlyViewedViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _recentProducts = MutableStateFlow<List<Product>>(emptyList())
    val recentProducts = _recentProducts.asStateFlow()

    init {
        loadHistory()
    }
    fun loadHistory() {
        viewModelScope.launch {
            productRepository.getViewHistory().collect { historyList ->
                _recentProducts.value = historyList
            }
        }
    }
    fun clearHistory() {
        viewModelScope.launch {
            productRepository.clearViewHistory()
            _recentProducts.value = emptyList()
        }
    }
}