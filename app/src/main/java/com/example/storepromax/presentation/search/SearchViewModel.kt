package com.example.storepromax.presentation.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.ProductRepository
import com.example.storepromax.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "search_prefs")

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    @ApplicationContext private val context: Context // Inject Context
) : ViewModel() {
    private val HISTORY_KEY = stringPreferencesKey("search_history")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Product>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = _searchHistory.asStateFlow()

    init {
        observeSearchHistory()
        observeSearchQuery()
        viewModelScope.launch {
            productRepository.syncLowercaseNames()
        }
    }

    private fun observeSearchHistory() {
        viewModelScope.launch {
            context.dataStore.data
                .map { preferences ->
                    preferences[HISTORY_KEY] ?: ""
                }
                .collect { historyStr ->
                    if (historyStr.isNotEmpty()) {
                        _searchHistory.value = historyStr.split("|||")
                    } else {
                        _searchHistory.value = emptyList()
                    }
                }
        }
    }

    fun saveSearchQuery(query: String) {
        if (query.isBlank()) return
        val currentList = _searchHistory.value.toMutableList()
        currentList.remove(query)
        currentList.add(0, query)
        val trimmedList = currentList.take(10)
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[HISTORY_KEY] = trimmedList.joinToString("|||")
            }
        }
    }

    fun removeSearchHistoryItem(query: String) {
        val currentList = _searchHistory.value.toMutableList()
        currentList.remove(query)

        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[HISTORY_KEY] = currentList.joinToString("|||")
            }
        }
    }

    fun clearAllSearchHistory() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences.remove(HISTORY_KEY)
            }
        }
    }
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                    } else {
                        val normalizedQuery = query.trim()
                        if (normalizedQuery.isNotEmpty()) {
                            performSearch(normalizedQuery)
                        }
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val normalizedQuery = query.trim().lowercase()

            productRepository.searchProducts(normalizedQuery)
                .onSuccess {
                    _searchResults.value = it
                    _isLoading.value = false
                    if (it.isNotEmpty()) {
                        saveSearchQuery(query.trim())
                    }
                }
                .onFailure {
                    _isLoading.value = false
                    _searchResults.value = emptyList()
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