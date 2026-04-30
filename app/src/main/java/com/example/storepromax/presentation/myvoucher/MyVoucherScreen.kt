package com.example.storepromax.presentation.myvoucher

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.presentation.cart.AlertRed
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp) // Cố định 105dp
                                .alpha(if (isAvailable) 1f else 0.5f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(4.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(96.dp)
                                        .background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            if (v.type == "FREESHIP") Icons.Default.LocalShipping else Icons.Default.ConfirmationNumber,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            if (v.type == "FREESHIP") "FREESHIP" else "DISCOUNT",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxHeight().width(1.dp)) {
                                    drawLine(
                                        color = Color(0xFFE0E0E0),
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(0f, size.height),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = v.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF222222),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 20.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Đơn từ ₫${priceFormatter.format(v.minOrderValue)}",
                                            color = Color(0xFF757575),
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        val validTime: Long = (v.expirationDate as? Number)?.toLong() ?: 0L
                                        val hsdText = if (validTime > 0L) {
                                            val dFormat = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                                            "Có hiệu lực: ${dFormat.format(java.util.Date(validTime))}"
                                        } else {
                                            "Không giới hạn"
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = if (isAvailable) AlertRed else Color(0xFF9E9E9E),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = hsdText,
                                                color = if (isAvailable) AlertRed else Color(0xFF9E9E9E),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    if (isAvailable) {
                                        OutlinedButton(
                                            onClick = { navController.navigate("home_screen") },
                                            modifier = Modifier.height(32.dp).widthIn(min = 76.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = bgColor),
                                            border = BorderStroke(1.dp, bgColor)
                                        ) {
                                            Text("Dùng ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(
                                            text = if (userVoucher.status == "USED") "Đã dùng" else "Hết hạn",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 4.dp)
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