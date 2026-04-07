package com.example.storepromax.presentation.myvoucher

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.presentation.cart.GunplaBlue
import com.example.storepromax.presentation.cart.TealFreeship
import com.example.storepromax.presentation.cart.BgLight
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVoucherScreen(
    navController: NavController,
    viewModel: MyVoucherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val myVouchers by viewModel.myVouchers.collectAsState()

    var inputCode by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val currentTime = System.currentTimeMillis()
    val dateFormat =
        remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()) }
    val priceFormatter = remember { DecimalFormat("#,###") }
    val availableVouchers = myVouchers.filter { userVoucher ->
        val v = userVoucher.voucher
        val validTime: Long = (v.expirationDate as? Number)?.toLong() ?: 0L
        val isNotExpired = validTime == 0L || validTime > currentTime

        userVoucher.status == "AVAILABLE" && isNotExpired
    }
    val historyVouchers = myVouchers.filter { userVoucher ->
        val v = userVoucher.voucher
        val validTime: Long = (v.expirationDate as? Number)?.toLong() ?: 0L
        val isExpired = validTime > 0L && validTime <= currentTime

        userVoucher.status != "AVAILABLE" || isExpired
    }

    Scaffold(
        containerColor = BgLight,
        topBar = {
            TopAppBar(
                title = { Text("Kho Voucher", fontWeight = FontWeight.Bold, color = GunplaBlue) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GunplaBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it.uppercase() },
                    placeholder = { Text("Nhập mã voucher...", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GunplaBlue)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (inputCode.isNotBlank()) {
                            viewModel.claimVoucherByCode(inputCode) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) inputCode = ""
                            }
                        }
                    },
                    enabled = inputCode.isNotBlank(),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue)
                ) { Text("LƯU MÃ", fontWeight = FontWeight.Bold) }
            }
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = GunplaBlue
            ) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                    Text(
                        "Có hiệu lực (${availableVouchers.size})",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                    Text(
                        "Lịch sử (${historyVouchers.size})",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            val currentList = if (selectedTabIndex == 0) availableVouchers else historyVouchers

            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có mã giảm giá nào ở đây", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.id }) { userVoucher ->
                        val v = userVoucher.voucher
                        val formatter = DecimalFormat("#,###")
                        val isAvailable = selectedTabIndex == 0
                        val bgColor =
                            if (!isAvailable) Color.Gray else if (v.type == "FREESHIP") TealFreeship else GunplaBlue
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(90.dp)
                                        .background(bgColor), contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    ) {
                                        Icon(
                                            if (v.type == "FREESHIP") Icons.Default.LocalShipping else Icons.Default.ConfirmationNumber,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (v.type == "FREESHIP") "FREESHIP" else "DISCOUNT",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(v.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Đơn tối thiểu ₫${priceFormatter.format(v.minOrderValue)}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )

                                    if (isAvailable) {
                                        val validTime: Long =
                                            (v.expirationDate as? Number)?.toLong() ?: 0L
                                        val hsdText = if (validTime > 0L) {
                                            val dateFormat = java.text.SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm",
                                                java.util.Locale.getDefault()
                                            )
                                            "HSD: ${dateFormat.format(java.util.Date(validTime))}"
                                        } else {
                                            "HSD: Không giới hạn"
                                        }
                                        Text(
                                            text = hsdText,
                                            color = GunplaBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Button(
                                            onClick = { navController.navigate("home_screen") },
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .height(32.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                                            contentPadding = PaddingValues(
                                                horizontal = 12.dp,
                                                vertical = 0.dp
                                            )
                                        ) { Text("Dùng ngay", fontSize = 12.sp) }
                                    } else {
                                        Text(
                                            text = if (userVoucher.status == "USED") "Đã sử dụng" else "Đã hết hạn",
                                            color = Color.Red,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}