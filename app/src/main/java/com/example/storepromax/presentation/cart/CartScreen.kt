package com.example.storepromax.presentation.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.CartItem
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    viewModel: CartViewModel = hiltViewModel(),
    showBackBtn: Boolean = false
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()
    val bgLight = Color(0xFFF5F5F5)
    val cardBg = Color.White
    val textPrimary = Color.Black
    val textSecondary = Color.Gray
    val cyberBlue = Color(0xFF007AFF)
    val alertRed = Color(0xFFFF3B30)

    Scaffold(
        containerColor = bgLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Giỏ Hàng",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = textPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    if (showBackBtn) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgLight)
            )
        },
        bottomBar = {
            CartBottomBar(totalPrice, cyberBlue, alertRed, cardBg, textPrimary, onCheckout = {
                navController.navigate("checkout_screen")
            })
        }
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Giỏ Hàng Trống", color = textSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        primaryColor = cyberBlue,
                        secondaryColor = alertRed,
                        backgroundColor = cardBg, // 🔥 Nền thẻ trắng
                        textColor = textPrimary,  // 🔥 Chữ đen
                        onToggle = { viewModel.toggleSelection(item) },
                        onIncrease = { viewModel.increaseQuantity(item) },
                        onDecrease = { viewModel.decreaseQuantity(item) },
                        onDelete = { viewModel.removeItem(item.product.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    textColor: Color,
    onToggle: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    Surface(
        color = backgroundColor,
        shadowElevation = 2.dp, // Thêm đổ bóng nhẹ cho nổi trên nền trắng
        shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp), // Vẫn giữ nét Gundam
        border = BorderStroke(1.dp, if (item.isSelected) primaryColor else Color(0xFFE0E0E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = primaryColor,
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White
                )
            )

            AsyncImage(
                model = item.product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CutCornerShape(8.dp))
                    .background(Color.LightGray) // Placeholder màu xám
                    .border(1.dp, Color(0xFFEEEEEE), CutCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    color = textColor, // 🔥 Màu đen
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )

                Text(
                    text = "₫${formatter.format(item.product.price)}",
                    color = Color(0xFFFF5722), // Màu cam đỏ cho giá tiền (như Shopee)
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Nút giảm
                    QuantityControlBtn(
                        icon = Icons.Default.Remove,
                        isEnabled = item.quantity > 1,
                        onClick = onDecrease,
                        contentColor = textColor
                    )

                    Text(
                        text = "${item.quantity}",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontFamily = FontFamily.Monospace
                    )

                    // Nút tăng
                    QuantityControlBtn(
                        icon = Icons.Default.Add,
                        isEnabled = item.quantity < item.product.stock,
                        onClick = onIncrease,
                        contentColor = textColor
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Nút xóa
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = secondaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun QuantityControlBtn(
    icon: ImageVector,
    isEnabled: Boolean = true,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isEnabled) Color(0xFFEEEEEE) else Color(0xFFF5F5F5), // Nền nút xám nhạt
        shape = CutCornerShape(4.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier
            .size(28.dp) // To hơn xíu cho dễ bấm
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isEnabled) contentColor else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CartBottomBar(
    totalPrice: Long,
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    textColor: Color,
    onCheckout: () -> Unit
) {
    val formatter = DecimalFormat("#,###")

    Surface(
        color = backgroundColor, // 🔥 Nền trắng
        shadowElevation = 8.dp,  // Đổ bóng ngược lên trên
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Tổng thanh toán:",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "₫${formatter.format(totalPrice)}",
                        color = secondaryColor, // Màu đỏ cho tổng tiền nổi bật
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onCheckout,
                    shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor), // Màu xanh chủ đạo
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 140.dp)
                ) {
                    Text(
                        "MUA HÀNG",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}