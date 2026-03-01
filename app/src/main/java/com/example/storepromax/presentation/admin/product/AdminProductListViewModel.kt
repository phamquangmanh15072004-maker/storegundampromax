package com.example.storepromax.presentation.admin.product

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.ProductRepository
import com.opencsv.CSVReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class AdminProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())

    var searchQuery = MutableStateFlow("")
    var selectedCategory = MutableStateFlow("Tất cả")

    val filteredProducts = combine(_allProducts, searchQuery, selectedCategory) { products, query, category ->
        products.filter { product ->
            val matchesSearch = product.name.contains(query, ignoreCase = true)
            val matchesCategory = if (category == "Tất cả") true else product.category == category
            matchesSearch && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            productRepository.getProducts().onSuccess { list ->
                _allProducts.value = list.sortedByDescending { it.createdAt }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            productRepository.deleteProduct(productId).onSuccess {
                loadProducts()
            }
        }
    }

    fun onSearchTextChange(text: String) {
        searchQuery.value = text
    }

    fun onCategoryChange(category: String) {
        selectedCategory.value = category
    }
    fun importProductsFromCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val csvReader = CSVReader(InputStreamReader(inputStream))
                val rows = csvReader.readAll()
                var successCount = 0
                for (i in 1 until rows.size) {
                    val tokens = rows[i]

                    val name = tokens[0].trim()
                    val category = tokens[1].trim()
                    val price = tokens[2].trim().toLongOrNull() ?: 0L
                    val originalPrice = tokens[3].trim().toLongOrNull() ?: 0L
                    val stock = tokens[4].trim().toIntOrNull() ?: 0
                    val description = tokens[5].trim()
                    val imagesString = tokens[6].trim()

                    val imagesList = imagesString
                        .split("|")
                        .map { it.trim() }

                    val product = Product(
                        name = name,
                        category = category,
                        price = price,
                        originalPrice = originalPrice,
                        stock = stock,
                        description = description,
                        imageUrl = imagesList.firstOrNull() ?: "",
                        images = imagesList,
                        isNew = true,
                        isActive = true
                    )

                    productRepository.addProduct(product)
                    successCount++
                }

                Toast.makeText(context, "Đã import $successCount sản phẩm!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun deleteAllProducts() {
        viewModelScope.launch {
            try {
                productRepository.deleteAllProducts()
                loadProducts()
                Toast.makeText(context, "Đã xóa sạch kho hàng!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi xóa hàng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}