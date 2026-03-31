package com.example.storepromax.presentation.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Product
import com.example.storepromax.feature.product_detail.components.AddToCartSheet
import com.example.storepromax.presentation.admin.notification.NotificationViewModel
import com.example.storepromax.presentation.home.components.ProductItem
import com.example.storepromax.presentation.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.example.storepromax.feature.product_detail.components.VoucherHomeSection

val GunplaBlue = Color(0xFF0D47A1)
val BgColor = Color(0xFFF2F4F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val productList by viewModel.products.collectAsState()
    val newArrivals by viewModel.newArrivals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var productToAddToCart by remember { mutableStateOf<Product?>(null) }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val currentSortBy by viewModel.currentSortBy.collectAsState()
    val currentIsAscending by viewModel.currentIsAscending.collectAsState()
    val currentMinPrice by viewModel.currentMinPrice.collectAsState()
    val currentMaxPrice by viewModel.currentMaxPrice.collectAsState()

    var flyingImageUrl by remember { mutableStateOf<String?>(null) }
    var flyingStartOffset by remember { mutableStateOf(Offset.Zero) }
    val voucherOnHome by viewModel.voucherOnHome.collectAsState()
    val userVoucherIds by viewModel.userVoucherIds.collectAsState()
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 2 && totalItems > 0
        }
    }

    LaunchedEffect(isAtBottom) {
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedVisibility(visible = gridState.firstVisibleItemIndex > 2) {
                    FloatingActionButton(
                        onClick = { coroutineScope.launch { gridState.animateScrollToItem(0) } },
                        containerColor = Color.White, contentColor = GunplaBlue, shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Top") }
                }

                FloatingActionButton(
                    onClick = { showSupportSheet = true },
                    containerColor = GunplaBlue, contentColor = Color.White, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Default.SupportAgent, contentDescription = "Support", modifier = Modifier.size(28.dp)) }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    HeaderSection(navController, unreadCount)
                    Box(modifier = Modifier.padding(16.dp)) { BannerSection() }

                    if (voucherOnHome.isNotEmpty()) {
                        VoucherHomeSection(
                            vouchers = voucherOnHome,
                            userVoucherIds = userVoucherIds,
                            onClaim = { voucher -> viewModel.claimVoucher(voucher) }
                        )
                    }
                }
            }
            if (newArrivals.isNotEmpty() || (isLoading && newArrivals.isEmpty())) {
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        PaddingBox { SectionTitle(title = "HÀNG MỚI VỀ 🔥") }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isLoading && newArrivals.isEmpty()) {
                                items(3) { Box(modifier = Modifier.width(160.dp)) { ShimmerProductItem() } }
                            } else {
                                itemsIndexed(
                                    items = newArrivals,
                                    key = { index, product -> "new_${product.id}_$index" }
                                ) { _, product ->
                                    ProductItem(
                                        product = product, modifier = Modifier.width(160.dp),
                                        onClick = { navController.navigate(Screen.Detail.createRoute(product.id)) },
                                        onAddToCart = { offset ->
                                            productToAddToCart = product
                                            flyingStartOffset = offset
                                        }
                                    )
                                }
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle(title = "GỢI Ý DÀNH CHO BẠN")
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Default.FilterList, "Lọc", tint = GunplaBlue)
                            }
                        }
                    }
                    ActiveFiltersRow(viewModel, currentSortBy, currentIsAscending, currentMinPrice, currentMaxPrice)
                }
            }
            if (isLoading && productList.isEmpty()) {
                items(4) { ShimmerProductItem() }
            } else if (productList.isEmpty()) {
                item(span = { GridItemSpan(2) }) { EmptyStateMessage() }
            } else {
                itemsIndexed(
                    items = productList,
                    key = { index, product -> "grid_${product.id}_$index" }
                ) { index, product ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = if (index % 2 == 0) 16.dp else 0.dp, end = if (index % 2 == 1) 16.dp else 0.dp)
                    ) {
                        ProductItem(
                            product = product, modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Screen.Detail.createRoute(product.id)) },
                            onAddToCart = { offset ->
                                productToAddToCart = product
                                flyingStartOffset = offset
                            }
                        )
                    }
                }
            }
            if (isLoading && productList.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GunplaBlue)
                    }
                }
            }
        }

        if (productToAddToCart != null) {
            AddToCartSheet(
                product = productToAddToCart!!,
                onDismiss = { productToAddToCart = null },
                onConfirm = { quantity ->
                    val url = productToAddToCart?.images?.firstOrNull() ?: productToAddToCart?.imageUrl
                    viewModel.addToCart(productToAddToCart!!, quantity) { success, message ->
                        if (success) {
                            flyingImageUrl = url // Bắt đầu animation
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                    productToAddToCart = null
                }
            )
        }

        if (showFilterSheet) {
            FilterBottomSheet(currentSortBy, currentIsAscending, currentMinPrice, currentMaxPrice,
                onDismiss = { showFilterSheet = false },
                onApply = { sortBy, isAsc, min, max ->
                    viewModel.applyFilterAndSort(sortBy, isAsc, min, max)
                    showFilterSheet = false
                }
            )
        }

        if (showSupportSheet) {
            ModalBottomSheet(onDismissRequest = { showSupportSheet = false }) {
                SupportSheetContent(
                    onChatCSKH = {
                        showSupportSheet = false
                        viewModel.getOrCreateSupportChat { navController.navigate("chat_detail/$it") }
                    },
                    onChatAI = {
                        showSupportSheet = false
                        navController.navigate("ai_chat_screen")
                    }
                )
            }
        }
    }
    if (flyingImageUrl != null) {
        FlyingToCartAnimation(flyingImageUrl!!, flyingStartOffset) { flyingImageUrl = null }
    }
}


@Composable
fun SupportSheetContent(onChatCSKH: () -> Unit, onChatAI: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
        Text("Bạn cần hỗ trợ gì?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GunplaBlue)
        Spacer(modifier = Modifier.height(24.dp))
        SupportCard("Chat với CSKH", "Hỗ trợ đơn hàng (8h-22h)", Icons.Default.Person, GunplaBlue, onChatCSKH)
        Spacer(modifier = Modifier.height(16.dp))
        SupportCard("Trợ lý AI", "Tư vấn chọn Gundam", Icons.Default.SmartToy, Color(0xFFFF5252), onChatAI, isNew = true)
    }
}

@Composable
fun SupportCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, isNew: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(color, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(sub, fontSize = 12.sp, color = Color.Gray)
            }
            if (isNew) {
                Spacer(modifier = Modifier.weight(1f))
                Badge(containerColor = color) { Text("MỚI", color = Color.White) }
            }
        }
    }
}

@Composable
fun HeaderSection(navController: NavController, unreadCount: Int) {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(GunplaBlue, Color(0xFF1976D2)))).statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f).height(50.dp).background(Color.White, CircleShape).clip(CircleShape).clickable { navController.navigate("search") }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = GunplaBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Tìm kiếm Gundam...", color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { navController.navigate("notification_screen") }) {
                BadgedBox(badge = { if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) } }) {
                    Icon(if (unreadCount > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun BannerSection() {
    Card(modifier = Modifier.fillMaxWidth().height(180.dp).shadow(8.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
        Box {
            AsyncImage(model = "https://wallpaperaccess.com/full/19921.jpg", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
            Text("NEW ARRIVALS\nGUNDAM AERIAL", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
        }
    }
}

@Composable
fun FlyingToCartAnimation(imageUrl: String, startOffset: Offset, onAnimationEnd: () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val buttonSizePx = with(density) { 30.dp.toPx() }
    val imageSizePx = with(density) { 80.dp.toPx() }

    val startX = startOffset.x + (buttonSizePx / 2f) - (imageSizePx / 2f)
    val startY = startOffset.y + (buttonSizePx / 2f) - (imageSizePx / 2f)
    val endX = (screenWidth * 0.7f) - (imageSizePx / 2f)
    val endY = screenHeight - with(density) { 32.dp.toPx() } - (imageSizePx / 2f)

    val offsetX = remember { Animatable(startX) }
    val offsetY = remember { Animatable(startY) }
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { offsetX.animateTo(endX, tween(1200, easing = LinearEasing)) }
        launch {
            offsetY.animateTo(startY - 400f, tween(500, easing = FastOutSlowInEasing))
            offsetY.animateTo(endY, tween(700, easing = LinearOutSlowInEasing))
        }
        launch { scale.animateTo(0.2f, tween(1200)) }
        launch { delay(1000); alpha.animateTo(0f, tween(200)) }
        delay(1300); onAnimationEnd()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }.size(80.dp).scale(scale.value).alpha(alpha.value).clip(CircleShape).background(Color.White).border(2.dp, Color(0xFFFF424F), CircleShape), contentScale = ContentScale.Crop)
    }
}

@Composable
fun PaddingBox(content: @Composable () -> Unit) { Box(modifier = Modifier.padding(horizontal = 16.dp)) { content() } }

@Composable
fun SectionTitle(title: String) { Text(title.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GunplaBlue) }

@Composable
fun CategorySection(selected: String, onSelected: (String) -> Unit) {
    val cats = listOf("All", "3D Model", "HG", "RG", "MG", "PG", "Tools")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(cats) { cat -> CategoryChip(cat, cat == selected) { onSelected(cat) } }
    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.clickable { onClick() }.height(40.dp), shape = RoundedCornerShape(20.dp), color = if (isSelected) GunplaBlue else Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GunplaBlue else Color(0xFFE0E0E0))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            if (text == "3D Model") { Icon(Icons.Default.ViewInAr, null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)) }
            Text(text, color = if (isSelected) Color.White else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
        }
    }
}

@Composable
fun EmptyStateMessage() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Search, null, tint = Color.LightGray, modifier = Modifier.size(60.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Không tìm thấy sản phẩm nào", color = Color.Gray)
    }
}

@Composable
fun ShimmerProductItem() {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim by transition.animateFloat(0f, 1000f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing)), label = "")
    val brush = Brush.linearGradient(listOf(Color.LightGray.copy(0.6f), Color.LightGray.copy(0.2f), Color.LightGray.copy(0.6f)), start = Offset(10f, 10f), end = Offset(translateAnim, translateAnim))
    Card(modifier = Modifier.fillMaxWidth().height(250.dp).padding(4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(brush))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp).padding(horizontal = 8.dp).background(brush))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).padding(horizontal = 8.dp).background(brush))
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
    var selectedSort by remember { mutableStateOf(if (currentSortBy == "price") if (currentIsAscending) 1 else 2 else 0) }
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
            Text("SẮP XẾP THEO", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GunplaBlue, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedSort == 0, onClick = { selectedSort = 0 }, label = { Text("Mới nhất") }) }
                item { FilterChip(selected = selectedSort == 1, onClick = { selectedSort = 1 }, label = { Text("Giá: Thấp -> Cao") }) }
                item { FilterChip(selected = selectedSort == 2, onClick = { selectedSort = 2 }, label = { Text("Giá: Cao -> Thấp") }) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("KHOẢNG GIÁ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GunplaBlue, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    val minP = when (selectedPriceRange) { 2 -> 500000L; 3 -> 1500000L; 4 -> 3000000L; 5 -> 5000000L; else -> null }
                    val maxP = when (selectedPriceRange) { 1 -> 500000L; 2 -> 1500000L; 3 -> 3000000L; 4 -> 5000000L; else -> null }
                    onApply(sortBy, isAsc, minP, maxP)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue)
            ) { Text("ÁP DỤNG", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
fun ActiveFiltersRow(viewModel: HomeViewModel, currentSortBy: String, currentIsAscending: Boolean, currentMinPrice: Long?, currentMaxPrice: Long?, modifier: Modifier = Modifier) {
    val hasSortFilter = currentSortBy == "price"
    val hasPriceFilter = currentMinPrice != null || currentMaxPrice != null

    if (hasSortFilter || hasPriceFilter) {
        LazyRow(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    AssistChip(onClick = { viewModel.clearPriceFilter() }, label = { Text(priceLabel, fontSize = 12.sp) }, trailingIcon = { Icon(Icons.Default.Close, "Xóa", Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = GunplaBlue.copy(alpha = 0.1f), labelColor = GunplaBlue))
                }
            }
            if (hasSortFilter) {
                val sortLabel = if (currentIsAscending) "Giá tăng dần" else "Giá giảm dần"
                item {
                    AssistChip(onClick = { viewModel.clearSortFilter() }, label = { Text(sortLabel, fontSize = 12.sp) }, trailingIcon = { Icon(Icons.Default.Close, "Xóa", Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = GunplaBlue.copy(alpha = 0.1f), labelColor = GunplaBlue))
                }
            }
        }
    }
}