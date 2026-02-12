package com.example.storepromax.presentation.checkout

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun <T> SearchableDropdown(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemToString: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchText) {
        if (searchText.isBlank()) items
        else items.filter { itemToString(it).contains(searchText, ignoreCase = true) }
    }

    LaunchedEffect(selectedItem) {
        if (selectedItem != null) {
            searchText = itemToString(selectedItem)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                expanded = true
            },
            label = { Text(label, color = Color.Gray) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF007AFF),
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        if (filteredItems.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White).heightIn(max = 250.dp) // Giới hạn chiều cao
            ) {
                filteredItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemToString(item), color = Color.Black) },
                        onClick = {
                            onItemSelected(item)
                            searchText = itemToString(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth()
    )
    Divider(color = Color(0xFF00D4FF), thickness = 1.dp, modifier = Modifier.padding(top = 4.dp).width(50.dp))
}

@Composable
fun TechTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF00D4FF)) },
        modifier = Modifier.fillMaxWidth(),
        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00D4FF),
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF00D4FF),
            focusedContainerColor = Color(0xFF1A1A1A),
            unfocusedContainerColor = Color(0xFF1A1A1A)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

@Composable
fun MiniCartItem(item: CartItem) {
    val formatter = DecimalFormat("#,###")
    Surface(
        color = Color(0xFF2D2D2D),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.quantity}x",
                color = Color(0xFF00D4FF),
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = "₫${formatter.format(item.totalPrice)}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CostRow(label: String, amount: Long, isTotal: Boolean = false, color: Color = Color.White) {
    val formatter = DecimalFormat("#,###")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = Color.Gray,
            fontSize = if (isTotal) 16.sp else 14.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "₫${formatter.format(amount)}",
            color = color,
            fontSize = if (isTotal) 20.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
fun PaymentOptionCard(
    title: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF00D4FF) else Color.DarkGray
    val containerColor = if (isSelected) Color(0xFF00D4FF).copy(alpha = 0.1f) else Color(0xFF1A1A1A)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
        border = BorderStroke(1.dp, borderColor),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF00D4FF) else Color.Gray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF00D4FF), androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}

@Composable
fun QRPaymentDialog(
    amount: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val qrUrl = "https://img.vietqr.io/image/MB-0987654321-compact.png?amount=$amount&addInfo=Thanh toan don hang Gundam"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(
                "SCAN DATA LINK",
                color = Color(0xFF00D4FF),
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = qrUrl,
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(250.dp)
                        .background(Color.White)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Amount: ₫${java.text.DecimalFormat("#,###").format(amount)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Waiting for data packet transfer...",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF))
            ) {
                Text("CONFIRM TRANSFER", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}

@Composable
fun OrderSuccessDialog(
    onGoHome: () -> Unit
) {
    val neonGreen = Color(0xFF00FFCC)
    val darkMetal = Color(0xFF1A1A1A)

    AlertDialog(
        onDismissRequest = {  },
        containerColor = darkMetal,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = neonGreen,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Đặt Hàng Thành Công",
                    color = neonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        },
        text = {
            Text(
                text = "Đơn hàng của bạn đã được tiếp nhận.\nHệ thống đang chuẩn bị vật phẩm để vận chuyển!",
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onGoHome,
                colors = ButtonDefaults.buttonColors(containerColor = neonGreen),
                modifier = Modifier.fillMaxWidth(),
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
            ) {
                Text(
                    "Quay lại Trang Chủ",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    viewModel: CheckoutViewModel = hiltViewModel(),

    productId: String? = null,
    quantity: Int? = null
) {
    val selectedItems by viewModel.selectedItems.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val context = LocalContext.current
    val name by viewModel.name.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val specificAddress by viewModel.specificAddress.collectAsState()


    val provinces by viewModel.provinces.collectAsState()
    val districts by viewModel.districts.collectAsState()
    val wards by viewModel.wards.collectAsState()
    val selectedProvince by viewModel.selectedProvince.collectAsState()
    val selectedDistrict by viewModel.selectedDistrict.collectAsState()
    val selectedWard by viewModel.selectedWard.collectAsState()

    var showQRDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val bgLight = Color(0xFFF5F5F5)
    val primaryColor = Color(0xFF007AFF)
    LaunchedEffect(productId, quantity) {
        if (productId != null && quantity != null && quantity > 0) {
            viewModel.loadSingleProductForCheckout(productId, quantity)
        } else {
            viewModel.loadSelectedCartItems()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = bgLight,
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tổng thanh toán", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            "₫${DecimalFormat("#,###").format(totalPrice + 30000)}",
                            color = Color(0xFFD32F2F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            if (paymentMethod == "BANKING") showQRDialog = true
                            else viewModel.submitOrder { showSuccessDialog = true }
                        },
                        enabled = !isProcessing && selectedItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Đặt hàng", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard("Địa chỉ nhận hàng") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SimpleTextField(value = name, onValueChange = { viewModel.onNameChange(it) }, label = "Họ và tên", icon = Icons.Default.Person)
                        SimpleTextField(value = phone, onValueChange = { viewModel.onPhoneChange(it) }, label = "Số điện thoại", icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)

                        Divider(color = Color(0xFFEEEEEE))

                        SearchableDropdown(
                            label = "Tỉnh / Thành phố",
                            items = provinces,
                            selectedItem = selectedProvince,
                            onItemSelected = { viewModel.onProvinceSelected(it) },
                            itemToString = { it.name }
                        )

                        SearchableDropdown(
                            label = "Quận / Huyện",
                            items = districts,
                            selectedItem = selectedDistrict,
                            onItemSelected = { viewModel.onDistrictSelected(it) },
                            itemToString = { it.name }
                        )

                        SearchableDropdown(
                            label = "Phường / Xã",
                            items = wards,
                            selectedItem = selectedWard,
                            onItemSelected = { viewModel.onWardSelected(it) },
                            itemToString = { it.name }
                        )

                        SimpleTextField(value = specificAddress, onValueChange = { viewModel.onSpecificAddressChange(it) }, label = "Số nhà, tên đường", icon = Icons.Default.LocationOn)
                    }
                }
            }

            item {
                SectionCard("Sản phẩm") {
                    selectedItems.forEach { item ->
                        Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.quantity}x", fontWeight = FontWeight.Bold, color = primaryColor)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(item.product.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("₫${DecimalFormat("#,###").format(item.totalPrice)}", fontWeight = FontWeight.Medium)
                        }
                        if (item != selectedItems.last()) Divider(color = Color(0xFFEEEEEE))
                    }
                }
            }

            item {
                SectionCard("Phương thức thanh toán") {
                    PaymentOptionItem("Thanh toán khi nhận hàng (COD)", Icons.Default.Money, paymentMethod == "COD") { viewModel.onPaymentMethodChange("COD") }
                    Spacer(modifier = Modifier.height(8.dp))
                    PaymentOptionItem("Chuyển khoản ngân hàng (QR)", Icons.Default.QrCode, paymentMethod == "BANKING") { viewModel.onPaymentMethodChange("BANKING") }
                }
            }
            item {
                SectionCard("Chi tiết thanh toán") {
                    BillRow("Tổng tiền hàng", totalPrice)
                    BillRow("Phí vận chuyển", 30000)
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                    BillRow("Tổng thanh toán", totalPrice + 30000, isTotal = true)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showQRDialog) {
        QRPaymentDialogLight(
            amount = totalPrice + 30000,
            onDismiss = { showQRDialog = false },
            onConfirm = {
                showQRDialog = false
                viewModel.submitOrder {
                    showSuccessDialog = true
                }
            }
        )
    }

    if (showSuccessDialog) {
        OrderSuccessDialogLight {
            showSuccessDialog = false
            navController.navigate("home_screen") {
                popUpTo("home_screen") { inclusive = true }
            }
        }
    }
}
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SimpleTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF007AFF),
            unfocusedBorderColor = Color.LightGray,
            cursorColor = Color(0xFF007AFF),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

@Composable
fun PaymentOptionItem(title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF007AFF) else Color.LightGray
    val bgColor = if (isSelected) Color(0xFFE3F2FD) else Color.White

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        color = bgColor
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color(0xFF007AFF) else Color.Gray)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal, color = Color.Black)
        }
    }
}

@Composable
fun BillRow(label: String, amount: Long, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isTotal) Color.Black else Color.Gray, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal)
        Text("₫${DecimalFormat("#,###").format(amount)}", color = if (isTotal) Color(0xFFD32F2F) else Color.Black, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal)
    }
}

// Dialogs Light Theme
@Composable
fun OrderSuccessDialogLight(onGoHome: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) },
        title = { Text("Đặt hàng thành công!", fontWeight = FontWeight.Bold) },
        text = { Text("Cảm ơn bạn đã mua sắm. Đơn hàng đang được xử lý.") },
        confirmButton = { Button(onClick = onGoHome, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Về trang chủ") } }
    )
}

@Composable
fun QRPaymentDialogLight(amount: Long, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val qrUrl = "https://img.vietqr.io/image/MB-0375520600-compact.png?amount=$amount&addInfo=Thanh toan don hang"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "Quét mã thanh toán",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    AsyncImage(
                        model = qrUrl,
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Số tiền cần thanh toán:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Text(
                    text = "₫${DecimalFormat("#,###").format(amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFFD32F2F)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Đã chuyển khoản xong")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hủy bỏ", color = Color.Gray)
            }
        }
    )
}