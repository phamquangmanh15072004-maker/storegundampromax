package com.example.storepromax.presentation.admin.voucher

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.storepromax.domain.model.Voucher
import java.text.DecimalFormat

val AdminBlue = Color(0xFF1565C0)
val BgAdmin = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVoucherScreen(
    navController: NavController,
    viewModel: AdminVoucherViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredVouchers by viewModel.filteredVouchers.collectAsState()
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val isLoading by viewModel.isLoading.collectAsState()
    val activeVouchers = filteredVouchers.filter { (it.usageLimit == 0L || it.usedCount < it.usageLimit) && it.isActive }
    val depletedVouchers = filteredVouchers.filter { (it.usageLimit > 0 && it.usedCount >= it.usageLimit) || !it.isActive }
    LaunchedEffect(Unit) {
        viewModel.fetchAllVouchersForAdmin()
    }
    Scaffold(
        containerColor = BgAdmin,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quản lý Voucher",
                        fontWeight = FontWeight.Bold,
                        color = AdminBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = AdminBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("admin_voucher_form") },
                containerColor = AdminBlue, contentColor = Color.White
            ) { Icon(Icons.Default.Add, "Tạo mới") }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Tìm theo mã Code hoặc Tên...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdminBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = AdminBlue
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }) {
                    Text(
                        "Đang chạy (${activeVouchers.size})",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }) {
                    Text(
                        "Đã hết/Tắt (${depletedVouchers.size})",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val currentList = if (selectedTabIndex == 0) activeVouchers else depletedVouchers
            if (isLoading && currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminBlue)
                }
            } else if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy Voucher nào", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.id }) { voucher ->
                        AdminVoucherCard(
                            voucher = voucher,
                            onToggle = {
                                viewModel.toggleVoucherStatus(
                                    voucher.id,
                                    voucher.isActive
                                )
                            },
                            onClick = { navController.navigate("admin_voucher_form/${voucher.id}") }
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun AdminVoucherCard(voucher: Voucher, onToggle: () -> Unit, onClick: () -> Unit) {
    val formatter = DecimalFormat("#,###")
    val isDepleted = (voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit) || !voucher.isActive

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (voucher.type == "FREESHIP") Color(0xFF00BFA5) else AdminBlue)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        voucher.type,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    voucher.code,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = if (voucher.isActive) AdminBlue else Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(voucher.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(
                "Đơn tối thiểu: ₫${formatter.format(voucher.minOrderValue)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Đã dùng: ${voucher.usedCount} / ${voucher.usageLimit}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDepleted) Color.Red else Color.DarkGray
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDepleted) {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) { Text("Gia hạn", fontSize = 12.sp) }
                    } else {
                        Switch(
                            checked = voucher.isActive,
                            onCheckedChange = { onToggle() },
                            modifier = Modifier
                                .scale(0.8f)
                                .zIndex(1f)
                        )
                    }
                }
            }
        }
    }
}
