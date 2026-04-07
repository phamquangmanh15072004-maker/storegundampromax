package com.example.storepromax.presentation.admin.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    var orderToCancel by remember { mutableStateOf<Order?>(null) }

    val selectedOrderIds = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedOrderIds.isNotEmpty()

    val tabs = listOf("Tất cả", "Chờ xác nhận", "Lấy hàng", "Đang giao", "Hoàn thành", "Hủy", "Hoàn tiền", "Đã hoàn")
    val statusMap = listOf("ALL", OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDING, OrderStatus.REFUNDED)

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
                    title = {
                        if (isSelectionMode) {
                            Text("Đã chọn ${selectedOrderIds.size} đơn", fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                        } else {
                            Text("Quản lý đơn hàng", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = { selectedOrderIds.clear() }) { Icon(Icons.Default.Close, contentDescription = "Hủy chọn") }
                        } else {
                            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                        }
                    }
                )
                if (!isSelectionMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Tìm theo Mã đơn, Tên khách...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5), unfocusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color(0xFF007AFF)
                        ),
                        singleLine = true
                    )
                }
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
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = Color(0xFF007AFF))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = if (index == 0) orders.size else orders.count { it.status == statusMap[index] }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index; selectedOrderIds.clear() }, // Chuyển tab thì xóa chọn
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, color = if(selectedTabIndex == index) Color(0xFF007AFF) else Color.Gray, fontWeight = if(selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium)
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
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OptimizedAdminOrderItem(
                            order = order,
                            isSelected = selectedOrderIds.contains(order.id),
                            isSelectionMode = isSelectionMode,
                            onSelect = {
                                if (selectedOrderIds.contains(order.id)) selectedOrderIds.remove(order.id)
                                else selectedOrderIds.add(order.id)
                            },
                            onConfirm = {
                                val next = getNextStatusForSwipe(order.status)
                                if (next == OrderStatus.REFUNDED) viewModel.confirmRefund(order.id)
                                else if (next != null) viewModel.updateStatus(order.id, next)
                            },
                            onCancel = { orderToCancel = order },
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedOrderIds.contains(order.id)) selectedOrderIds.remove(order.id) else selectedOrderIds.add(order.id)
                                } else {
                                    navController.navigate(Screen.AdminOrderDetail.createRoute(order.id))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (orderToCancel != null) {
        CancelOrderDialog(
            isPaid = orderToCancel!!.paymentStatus == "PAID",
            onDismiss = { orderToCancel = null },
            onConfirm = { reason ->
                viewModel.cancelOrder(orderToCancel!!.id, reason)
                orderToCancel = null
            }
        )
    }
}
fun getNextStatusForSwipe(current: String): String? {
    return when(current) {
        OrderStatus.PENDING -> OrderStatus.CONFIRMED
        OrderStatus.CONFIRMED -> OrderStatus.SHIPPING
        OrderStatus.SHIPPING -> OrderStatus.DELIVERED
        OrderStatus.REFUNDING -> OrderStatus.REFUNDED
        else -> null
    }
}

@Composable
fun CancelOrderDialog(isPaid: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val reasons = listOf("Hết hàng / Lỗi kho", "Sản phẩm hư hỏng", "Khu vực không hỗ trợ", "Nghi ngờ gian lận", "Khách hàng yêu cầu hủy", "Khác")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Lý do hủy đơn", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                if (isPaid) {
                    Surface(color = Color(0xFFFFF0F0), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("⚠️ Đơn này ĐÃ THANH TOÁN. Nếu hủy, đơn sẽ được chuyển sang Tab 'Hoàn Tiền'.", color = Color(0xFFD32F2F), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(12.dp))
                    }
                } else {
                    Text("Vui lòng chọn lý do hủy. Thông báo sẽ được gửi đến khách hàng.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                reasons.forEach { text ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = (text == selectedReason), onClick = { selectedReason = text }).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (text == selectedReason), onClick = { selectedReason = text }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF)))
                        Text(text = text, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (selectedReason == "Khác") {
                    OutlinedTextField(value = customReason, onValueChange = { customReason = it }, placeholder = { Text("Nhập lý do hủy...") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Khác") customReason.ifBlank { "Lý do khác" } else selectedReason
                    onConfirm(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Hủy Đơn") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng", color = Color.Gray) } }
    )
}
@Composable
fun StatusBadge(status: String) {
    val (label, color, bgColor) = when(status) {
        OrderStatus.PENDING -> Triple("Chờ duyệt", Color(0xFFE65100), Color(0xFFFFE0B2))
        OrderStatus.CONFIRMED -> Triple("Lấy hàng", Color(0xFF1565C0), Color(0xFFE3F2FD))
        OrderStatus.SHIPPING -> Triple("Đang giao", Color(0xFF00838F), Color(0xFFE0F7FA))
        OrderStatus.DELIVERED -> Triple("Hoàn thành", Color(0xFF2E7D32), Color(0xFFE8F5E9))
        OrderStatus.CANCELLED -> Triple("Đã hủy", Color(0xFFC62828), Color(0xFFFFEBEE))
        OrderStatus.REFUNDING -> Triple("Chờ hoàn tiền", Color(0xFFF57C00), Color(0xFFFFF3E0))
        OrderStatus.REFUNDED -> Triple("Đã hoàn tiền", Color(0xFF1976D2), Color(0xFFE3F2FD))
        else -> Triple("Không rõ", Color.Gray, Color(0xFFEEEEEE))
    }

    Surface(color = bgColor, shape = RoundedCornerShape(4.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OptimizedAdminOrderItem(
    order: Order,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onSelect: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED && order.status != OrderStatus.REFUNDED) {
                        onConfirm(); true
                    } else false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (order.status == OrderStatus.PENDING || order.status == OrderStatus.CONFIRMED) {
                        onCancel(); false
                    } else false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED && order.status != OrderStatus.REFUNDED,
        enableDismissFromEndToStart = order.status == OrderStatus.PENDING || order.status == OrderStatus.CONFIRMED,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Xanh lá: Xác nhận
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336) // Đỏ: Hủy
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().background(color, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.CheckCircle else Icons.Default.Delete
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = onSelect
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) BorderStroke(2.dp, Color(0xFF007AFF)) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
                    }
                    Column {
                        Text("Đơn hàng #${order.id.takeLast(6).uppercase()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault()).format(order.createdAt), fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    StatusBadge(order.status)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(order.receiverName, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("₫${DecimalFormat("#,###").format(order.totalPrice)}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 16.sp)
                }

                Text(
                    text = order.items.joinToString { "${it.product.name} x${it.quantity}" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}