package com.example.storepromax.presentation.admin.product

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
                var updateCount = 0
                for (i in 1 until rows.size) {
                    val tokens = rows[i]
                    if (tokens.size < 10) continue
                    val sku = tokens[0].trim()
                    val name = tokens[1].trim()
                    val category = tokens[2].trim()
                    val price = tokens[3].trim().toLongOrNull() ?: 0L
                    val originalPrice = tokens[4].trim().toLongOrNull() ?: 0L
                    val costPrice = tokens[5].trim().toLongOrNull() ?: 0L
                    val stock = tokens[6].trim().toIntOrNull() ?: 0
                    val weight = tokens[7].trim().toIntOrNull() ?: 0
                    val description = tokens[8].trim()
                    val imagesString = tokens[9].trim()
                    val imagesList = imagesString.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    val existingProduct = _allProducts.value.find { it.sku == sku }
                    if (existingProduct != null) {
                        val updatedProduct = existingProduct.copy(
                            stock = existingProduct.stock + stock,
                            price = price,
                            costPrice = costPrice,
                            updatedAt = System.currentTimeMillis()
                        )
                        productRepository.updateProduct(updatedProduct)
                        updateCount++
                    } else {
                        val newProduct = Product(
                            sku = sku,
                            name = name,
                            category = category,
                            price = price,
                            originalPrice = originalPrice,
                            costPrice = costPrice,
                            stock = stock,
                            weight = weight,
                            description = description,
                            imageUrl = imagesList.firstOrNull() ?: "",
                            images = imagesList,
                            isActive = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        productRepository.addProduct(newProduct)
                        successCount++
                    }
                }
                loadProducts()
                Toast.makeText(context, "Thành công: Thêm mới $successCount | Cập nhật $updateCount SP!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Lỗi Import File: ${e.message}", Toast.LENGTH_LONG).show()
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
