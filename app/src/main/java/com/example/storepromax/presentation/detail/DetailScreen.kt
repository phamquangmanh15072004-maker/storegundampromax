package com.example.storepromax.presentation.detail

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Product
import com.example.storepromax.feature.product_detail.components.AddToCartSheet
import com.example.storepromax.feature.product_detail.components.ReviewSection
import com.example.storepromax.presentation.home.components.ProductItem
import com.example.storepromax.presentation.navigation.Screen
import com.example.storepromax.presentation.wishlist.WishlistViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat

private val CurrencyFormatter = DecimalFormat("#,###")

val GunplaBlue = Color(0xFF0D47A1)
val GunplaRed = Color(0xFFFF4136)
val WarningYellow = Color(0xFFFFDC00)
val DarkMetal = Color(0xFF111111)
val BgColor = Color(0xFFF2F4F7)

@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel()
) {
    var isBuyNowAction by remember { mutableStateOf(false) }
    var showAddToCartSheet by remember { mutableStateOf(false) }
    var relatedProductToAddToCart by remember { mutableStateOf<Product?>(null) }
    val product = viewModel.state.value
    val isLoading = viewModel.isLoading.value
    val reviews = viewModel.reviews.value

    val wishlistIds by wishlistViewModel.wishlistIds.collectAsState()
    val isFavorite = product != null && wishlistIds.contains(product.id)
    val relatedProducts by viewModel.relatedProducts
    val context = LocalContext.current

    val averageRating by viewModel.averageRating
    val totalRatings by viewModel.totalRatingsCount

    if (isLoading || product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GunplaBlue)
        }
    } else {
        Scaffold(
            containerColor = BgColor,
            bottomBar = {
                BottomActionButtons(
                    product = product,
                    primaryColor = GunplaBlue,
                    onAddToCart = {
                        isBuyNowAction = false
                        showAddToCartSheet = true
                    },
                    onBuyNow = {
                        isBuyNowAction = true
                        showAddToCartSheet = true
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                ProductImageSlider(
                    product = product,
                    navController = navController,
                    isFavorite = isFavorite,
                    onFavoriteClick = { wishlistViewModel.toggleFavorite(product.id) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TechTag(text = product.category, color = GunplaBlue)
                        if (product.isNewProduct()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TechTag(text = "NEW", color = WarningYellow, textColor = Color.Black)
                        }
                        if (product.isHotProduct()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TechTag(text = "HOT 🔥", color = GunplaRed, textColor = Color.White)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Star, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(20.dp))
                        Text(text = String.format("%.1f", averageRating), fontWeight = FontWeight.Bold)
                        Text(text = " | Đã bán ${product.sold}", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = product.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkMetal,
                        lineHeight = 28.sp
                    )

                    if (product.sku.isNotEmpty()) {
                        Text(text = "SKU: ${product.sku}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (product.stock in 1..5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sắp cháy hàng! Chỉ còn đúng ${product.stock} hộp.", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!product.isActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = GunplaRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sản phẩm này đã ngừng kinh doanh.", color = GunplaRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₫${CurrencyFormatter.format(product.price)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (product.isActive) GunplaRed else Color.Gray
                        )
                        if (product.originalPrice > product.price) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "₫${CurrencyFormatter.format(product.originalPrice)}",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val percent = product.getDiscountPercentage()
                            if (percent > 0) {
                                Surface(color = GunplaRed.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Text("-$percent%", color = GunplaRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
                    if (!product.model3DUrl.isNullOrEmpty()) {
                        Button3D(onClick = {
                            val encodedUrl = URLEncoder.encode(product.model3DUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("model_3d/$encodedUrl")
                        })
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    TechSpecsSection(product)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
                    ExpandableDescription(product.description)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
                    Text(
                        text = "ĐÁNH GIÁ SẢN PHẨM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ReviewSection(
                        averageRating = averageRating,
                        totalRatings = totalRatings,
                        reviews = reviews
                    )
                }

                if (relatedProducts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "CÓ THỂ BẠN CŨNG THÍCH",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(relatedProducts) { relatedProd ->
                                ProductItem(
                                    product = relatedProd,
                                    modifier = Modifier.width(160.dp),
                                    onClick = { navController.navigate(Screen.Detail.createRoute(relatedProd.id)) },
                                    onAddToCart = { _ ->
                                        relatedProductToAddToCart = relatedProd
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            if (showAddToCartSheet) {
                AddToCartSheet(
                    product = product,
                    onDismiss = { showAddToCartSheet = false },
                    confirmButtonText = if (isBuyNowAction) "ĐẾN THANH TOÁN" else "THÊM VÀO GIỎ",
                    onConfirm = { quantity ->
                        if (isBuyNowAction) {
                            showAddToCartSheet = false
                            navController.navigate("checkout_screen?productId=${product.id}&quantity=$quantity")
                        } else {
                            viewModel.addToCart(quantity) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            showAddToCartSheet = false
                        }
                    }
                )
            }
            if (relatedProductToAddToCart != null) {
                AddToCartSheet(
                    product = relatedProductToAddToCart!!,
                    onDismiss = { relatedProductToAddToCart = null },
                    confirmButtonText = "THÊM VÀO GIỎ",
                    onConfirm = { quantity ->
                        viewModel.addRelatedToCart(relatedProductToAddToCart!!, quantity) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        relatedProductToAddToCart = null
                    }
                )
            }
        }
    }
}

@Composable
fun ExpandableDescription(description: String) {
    var isExpanded by remember { mutableStateOf(false) }
    var hasVisualOverflow by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Text(text = "Chi Tiết Sản Phẩm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = Color.DarkGray,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.hasVisualOverflow) {
                    hasVisualOverflow = true
                }
            }
        )
        if (hasVisualOverflow || isExpanded) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (isExpanded) "Thu gọn" else "Xem thêm",
                    color = GunplaBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductImageSlider(
    product: Product,
    navController: NavController,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    val images = if (product.images.isNotEmpty()) product.images else listOf(product.imageUrl)
    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(modifier = Modifier.height(350.dp).fillMaxWidth()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(Color.White)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleIconButton(icon = Icons.Default.ArrowBack) { navController.popBackStack() }

            Row {
                CircleIconButton(icon = Icons.Default.Share) { /* Share logic */ }
                Spacer(modifier = Modifier.width(12.dp))
                CircleIconButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    iconColor = if (isFavorite) Color(0xFFFF4136) else Color.White,
                    onClick = onFavoriteClick
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TechTag(text: String, color: Color, textColor: Color = Color.White) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun Button3D(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
    ) {
        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF00FFCC))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Xem Mô Hình 3D",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

@Composable
fun TechSpecsSection(product: Product) {
    val stockStatus = when {
        product.stock > 10 -> "${product.stock}"
        product.stock > 0 -> "Chỉ còn ${product.stock}!"
        else -> "Hết Hàng"
    }
    val stockColor = when {
        product.stock > 10 -> Color(0xFF4CAF50)
        product.stock > 0 -> Color(0xFFFF9800)
        else -> Color.Red
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text("Thông Số Kỹ Thuật", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

        TechRow("Thể Loại", product.category)
        TechRow("Tình Trạng", if (product.isNewProduct()) "Hàng Mới" else "Tiêu chuẩn")
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tồn Kho", fontSize = 14.sp, color = Color.Gray)
            Text(text = stockStatus, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = stockColor)
        }
    }
}

@Composable
fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.4f),
        modifier = Modifier.size(40.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor)
        }
    }
}

@Composable
fun BottomActionButtons(
    product: Product,
    primaryColor: Color,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit
) {
    val isAvailable = product.stock > 0 && product.isActive

    val buttonText = when {
        !product.isActive -> "NGỪNG KINH DOANH"
        product.stock <= 0 -> "HẾT HÀNG"
        else -> "MUA NGAY"
    }

    Surface(
        shadowElevation = 16.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onAddToCart,
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor),
                contentPadding = PaddingValues(0.dp),
                enabled = isAvailable
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = if (isAvailable) primaryColor else Color.Gray)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onBuyNow,
                enabled = isAvailable,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    text = buttonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}