package com.example.storepromax.presentation.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
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

    // 🌟 STATE QUẢN LÝ DIALOG HỦY ĐƠN CỦA USER
    var orderToCancel by remember { mutableStateOf<Order?>(null) }

    val statusCodes = listOf("ALL", "PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED")
    val tabTitles = listOf("Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đang giao", "Đã giao", "Đã hủy")

    val filteredOrders = remember(allOrders, selectedTabIndex) {
        if (selectedTabIndex == 0) allOrders else allOrders.filter { it.status == statusCodes[selectedTabIndex] }
    }

    val bgLight = Color(0xFFF5F5F5)
    val primaryColor = Color(0xFF007AFF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = bgLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = primaryColor,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = primaryColor)
                    }
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium, color = if (selectedTabIndex == index) primaryColor else Color.Gray) }
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
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredOrders) { order ->
                        OrderItem(
                            order = order,
                            onCancelClick = { orderToCancel = order }
                        )
                    }
                }
            }
        }
    }
    if (orderToCancel != null) {
        UserCancelOrderDialog(
            onDismiss = { orderToCancel = null },
            onConfirm = { reason ->
                viewModel.cancelOrder(orderToCancel!!.id, reason)
                orderToCancel = null
            }
        )
    }
}

// 🌟 DIALOG CHỌN LÝ DO HỦY CỦA USER
@Composable
fun UserCancelOrderDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val reasons = listOf(
        "Tôi muốn thay đổi địa chỉ giao hàng",
        "Tôi muốn thêm/bớt sản phẩm",
        "Tôi quên áp mã giảm giá",
        "Đổi ý, không muốn mua nữa",
        "Lý do khác"
    )
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Xác nhận hủy đơn", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                Text("Bạn chắc chắn muốn hủy đơn hàng này? Vui lòng cho chúng tôi biết lý do:", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                reasons.forEach { text ->
                    Row(
                        Modifier.fillMaxWidth().selectable(
                            selected = (text == selectedReason),
                            onClick = { selectedReason = text }
                        ).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedReason),
                            onClick = { selectedReason = text },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF))
                        )
                        Text(text = text, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (selectedReason == "Lý do khác") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        placeholder = { Text("Nhập lý do của bạn...") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Lý do khác") customReason.ifBlank { "Khách hàng đổi ý" } else selectedReason
                    onConfirm(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Đồng ý Hủy") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bỏ qua", color = Color.Gray) }
        }
    )
}

@Composable
fun OrderItem(
    order: Order,
    onCancelClick: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = try { dateFormat.format(order.createdAt) } catch (e: Exception) { "" }

    val (statusLabel, statusColor, statusBg) = when(order.status) {
        "PENDING" -> Triple("Chờ xác nhận", Color(0xFFE65100), Color(0xFFFFE0B2))
        "CONFIRMED" -> Triple("Đã xác nhận", Color(0xFF0277BD), Color(0xFFB3E5FC))
        "SHIPPING" -> Triple("Đang giao", Color(0xFF00838F), Color(0xFFB2EBF2))
        "DELIVERED" -> Triple("Hoàn thành", Color(0xFF2E7D32), Color(0xFFC8E6C9))
        "CANCELLED" -> Triple("Đã hủy", Color(0xFFC62828), Color(0xFFFFCDD2))
        else -> Triple("Không rõ", Color.Gray, Color(0xFFEEEEEE))
    }

    val isPaid = order.paymentStatus == "PAID"
    val paymentText = if (isPaid) "Đã thanh toán" else "Chưa thanh toán"
    val paymentColor = if (isPaid) Color(0xFF2E7D32) else Color(0xFFE65100)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Mã đơn: ${order.id.takeLast(8).uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dateString, fontSize = 12.sp, color = Color.Gray)
                }
                Surface(color = statusBg, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = statusLabel, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))
            order.items.take(2).forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
                    AsyncImage(model = item.product.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.product.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("x${item.quantity}", fontSize = 13.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "₫${formatter.format(item.totalPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (order.items.size > 2) {
                Text(text = "Xem thêm ${order.items.size - 2} sản phẩm khác...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            }

            // 🌟 HIỂN THỊ LÝ DO HỦY ĐƠN VỚI UI ĐẸP MẮT
            if (order.status == "CANCELLED" && !order.cancelReason.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFFFF0F0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(
                        text = "Lý do hủy: ${order.cancelReason}",
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))

            // Footer
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if(isPaid) Icons.Default.CheckCircle else Icons.Default.Info, contentDescription = null, tint = paymentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(paymentText, fontSize = 12.sp, color = paymentColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Thành tiền:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "₫${formatter.format(order.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD32F2F))
                    }
                }

                // CHỈ CHO PHÉP HỦY KHI PENDING
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