package com.example.storepromax.presentation.cart

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {

    val cartItems: StateFlow<List<CartItem>> = cartRepository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalPrice: StateFlow<Long> = cartItems.map { list ->
        list.filter { it.isSelected }.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    fun toggleSelection(item: CartItem) {
        viewModelScope.launch {
            cartRepository.updateSelection(item.product.id, !item.isSelected)
        }
    }
    fun increaseQuantity(item: CartItem) {
        if (item.quantity < item.product.stock) {
            viewModelScope.launch {
                cartRepository.updateQuantity(item.product.id, item.quantity + 1)
            }
        } else {

        }
    }
    fun decreaseQuantity(item: CartItem) {
        viewModelScope.launch {
            cartRepository.updateQuantity(item.product.id, item.quantity - 1)
        }
    }
    fun removeItem(productId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(productId)
        }
    }
}