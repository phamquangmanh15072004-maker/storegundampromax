package com.example.storepromax.presentation.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Product
import com.example.storepromax.feature.product_detail.components.AddToCartSheet
import com.example.storepromax.presentation.components.SupportButton
import com.example.storepromax.presentation.home.components.ProductItem
import com.example.storepromax.presentation.navigation.Screen

val GunplaBlue = Color(0xFF0D47A1)
val BgColor = Color(0xFFF8F9FA)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val productList by viewModel.products.collectAsState()
    val newArrivals by viewModel.newArrivals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var productToAddToCart by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        containerColor = BgColor,
        floatingActionButton = {
            SupportButton(onClick = { viewModel.contactSupport { id -> navController.navigate("chat_detail/$id") } })
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 100.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    HeaderSection(navController)

                    Box(modifier = Modifier.padding(16.dp)) {
                        BannerSection()
                    }

                    CategorySection(selectedCategory) { viewModel.selectCategory(it) }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            if (newArrivals.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        PaddingBox {
                            SectionTitle(title = "Sản phẩm mới 🔥", onSeeAll = {})
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(newArrivals) { product ->
                                ProductItem(
                                    product = product,
                                    modifier = Modifier.width(160.dp),
                                    onClick = { navController.navigate(Screen.Detail.createRoute(product.id)) },
                                    onAddToCart = { productToAddToCart = product }
                                )
                            }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                PaddingBox {
                    SectionTitle(title = "Gợi ý hôm nay", onSeeAll = {})
                }
            }
            if (isLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GunplaBlue)
                    }
                }
            } else {
                if (productList.isEmpty()) {
                    item(span = { GridItemSpan(2) }) { EmptyStateMessage() }
                } else {
                    items(productList.size) { index ->
                        val product = productList[index]
                        val paddingStart = if (index % 2 == 0) 16.dp else 6.dp
                        val paddingEnd = if (index % 2 == 0) 6.dp else 16.dp

                        Box(
                            modifier = Modifier
                                .padding(start = paddingStart, end = paddingEnd, bottom = 12.dp)
                        ) {
                            ProductItem(
                                product = product,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Screen.Detail.createRoute(product.id)) },
                                onAddToCart = { productToAddToCart = product }
                            )
                        }
                    }
                }
            }
        }
        if (productToAddToCart != null) {
            AddToCartSheet(
                product = productToAddToCart!!,
                onDismiss = { productToAddToCart = null },
                onConfirm = { quantity ->
                    viewModel.addToCart(productToAddToCart!!, quantity) {
                        Toast.makeText(context, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                    }
                    productToAddToCart = null
                }
            )
        }
    }
}

@Composable
fun PaddingBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        content()
    }
}

@Composable
fun HeaderSection(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GunplaBlue, Color(0xFF1976D2))
                )
            )
            .statusBarsPadding()
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.White, shape = RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp))
                .clickable { navController.navigate("search") }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = GunplaBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Tìm kiếm Gundam, Tool...", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun BannerSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            AsyncImage(
                model = "https://wallpaperaccess.com/full/19921.jpg",
                contentDescription = "Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            )
            Text(
                text = "NEW ARRIVALS\nGUNDAM AERIAL",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            )
        }
    }
}

@Composable
fun CategorySection(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("All", "3D Model", "HG", "RG", "MG", "PG", "Tools")
    Column {
        PaddingBox {
            Text("DANH MỤC", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { cat ->
                CategoryChip(
                    text = cat,
                    isSelected = cat == selectedCategory,
                    onClick = { onCategorySelected(cat) }
                )
            }
        }
    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) GunplaBlue else Color.White
    val textColor = if (isSelected) Color.White else Color.Gray
    val borderColor = if (isSelected) GunplaBlue else Color(0xFFE0E0E0)
    Surface(modifier = Modifier.clickable { onClick() }.height(40.dp), shape = RoundedCornerShape(20.dp), color = backgroundColor, border = androidx.compose.foundation.BorderStroke(1.dp, borderColor), shadowElevation = if(isSelected) 4.dp else 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            if (text == "3D Model") { Icon(Icons.Default.ViewInAr, null, tint = textColor, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)) }
            Text(text, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
        }
    }
}

@Composable
fun SectionTitle(title: String, onSeeAll: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GunplaBlue)
        Text("Xem tất cả", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.clickable { onSeeAll() })
    }
}

@Composable
fun EmptyStateMessage() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Search, null, tint = Color.LightGray, modifier = Modifier.size(60.dp)); Spacer(modifier = Modifier.height(8.dp))
        Text("Không tìm thấy sản phẩm nào", color = Color.Gray)
    }
}