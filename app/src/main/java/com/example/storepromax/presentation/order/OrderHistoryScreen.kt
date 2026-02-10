package com.example.storepromax.presentation.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Order
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    navController: NavController,
    viewModel: OrderViewModel = hiltViewModel(),
    initialTabIndex: Int = 0
) {
    val allOrders by viewModel.orders.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex) }

    // Danh sách Tab (Logic Code vẫn dùng tiếng Anh để khớp DB, nhưng hiển thị Tiếng Việt)
    val statusCodes = listOf("ALL", "PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED")
    val tabTitles = listOf("Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đang giao", "Đã giao", "Đã hủy")

    val filteredOrders = remember(allOrders, selectedTabIndex) {
        if (selectedTabIndex == 0) allOrders
        else allOrders.filter { it.status == statusCodes[selectedTabIndex] }
    }

    // --- MÀU SẮC LIGHT THEME ---
    val bgLight = Color(0xFFF5F5F5) // Nền xám nhạt
    val primaryColor = Color(0xFF007AFF) // Xanh dương thương hiệu
    val white = Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = white)
            )
        },
        containerColor = bgLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // --- TAB BAR ---
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = white,
                contentColor = primaryColor,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = primaryColor
                        )
                    }
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == index) primaryColor else Color.Gray
                            )
                        }
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Chưa có đơn hàng nào", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredOrders) { order ->
                        OrderItem(order = order, onCancelClick = { viewModel.cancelOrder(order.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItem(
    order: Order,
    onCancelClick: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = try { dateFormat.format(order.createdAt) } catch (e: Exception) { "" }

    // --- CẤU HÌNH MÀU SẮC & TÊN TRẠNG THÁI (TIẾNG VIỆT) ---
    // Sử dụng cặp màu Nền nhạt / Chữ đậm cho Chip trạng thái
    val (statusLabel, statusColor, statusBg) = when(order.status) {
        "PENDING" -> Triple("Chờ xác nhận", Color(0xFFE65100), Color(0xFFFFE0B2))     // Cam đậm / Cam nhạt
        "CONFIRMED" -> Triple("Đã xác nhận", Color(0xFF0277BD), Color(0xFFB3E5FC))    // Xanh dương
        "SHIPPING" -> Triple("Đang giao", Color(0xFF00838F), Color(0xFFB2EBF2))       // Xanh trời
        "DELIVERED" -> Triple("Hoàn thành", Color(0xFF2E7D32), Color(0xFFC8E6C9))     // Xanh lá
        "CANCELLED" -> Triple("Đã hủy", Color(0xFFC62828), Color(0xFFFFCDD2))         // Đỏ
        else -> Triple("Không rõ", Color.Gray, Color(0xFFEEEEEE))
    }

    val isPaid = order.paymentStatus == "PAID"
    val paymentText = if (isPaid) "Đã thanh toán" else "Chưa thanh toán"
    val paymentColor = if (isPaid) Color(0xFF2E7D32) else Color(0xFFE65100)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp), // Tăng độ đổ bóng nhẹ
        shape = RoundedCornerShape(12.dp), // Bo góc mềm mại hơn
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // Khoảng cách giữa các thẻ đơn hàng
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // =Q= HEADER: Mã đơn, Ngày & Trạng thái ===
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Mã đơn: ${order.id.takeLast(8).uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dateString, fontSize = 12.sp, color = Color.Gray)
                }

                // Chip trạng thái
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(16.dp), // Bo tròn kiểu chip
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))

            order.items.take(2).forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()
                ) {
                    AsyncImage(
                        model = item.product.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5)) // Màu nền placeholder khi load ảnh
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Thông tin tên và số lượng
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.product.name,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("x${item.quantity}", fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Giá tiền từng món
                    Text(
                        text = "₫${formatter.format(item.totalPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Nếu có nhiều hơn 2 sản phẩm, hiện dòng thông báo
            if (order.items.size > 2) {
                Text(
                    text = "Xem thêm ${order.items.size - 2} sản phẩm khác...",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))

            // === FOOTER: Tổng tiền & Nút hành động ===
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hàng tổng tiền
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Trạng thái thanh toán
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if(isPaid) androidx.compose.material.icons.Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Default.Info,
                            contentDescription = null,
                            tint = paymentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(paymentText, fontSize = 12.sp, color = paymentColor)
                    }

                    // Tổng thành tiền
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Thành tiền:", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "₫${formatter.format(order.totalPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFD32F2F) // Màu đỏ nổi bật cho tổng tiền
                        )
                    }
                }

                // Nút Hủy đơn (Chỉ hiện khi PENDING)
                if (order.status == "PENDING") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Hủy Đơn Hàng", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}