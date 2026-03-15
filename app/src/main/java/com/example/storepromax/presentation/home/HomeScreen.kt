package com.example.storepromax.presentation.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import coil.request.ImageRequest
import com.example.storepromax.domain.model.Product
import com.example.storepromax.feature.product_detail.components.AddToCartSheet
import com.example.storepromax.presentation.admin.notification.NotificationViewModel
import com.example.storepromax.presentation.components.SupportButton
import com.example.storepromax.presentation.home.components.ProductItem
import com.example.storepromax.presentation.navigation.Screen
import com.google.android.filament.Filament.init
import kotlinx.coroutines.launch

val GunplaBlue = Color(0xFF0D47A1)
val BgColor = Color(0xFFF2F4F7)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val productList by viewModel.products.collectAsState()
    val newArrivals by viewModel.newArrivals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var productToAddToCart by remember { mutableStateOf<Product?>(null) }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 2
        }
    }
    val currentSortBy by viewModel.currentSortBy.collectAsState()
    val currentIsAscending by viewModel.currentIsAscending.collectAsState()
    val currentMinPrice by viewModel.currentMinPrice.collectAsState()
    val currentMaxPrice by viewModel.currentMaxPrice.collectAsState()
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 2 && totalItems > 0
        }
    }
    LaunchedEffect(isAtBottom) {
        println("Pagination: isAtBottom = $isAtBottom, isLastPage = ${viewModel.isLastPage}")

        if (isAtBottom && !viewModel.isLastPage && !viewModel.isPaginating) {
            viewModel.loadMoreProducts()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.loadInitialProducts()
    }
    Scaffold(
        containerColor = BgColor,
        floatingActionButton = {
            AnimatedVisibility(visible = showScrollToTop) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    },
                    containerColor = GunplaBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Lên đầu trang",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    HeaderSection(navController,unreadCount)
                    Box(modifier = Modifier.padding(16.dp)) { BannerSection() }
                }
            }
            if (isLoading && newArrivals.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        PaddingBox { SectionTitle(title = "HÀNG MỚI VỀ 🔥") }
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(3) {
                                Box(modifier = Modifier.width(160.dp)) {
                                    ShimmerProductItem()
                                }
                            }
                        }
                    }
                }
            } else if (newArrivals.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        PaddingBox { SectionTitle(title = "HÀNG MỚI VỀ 🔥") }
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = newArrivals,
                                key = { _, product -> "new_${product.id}" }
                            ) { _, product ->
                                ProductItem(
                                    product = product,
                                    modifier = Modifier.width(160.dp),
                                    onClick = {
                                        navController.navigate(
                                            Screen.Detail.createRoute(product.id)
                                        )
                                    },
                                    onAddToCart = { productToAddToCart = product }
                                )
                            }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(2) }) {
                Column {
                    CategorySection(selectedCategory) { viewModel.selectCategory(it) }
                    Spacer(modifier = Modifier.height(16.dp))
                    PaddingBox {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle(title = "GỢI Ý DÀNH CHO BẠN")
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Lọc",
                                    tint = GunplaBlue
                                )
                            }
                        }
                    }
                    ActiveFiltersRow(
                        viewModel = viewModel,
                        currentSortBy = currentSortBy,
                        currentIsAscending = currentIsAscending,
                        currentMinPrice = currentMinPrice,
                        currentMaxPrice = currentMaxPrice
                    )
                }
            }
            if (isLoading && productList.isEmpty()) {
                items(4) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)) {
                        ShimmerProductItem()
                    }
                }
            } else if (productList.isEmpty()) {
                item(span = { GridItemSpan(2) }) { EmptyStateMessage() }
            } else {
                itemsIndexed(
                    items = productList,
                    key = { index, product -> "grid_${product.id}_$index" }
                ) { index, product ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (index % 2 == 0) 16.dp else 0.dp,
                                end = if (index % 2 == 1) 16.dp else 0.dp
                            )
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
            if (isLoading && productList.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = GunplaBlue
                        )
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
        if (showFilterSheet) {
            FilterBottomSheet(
                currentSortBy = currentSortBy,
                currentIsAscending = currentIsAscending,
                currentMinPrice = currentMinPrice,
                currentMaxPrice = currentMaxPrice,
                onDismiss = { showFilterSheet = false },
                onApply = { sortBy, isAsc, minPrice, maxPrice ->
                    viewModel.applyFilterAndSort(sortBy, isAsc, minPrice, maxPrice)
                    showFilterSheet = false
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    navController: NavController,
    unreadCount: Int
) {
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
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

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { navController.navigate("notification_screen") },
                modifier = Modifier.size(48.dp)
            ) {
                if (unreadCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(text = if (unreadCount > 99) "99+" else unreadCount.toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Thông báo",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
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
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://wallpaperaccess.com/full/19921.jpg")
                    .crossfade(true)
                    .size(800, 400)
                    .build(),
                contentDescription = "Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            Text(
                text = "NEW ARRIVALS\nGUNDAM AERIAL",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun CategorySection(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = listOf("All", "3D Model", "HG", "RG", "MG", "PG", "Tools")
    Column {
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
fun SectionTitle(title: String) {
    Text(title.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GunplaBlue)
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) GunplaBlue else Color.White
    val textColor = if (isSelected) Color.White else Color.Gray
    val borderColor = if (isSelected) GunplaBlue else Color(0xFFE0E0E0)
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (text == "3D Model") {
                Icon(
                    Icons.Default.ViewInAr,
                    null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                ); Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun EmptyStateMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            null,
            tint = Color.LightGray,
            modifier = Modifier.size(60.dp)
        ); Spacer(modifier = Modifier.height(8.dp))
        Text("Không tìm thấy sản phẩm nào", color = Color.Gray)
    }
}
@Composable
fun ShimmerProductItem() {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(brush))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(16.dp)
                .padding(horizontal = 8.dp)
                .background(brush))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(16.dp)
                .padding(horizontal = 8.dp)
                .background(brush))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(20.dp)
                .padding(horizontal = 8.dp)
                .background(brush))
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentSortBy: String,
    currentIsAscending: Boolean,
    currentMinPrice: Long?,
    currentMaxPrice: Long?,
    onDismiss: () -> Unit,
    onApply: (sortBy: String, isAsc: Boolean, minPrice: Long?, maxPrice: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedSort by remember {
        mutableStateOf(if(currentSortBy == "price") if(currentIsAscending) 1 else 2 else 0)
    }
    var selectedPriceRange by remember {
        mutableStateOf(
            when {
                currentMinPrice == null && currentMaxPrice == 500000L -> 1
                currentMinPrice == 500000L && currentMaxPrice == 1500000L -> 2
                currentMinPrice == 1500000L && currentMaxPrice == 3000000L -> 3
                currentMinPrice == 3000000L && currentMaxPrice == 5000000L -> 4
                currentMinPrice == 5000000L && currentMaxPrice == null -> 5
                else -> 0
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {

            Text(
                "SẮP XẾP THEO",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GunplaBlue,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(selected = selectedSort == 0, onClick = { selectedSort = 0 }, label = { Text("Mới nhất") }) }
                item { FilterChip(selected = selectedSort == 1, onClick = { selectedSort = 1 }, label = { Text("Giá: Thấp -> Cao") }) }
                item { FilterChip(selected = selectedSort == 2, onClick = { selectedSort = 2 }, label = { Text("Giá: Cao -> Thấp") }) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "KHOẢNG GIÁ",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GunplaBlue,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(selected = selectedPriceRange == 0, onClick = { selectedPriceRange = 0 }, label = { Text("Tất cả") }) }
                item { FilterChip(selected = selectedPriceRange == 1, onClick = { selectedPriceRange = 1 }, label = { Text("Dưới 500k") }) }
                item { FilterChip(selected = selectedPriceRange == 2, onClick = { selectedPriceRange = 2 }, label = { Text("500k - 1tr5") }) }
                item { FilterChip(selected = selectedPriceRange == 3, onClick = { selectedPriceRange = 3 }, label = { Text("1tr5 - 3tr") }) }
                item { FilterChip(selected = selectedPriceRange == 4, onClick = { selectedPriceRange = 4 }, label = { Text("3tr - 5tr") }) }
                item { FilterChip(selected = selectedPriceRange == 5, onClick = { selectedPriceRange = 5 }, label = { Text("Trên 5tr") }) }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val sortBy = if (selectedSort == 0) "createdAt" else "price"
                    val isAsc = selectedSort == 1
                    val minP = when(selectedPriceRange) {
                        2 -> 500000L
                        3 -> 1500000L
                        4 -> 3000000L
                        5 -> 5000000L
                        else -> null
                    }
                    val maxP = when(selectedPriceRange) {
                        1 -> 500000L
                        2 -> 1500000L
                        3 -> 3000000L
                        4 -> 5000000L
                        else -> null
                    }

                    onApply(sortBy, isAsc, minP, maxP)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue)
            ) {
                Text("ÁP DỤNG", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
@Composable
fun ActiveFiltersRow(
    viewModel: HomeViewModel,
    currentSortBy: String,
    currentIsAscending: Boolean,
    currentMinPrice: Long?,
    currentMaxPrice: Long?,
    modifier: Modifier = Modifier
) {
    val hasSortFilter = currentSortBy == "price"
    val hasPriceFilter = currentMinPrice != null || currentMaxPrice != null

    if (hasSortFilter || hasPriceFilter) {
        LazyRow(
            modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasPriceFilter) {
                val priceLabel = when {
                    currentMinPrice == null && currentMaxPrice == 500000L -> "Dưới 500k"
                    currentMinPrice == 500000L && currentMaxPrice == 1500000L -> "500k - 1tr5"
                    currentMinPrice == 1500000L && currentMaxPrice == 3000000L -> "1tr5 - 3tr"
                    currentMinPrice == 3000000L && currentMaxPrice == 5000000L -> "3tr - 5tr"
                    currentMinPrice == 5000000L && currentMaxPrice == null -> "Trên 5tr"
                    else -> "Khoảng giá tùy chỉnh"
                }
                item {
                    AssistChip(
                        onClick = { viewModel.clearPriceFilter() },
                        label = { Text(priceLabel, fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.Close, "Xóa", Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GunplaBlue.copy(alpha = 0.1f), labelColor = GunplaBlue)
                    )
                }
            }

            if (hasSortFilter) {
                val sortLabel = if (currentIsAscending) "Giá tăng dần" else "Giá giảm dần"
                item {
                    AssistChip(
                        onClick = { viewModel.clearSortFilter() },
                        label = { Text(sortLabel, fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.Close, "Xóa", Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GunplaBlue.copy(alpha = 0.1f), labelColor = GunplaBlue)
                    )
                }
            }
        }
    }
}