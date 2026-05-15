package com.example.storepromax.presentation.admin.voucher

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.storepromax.domain.model.Voucher
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVoucherFormScreen(
    navController: NavController,
    voucherId: String? = null,
    viewModel: AdminVoucherViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentUsedCount by remember { mutableStateOf(0L) }
    var isDepleted by remember { mutableStateOf(false) }

    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var discountValue by remember { mutableStateOf("") }
    var minOrderValue by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("") }
    var isFreeship by remember { mutableStateOf(false) }

    var expirationDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000L * 7) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEditMode = voucherId != null

    LaunchedEffect(voucherId) {
        if (voucherId != null) {
            viewModel.getVoucherById(voucherId) { voucher ->
                if (voucher != null) {
                    isDepleted = (voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit) || !voucher.isActive
                    code = voucher.code
                    title = voucher.title
                    discountValue = voucher.discountValue.toString()
                    minOrderValue = voucher.minOrderValue.toString()
                    usageLimit = voucher.usageLimit.toString()
                    isFreeship = voucher.type == "FREESHIP"
                    currentUsedCount = voucher.usedCount

                    expirationDate = if (voucher.expirationDate < System.currentTimeMillis()) System.currentTimeMillis() + 86400000L * 7 else voucher.expirationDate
                }
            }
        }
    }

    val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expirationDate))

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Gia hạn / Sửa Voucher" else "Tạo Voucher Mới", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0)) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF1565C0)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val finalUsedCount = if (isDepleted) 0L else currentUsedCount

                        val newVoucher = Voucher(
                            id = voucherId ?: "",
                            code = code,
                            title = title,
                            type = if (isFreeship) "FREESHIP" else "DISCOUNT",
                            discountValue = discountValue.toLongOrNull() ?: 0L,
                            minOrderValue = minOrderValue.toLongOrNull() ?: 0L,
                            usageLimit = usageLimit.toLongOrNull() ?: 0L,
                            usedCount = finalUsedCount,
                            expirationDate = expirationDate,
                            isActive = true
                        )
                        viewModel.saveVoucher(newVoucher) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("LƯU VOUCHER", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thông tin cơ bản", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Mã CODE (VD: TET2026)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề mô tả") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thiết lập giá trị", fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = discountValue, onValueChange = { discountValue = it }, label = { Text("Giảm (VNĐ)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = minOrderValue, onValueChange = { minOrderValue = it }, label = { Text("Đơn tối thiểu") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                    }

                    OutlinedTextField(value = usageLimit, onValueChange = { usageLimit = it }, label = { Text("Số lượng phát hành (Lượt)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)

                    OutlinedTextField(
                        value = dateString, onValueChange = { }, label = { Text("Ngày hết hạn") }, readOnly = true,
                        trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, "Chọn ngày") } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0F2F1), RoundedCornerShape(8.dp)).padding(8.dp)
                    ) {
                        Checkbox(checked = isFreeship, onCheckedChange = { isFreeship = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00BFA5)))
                        Text("Miễn Phí Vận Chuyển", fontWeight = FontWeight.Medium, color = Color(0xFF00796B))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expirationDate)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { expirationDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis(); showDatePicker = false }) { Text("CHỌN") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("HỦY") } }
            ) { DatePicker(state = datePickerState) }
        }
    }
}
