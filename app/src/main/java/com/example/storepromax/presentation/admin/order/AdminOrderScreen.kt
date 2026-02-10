package com.example.storepromax.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.storepromax.domain.model.Order
import com.example.storepromax.presentation.navigation.Screen
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderScreen(
    navController: NavController,
    viewModel: AdminOrderViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("Tất cả", "Chờ xác nhận", "Lấy hàng", "Đang giao", "Hoàn thành", "Hủy")
    val statusMap = listOf("ALL", "PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED")

    val filteredOrders = remember(orders, selectedTabIndex, searchQuery) {
        orders.filter { order ->
            val statusMatch = if (selectedTabIndex == 0) true else order.status == statusMap[selectedTabIndex]
            val searchMatch = if (searchQuery.isBlank()) true else {
                order.id.contains(searchQuery, true) ||
                        order.receiverName.contains(searchQuery, true)
            }
            statusMatch && searchMatch
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Quản lý đơn hàng", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Tìm theo Mã đơn, Tên khách...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF007AFF)
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = Color(0xFFF2F4F8)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF007AFF)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = if (index == 0) orders.size else orders.count { it.status == statusMap[index] }

                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    title,
                                    color = if(selectedTabIndex == index) Color(0xFF007AFF) else Color.Gray,
                                    fontWeight = if(selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                                if(count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("($count)", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy đơn hàng nào", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders) { order ->
                        AdminOrderItem(
                            order = order,
                            onUpdateStatus = { viewModel.updateStatus(order.id, order.status) },
                            onCancel = { viewModel.cancelOrder(order.id) },
                            onClick = {
                                navController.navigate(Screen.AdminOrderDetail.createRoute(order.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrderItem(
    order: Order,
    onUpdateStatus: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val dateString = try { dateFormat.format(order.createdAt) } catch (e: Exception) { "" }

    val (statusLabel, statusColor, actionBtnText, actionBtnColor) = when(order.status) {
        "PENDING" -> Quadruple("Chờ duyệt", Color(0xFFE65100), "Xác nhận đơn", Color(0xFF007AFF))
        "CONFIRMED" -> Quadruple("Chờ lấy hàng", Color(0xFF1565C0), "Bắt đầu giao", Color(0xFF0097A7))
        "SHIPPING" -> Quadruple("Đang giao", Color(0xFF00838F), "Đã giao xong", Color(0xFF2E7D32))
        "DELIVERED" -> Quadruple("Hoàn thành", Color(0xFF2E7D32), null, Color.Transparent)
        "CANCELLED" -> Quadruple("Đã hủy", Color(0xFFC62828), null, Color.Transparent)
        else -> Quadruple("", Color.Gray, null, Color.Transparent)
    }

    val isPaid = order.paymentStatus == "PAID"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: User Name + ID + Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(order.receiverName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(dateString, fontSize = 12.sp, color = Color.Gray)
            }
            Text("ID: ${order.id.uppercase()}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 20.dp))

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))

            // Body: Product info
            Row(verticalAlignment = Alignment.Top) {
                // Hiển thị số lượng món hàng
                Column(modifier = Modifier.weight(1f)) {
                    order.items.take(2).forEach { item ->
                        Text("• ${item.product.name} (x${item.quantity})", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if(order.items.size > 2) Text("... và ${order.items.size - 2} sản phẩm khác", fontSize = 12.sp, color = Color.Gray)
                }
                // Tổng tiền
                Column(horizontalAlignment = Alignment.End) {
                    Text("₫${formatter.format(order.totalPrice)}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))

                    // Payment Status Badge
                    Surface(
                        color = if(isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            if(isPaid) "Đã thanh toán" else "COD - Chưa thu",
                            fontSize = 10.sp,
                            color = if(isPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))

            // Footer: Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(statusLabel, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                Row {
                    if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text("Hủy", color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (actionBtnText != null) {
                        Button(
                            onClick = onUpdateStatus,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = actionBtnColor)
                        ) {
                            Text(actionBtnText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)