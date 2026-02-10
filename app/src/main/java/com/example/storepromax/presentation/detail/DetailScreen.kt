package com.example.storepromax.presentation.detail

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.storepromax.presentation.wishlist.WishlistViewModel // Import ViewModel Yêu thích
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel() // Inject thêm cái này để xử lý Tim
) {
    // State cơ bản
    var isBuyNowAction by remember { mutableStateOf(false) }
    var showAddToCartSheet by remember { mutableStateOf(false) }

    // Data từ DetailViewModel
    val product = viewModel.state.value
    val isLoading = viewModel.isLoading.value
    val reviews = viewModel.reviews.value
    val userRating = viewModel.userRating.intValue

    // Data từ WishlistViewModel (Lấy list ID đã thích)
    val wishlistIds by wishlistViewModel.wishlistIds.collectAsState()

    // Context & Colors
    val context = LocalContext.current
    val gundamBlue = Color(0xFF0074D9)
    val gundamRed = Color(0xFFFF4136)
    val warningYellow = Color(0xFFFFDC00)
    val darkMetal = Color(0xFF111111)

    if (isLoading || product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = gundamBlue)
        }
    } else {
        // Kiểm tra xem sản phẩm này có đang được thích không
        val isFavorite = wishlistIds.contains(product.id)

        Scaffold(
            containerColor = Color(0xFFF5F5F5),
            bottomBar = {
                BottomActionButtons(
                    product = product,
                    primaryColor = gundamBlue,
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
                // SLIDER ẢNH (Có nút Tim ở đây)
                ProductImageSlider(
                    product = product,
                    navController = navController,
                    isFavorite = isFavorite,
                    onFavoriteClick = { wishlistViewModel.toggleFavorite(product.id) }
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    // TAG & RATING
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TechTag(text = product.category, color = gundamBlue)
                        if (product.isNew) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TechTag(text = "NEW ARRIVAL", color = warningYellow, textColor = Color.Black)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Star, contentDescription = null, tint = warningYellow, modifier = Modifier.size(20.dp))
                        Text(text = "${product.rating} (${product.sold} sold)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TÊN SẢN PHẨM
                    Text(
                        text = product.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = darkMetal,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // GIÁ TIỀN
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₫${product.price}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = gundamRed
                        )
                        if (product.originalPrice > product.price) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "₫${product.originalPrice}",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // NÚT 3D
                    if (!product.model3DUrl.isNullOrEmpty()) {
                        Button3D(onClick = {
                            val encodedUrl = URLEncoder.encode(product.model3DUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("model_3d/$encodedUrl")
                        })
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // THÔNG SỐ KỸ THUẬT
                    TechSpecsSection(product)

                    Spacer(modifier = Modifier.height(24.dp))

                    // MÔ TẢ (Có thu gọn)
                    ExpandableDescription(product.description)

                    Spacer(modifier = Modifier.height(32.dp))
                    Divider(color = Color.LightGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // BÌNH LUẬN
                    Text(
                        text = "ĐÁNH GIÁ TỪ CỘNG ĐỒNG",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    ReviewSection(
                        reviews = reviews,
                        currentUserRating = userRating,
                        onCommentSubmit = { content, parentId, rating ->
                            viewModel.submitComment(content, parentId, rating)
                        },
                        onDeleteComment = { reviewId ->
                            viewModel.deleteComment(reviewId)
                            Toast.makeText(context, "Xóa Bình Luận thành công!!!", Toast.LENGTH_SHORT).show()
                        },
                        onEditComment = { reviewId, newContent ->
                            viewModel.editComment(reviewId, newContent)
                            Toast.makeText(context, "Sửa Bình Luận thành công!!!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // BOTTOM SHEET MUA HÀNG
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
                            viewModel.addToCart(quantity)
                            showAddToCartSheet = false
                            Toast.makeText(context, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

// --- CÁC COMPONENT CON ĐÃ TỐI ƯU ---

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

        // Gradient đen mờ để nút Back/Share/Heart luôn nổi bật
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp), // Né tai thỏ
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleIconButton(icon = Icons.Default.ArrowBack) { navController.popBackStack() }

            Row {
                CircleIconButton(icon = Icons.Default.Share) { /* Share logic */ }
                Spacer(modifier = Modifier.width(12.dp))

                // NÚT TIM YÊU THÍCH NẰM Ở ĐÂY
                CircleIconButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    iconColor = if (isFavorite) Color(0xFFFF4136) else Color.White,
                    onClick = onFavoriteClick
                )
            }
        }

        // Số trang ảnh
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.7f), shape = CutCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ExpandableDescription(description: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Text(text = "Mô Tả", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = Color.DarkGray,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(
            onClick = { isExpanded = !isExpanded },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isExpanded) "Thu gọn" else "Xem thêm",
                color = Color(0xFF0074D9),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TechTag(text: String, color: Color, textColor: Color = Color.White) {
    Surface(
        color = color,
        shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun Button3D(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = CutCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
        elevation = ButtonDefaults.buttonElevation(8.dp)
    ) {
        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF00FFCC))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Xem Mô Hình 3D",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
fun TechSpecsSection(product: Product) {
    val stockStatus = when {
        product.stock > 10 -> "${product.stock}"
        product.stock > 0 -> "Chỉ còn ${product.stock} !"
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
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text("Thông Tin Chi Tiết", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

        TechRow("Thể Loại", product.category)
        TechRow("Tình Trạng", if (product.isNew) "New Sealed" else "Standard")
        TechRow("Ngày Sản Xuất", "2024 (Estimated)")
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tồn Kho", fontSize = 14.sp, color = Color.Gray)
            Text(text = stockStatus, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = stockColor)
        }
    }
}

@Composable
fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
        color = Color.Black.copy(alpha = 0.4f), // Nền đen mờ
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
    val isAvailable = product.stock > 0
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
            // Nút Giỏ hàng (Màu Gundam)
            OutlinedButton(
                onClick = onAddToCart,
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = primaryColor)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nút MUA NGAY (Màu Gundam)
            Button(
                onClick = onBuyNow,
                enabled = isAvailable,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    text = if (isAvailable) "MUA NGAY" else "HẾT HÀNG",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}