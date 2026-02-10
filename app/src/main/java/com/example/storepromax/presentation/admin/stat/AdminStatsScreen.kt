package com.example.storepromax.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.ChartPoint
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(
    navController: NavController,
    viewModel: AdminStatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filterText by viewModel.filterText.collectAsState()
    val bgLight = Color(0xFFF5F5F5)
    val white = Color.White
    val primaryColor = Color(0xFF007AFF)
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = datePickerState.selectedStartDateMillis
                    val end = datePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        viewModel.loadStats(start, end)
                    }
                    showDatePicker = false
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DateRangePicker(state = datePickerState)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Thống kê", fontWeight = FontWeight.Bold)
                        Text(filterText, fontSize = 12.sp, color = Color.Gray)
                    }
                }, navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Chọn ngày")
                    }
                    IconButton(onClick = { viewModel.loadStats(null, null) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = bgLight
    ) { padding ->
        if (isLoading) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatCard(
                                title = "Doanh thu",
                                value = "₫${DecimalFormat("#,###").format(stats.totalRevenue)}",
                                icon = Icons.Default.AttachMoney,
                                color = Color(0xFF4CAF50), // Xanh lá
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Đơn hàng",
                                value = stats.totalOrders.toString(),
                                icon = Icons.Default.ShoppingCart,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatCard(
                                title = "Khách hàng",
                                value = stats.totalUsers.toString(),
                                icon = Icons.Default.People,
                                color = Color(0xFFFF9800), // Cam
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Sản phẩm",
                                value = stats.totalProducts.toString(),
                                icon = Icons.Default.Inventory,
                                color = Color(0xFF9C27B0), // Tím
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. BIỂU ĐỒ DOANH THU (Bar Chart Tự chế)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = white),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Doanh thu 7 ngày gần nhất",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // Vẽ biểu đồ
                            if (stats.revenueChartData.isNotEmpty()) {
                                SimpleBarChart(data = stats.revenueChartData)
                            } else {
                                Text(
                                    "Chưa có dữ liệu",
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }

                // 3. TOP SẢN PHẨM BÁN CHẠY
                item {
                    Text(
                        "Top sản phẩm bán chạy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(stats.topProducts.size) { index ->
                    val product = stats.topProducts[index]
                    TopProductItem(rank = index + 1, product = product)
                }

                // 4. MENU NHANH
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Truy cập nhanh", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionChip(
                            "Quản lý Đơn",
                            Icons.Default.ReceiptLong
                        ) { navController.navigate("admin_order") }
                        QuickActionChip(
                            "Duyệt Bài",
                            Icons.Default.FactCheck
                        ) { navController.navigate("admin_feed_approval") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionChip(
                            "Sản phẩm",
                            Icons.Default.Inventory2
                        ) { navController.navigate("admin_product_list") }
                        QuickActionChip(
                            "Người dùng",
                            Icons.Default.SupervisedUserCircle
                        ) { navController.navigate("admin_user") }
                    }
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Text(title, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SimpleBarChart(data: List<ChartPoint>) {
    val maxVal = data.maxOfOrNull { it.value }?.toFloat() ?: 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                val heightFraction = if (maxVal > 0) (point.value / maxVal) else 0f

                if (point.value > 0) {
                    Text(
                        text = compactFormat(point.value),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight(heightFraction.coerceAtLeast(0.01f) * 0.75f)
                        .background(
                            if (point.value == maxVal.toLong()) Color(0xFF007AFF) else Color(
                                0xFF90CAF9
                            ),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = point.label,
                    fontSize = 10.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
fun TopProductItem(rank: Int, product: com.example.storepromax.domain.model.TopProduct) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Surface(
                color = if (rank <= 3) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$rank",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (rank <= 3) Color.White else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Image
            AsyncImage(
                model = product.imageUrl.ifBlank { "https://via.placeholder.com/100" },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                Text("Đã bán: ${product.soldCount}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun QuickActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

fun compactFormat(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.0fk", number / 1_000.0)
        else -> number.toString()
    }
}