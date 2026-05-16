package com.example.storepromax.presentation.checkout

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.presentation.component.SearchableDropdown
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.DecimalFormat

val GunplaBlue = Color(0xFF0D47A1)
val BgLight = Color(0xFFF5F5F5)
val AlertRed = Color(0xFFFF3B30)
val SuccessGreen = Color(0xFF00C853)
val TealFreeship = Color(0xFF00BFA5)

data class PaymentPopupData(val url: String, val bin: String, val accNo: String, val amount: Long, val description: String, val orderId: String)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    viewModel: CheckoutViewModel = hiltViewModel(),
    productId: String? = null,
    quantity: Int? = null,
    discountCode: String? = null,
    freeshipCode: String? = null
) {
    val selectedItems by viewModel.selectedItems.collectAsState()
    val subTotal by viewModel.totalPrice.collectAsState()
    val shipFee by viewModel.shippingFee.collectAsState()
    val prodDisc by viewModel.productDiscountAmount.collectAsState()
    val shipDisc by viewModel.freeshipAmount.collectAsState()
    val finalTotal by viewModel.finalTotalPrice.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val name by viewModel.name.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val selectedProvince by viewModel.selectedProvince.collectAsState()
    val districts by viewModel.districts.collectAsState()
    val selectedDistrict by viewModel.selectedDistrict.collectAsState()
    val wards by viewModel.wards.collectAsState()
    val selectedWard by viewModel.selectedWard.collectAsState()
    val specificAddress by viewModel.specificAddress.collectAsState()

    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val shippingMethod by viewModel.shippingMethod.collectAsState()
    val bankingEnabled = finalTotal > 0L

    val availableVouchers by viewModel.availableVouchers.collectAsState()
    val selectedDiscountVoucher by viewModel.selectedDiscountVoucher.collectAsState()
    val selectedFreeshipVoucher by viewModel.selectedFreeshipVoucher.collectAsState()

    var showVoucherSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showTransferSuccessDialog by remember { mutableStateOf(false) }
    var paymentPopupData by remember { mutableStateOf<PaymentPopupData?>(null) }

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(productId, quantity, discountCode, freeshipCode) {
        val isBuyNow = !productId.isNullOrBlank() && productId != "{productId}"

        if (isBuyNow && quantity != null && quantity > 0) {
            viewModel.loadSingleProductForCheckout(productId!!, quantity)
        } else {
            viewModel.loadSelectedCartItems()
        }
        viewModel.setInitialVouchers(discountCode, freeshipCode)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            if (message == "PAYMENT_SUCCESS") {
                paymentPopupData = null
                showTransferSuccessDialog = true
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(finalTotal, paymentMethod) {
        if (finalTotal <= 0L && paymentMethod == "BANKING") {
            viewModel.onPaymentMethodChange("COD")
        }
    }

    Scaffold(
        containerColor = BgLight,
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán", fontWeight = FontWeight.Bold, color = GunplaBlue, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, tint = GunplaBlue, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text("Tổng thanh toán", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            text = "₫${DecimalFormat("#,###").format(finalTotal)}",
                            color = AlertRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (prodDisc > 0 || shipDisc > 0) {
                            Text(
                                text = "Tiết kiệm ₫${DecimalFormat("#,###").format(prodDisc + shipDisc)}",
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            viewModel.submitOrder(
                                onSuccess = { showSuccessDialog = true },
                                onShowPaymentPopup = { url, bin, accNo, description, orderId ->
                                    paymentPopupData = PaymentPopupData(url, bin, accNo, finalTotal, description, orderId)
                                }
                            )
                        },
                        enabled = !isProcessing && selectedItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp).widthIn(min = 120.dp)
                    ) {
                        if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("ĐẶT HÀNG", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { pv ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pv).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard("Địa chỉ nhận hàng") {
                    SimpleTextField(name, { viewModel.onNameChange(it) }, "Họ tên", Icons.Default.Person)
                    Spacer(Modifier.height(8.dp))
                    SimpleTextField(phone, { viewModel.onPhoneChange(it) }, "Số điện thoại", Icons.Default.Phone)
                    Spacer(Modifier.height(8.dp))

                    SearchableDropdown(
                        label = "Tỉnh thành",
                        items = provinces,
                        selectedItem = selectedProvince,
                        onItemSelected = { viewModel.onProvinceSelected(it) },
                        itemToString = { it.provinceName }
                    )
                    Spacer(Modifier.height(8.dp))
                    SearchableDropdown(
                        label = "Quận huyện",
                        items = districts,
                        selectedItem = selectedDistrict,
                        onItemSelected = { viewModel.onDistrictSelected(it) },
                        itemToString = { it.districtName }
                    )
                    Spacer(Modifier.height(8.dp))
                    SearchableDropdown(
                        label = "Phường xã",
                        items = wards,
                        selectedItem = selectedWard,
                        onItemSelected = { viewModel.onWardSelected(it) },
                        itemToString = { it.wardName }
                    )
                    Spacer(Modifier.height(8.dp))
                    SimpleTextField(specificAddress, { viewModel.onSpecificAddressChange(it) }, "Số nhà, tên đường", Icons.Default.LocationOn)
                }
            }

            item {
                SectionCard("Sản phẩm") {
                    selectedItems.forEachIndexed { index, item ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.quantity}x", color = GunplaBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(item.product.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            Text("₫${DecimalFormat("#,###").format(item.liveTotalPrice)}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        if (index != selectedItems.lastIndex) HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            item {
                SectionCard("Gói giao hàng") {
                    PaymentOptionItem(
                        title = "Tiêu chuẩn (Dự kiến 3-5 ngày)",
                        icon = Icons.Default.LocalShipping,
                        selected = shippingMethod == "STANDARD"
                    ) { viewModel.onShippingMethodChange("STANDARD") }

                    Spacer(Modifier.height(8.dp))

                    PaymentOptionItem(
                        title = "Hỏa tốc (Dự kiến 1-2 ngày)",
                        icon = Icons.Default.FlashOn,
                        selected = shippingMethod == "EXPRESS"
                    ) { viewModel.onShippingMethodChange("EXPRESS") }
                }
            }

            item {
                SectionCard("Phương thức thanh toán") {
                    PaymentOptionItem("Thanh toán khi nhận hàng (COD)", Icons.Default.Money, paymentMethod == "COD") { viewModel.onPaymentMethodChange("COD") }
                    Spacer(Modifier.height(8.dp))
                    PaymentOptionItem(
                        title = if (bankingEnabled) "Chuyển khoản an toàn qua PayOS" else "Chuyển khoản PayOS (không áp dụng cho đơn 0đ)",
                        icon = Icons.Default.QrCode,
                        selected = paymentMethod == "BANKING",
                        enabled = bankingEnabled
                    ) { viewModel.onPaymentMethodChange("BANKING") }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                    VoucherSelectionRow(
                        productDiscount = prodDisc,
                        freeshipAmount = shipDisc,
                        onClick = {
                            viewModel.refreshVouchers()
                            showVoucherSheet = true
                        }
                    )
                }
            }

            item {
                SectionCard("Chi tiết thanh toán") {
                    BillRow("Tiền hàng", subTotal)
                    BillRow("Phí vận chuyển", shipFee)
                    if (shipDisc > 0) BillRow("Miễn phí vận chuyển", -shipDisc, color = TealFreeship)
                    if (prodDisc > 0) BillRow("Voucher giảm giá", -prodDisc, color = SuccessGreen)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                    BillRow("Tổng thanh toán", finalTotal, isTotal = true)
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showVoucherSheet) {
        VoucherBottomSheet(
            availableVouchers = availableVouchers,
            selectedDiscount = selectedDiscountVoucher,
            selectedFreeship = selectedFreeshipVoucher,
            currentSubTotal = subTotal,
            onDismiss = { showVoucherSheet = false },
            onApplyCode = { code -> viewModel.applyVoucherByCode(code) { success, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } },
            onSelectVoucher = { voucher -> viewModel.toggleVoucher(voucher) }
        )
    }

    if (showSuccessDialog) {
        OrderSuccessDialogLight {
            showSuccessDialog = false
            navController.navigate("home_screen") { popUpTo("home_screen") { inclusive = true } }
        }
    }
    if (paymentPopupData != null) {
        HybridQRPaymentDialog(
            data = paymentPopupData!!,
            onDismiss = {
                paymentPopupData = null
                navController.navigate("home_screen") { popUpTo("home_screen") { inclusive = true } }
            },
            onCancelOrder = {
                viewModel.cancelOrderFromPopup(paymentPopupData!!.orderId)

                paymentPopupData = null
                navController.navigate("home_screen") { popUpTo("home_screen") { inclusive = true } }
            },
            onTimeout = {
                viewModel.cancelOrderFromPopup(
                    paymentPopupData!!.orderId,
                    "Hết thời gian thanh toán (5 phút)"
                )
                paymentPopupData = null
                navController.navigate("home_screen") { popUpTo("home_screen") { inclusive = true } }
            },
            onOpenWeb = { url ->
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {
                    Toast.makeText(context, "Không thể mở ứng dụng", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    if (showTransferSuccessDialog) {
        TransferSuccessDialog {
            showTransferSuccessDialog = false
            navController.navigate("home_screen") { popUpTo("home_screen") { inclusive = true } }
        }
    }
}

@Composable
fun HybridQRPaymentDialog(
    data: PaymentPopupData,
    onDismiss: () -> Unit,
    onCancelOrder: () -> Unit,
    onTimeout: () -> Unit,
    onOpenWeb: (String) -> Unit
) {
    val context = LocalContext.current
    var timeLeft by remember { mutableIntStateOf(5 * 60) }
    var qrRetryKey by remember(data.orderId) { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        Toast.makeText(context, "Mã QR có thể đã hết hạn. Vui lòng kiểm tra Lịch sử đơn hàng!", Toast.LENGTH_LONG).show()
        onTimeout()
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val description = URLEncoder.encode(data.description, "UTF-8")
    val accountName = URLEncoder.encode("Gunpla Store", "UTF-8")
    val qrUrl = "https://img.vietqr.io/image/${data.bin}-${data.accNo}-compact2.png?amount=${data.amount}&addInfo=$description&accountName=$accountName"

    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        title = { Text("Thanh toán đơn hàng", fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(color = Color(0xFFFFF0F0), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text("Đơn hàng sẽ được lưu cho đến khi hết thời gian hiệu lực!!!", color = Color(0xFFD32F2F), fontSize = 12.sp, modifier = Modifier.padding(12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                Text("Thời gian tồn tại mã QR:", fontSize = 14.sp, color = Color.Gray)
                Text(timeString, fontSize = 28.sp, fontWeight = FontWeight.Black, color = AlertRed)

                Spacer(Modifier.height(8.dp))

                Text("Phương thức 1: Quét mã QR để thanh toán:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GunplaBlue)
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    SubcomposeAsyncImage(
                        model = "$qrUrl&retry=$qrRetryKey",
                        contentDescription = "QR Code",
                        modifier = Modifier.size(216.dp).padding(8.dp),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = GunplaBlue, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                    Spacer(Modifier.height(10.dp))
                                    Text("Đang tải mã QR...", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        },
                        error = {
                            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = AlertRed, modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Mạng chậm nên chưa tải được mã QR.", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = { qrRetryKey++ }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Tải lại QR")
                                    }
                                }
                            }
                        }
                    )
                }
                Text("Số tiền: ₫${DecimalFormat("#,###").format(data.amount)}", color = AlertRed, fontWeight = FontWeight.Black, fontSize = 22.sp)

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(16.dp))

                Text("Phương thức 2: Chuyển đển Ngân Hàng:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GunplaBlue)

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onOpenWeb(data.url) },
                    colors = ButtonDefaults.buttonColors(GunplaBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mở App Ngân Hàng Trên Máy", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Đã ghi nhận! Đơn hàng sẽ được duyệt khi tiền vào tài khoản.", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tôi đã chuyển khoản xong", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onCancelOrder() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Đổi ý / Hủy đơn hàng này", color = AlertRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}
@Composable
fun VoucherSelectionRow(productDiscount: Long, freeshipAmount: Long, onClick: () -> Unit) {
    val formatter = DecimalFormat("#,###")
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GunplaBlue)
        Spacer(modifier = Modifier.width(12.dp))

        Text("Voucher", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)

        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            if (productDiscount == 0L && freeshipAmount == 0L) {
                Text("Chọn hoặc nhập mã", color = Color.Gray, fontSize = 14.sp)
            } else {
                if (productDiscount > 0) {
                    Text(
                        text = "-₫${formatter.format(productDiscount)}",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }
                if (freeshipAmount > 0) {
                    val topPadding = if (productDiscount > 0) 2.dp else 0.dp
                    Text(
                        text = "Miễn Phí Vận Chuyển",
                        color = TealFreeship,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = topPadding)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherBottomSheet(
    availableVouchers: List<Voucher>,
    selectedDiscount: Voucher?,
    selectedFreeship: Voucher?,
    currentSubTotal: Long,
    onDismiss: () -> Unit,
    onApplyCode: (String) -> Unit,
    onSelectVoucher: (Voucher) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputCode by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val freeshipVouchers = availableVouchers.filter { it.type == "FREESHIP" }
    val discountVouchers = availableVouchers.filter { it.type == "DISCOUNT" }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgLight) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Text("Chọn Voucher", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(16.dp))

            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it.uppercase() },
                    placeholder = { Text("Nhập mã voucher...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { focusManager.clearFocus(); if (inputCode.isNotBlank()) onApplyCode(inputCode) },
                    enabled = inputCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) { Text("ÁP DỤNG", fontWeight = FontWeight.Bold) }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (freeshipVouchers.isNotEmpty()) {
                    item { Text("Miễn Phí Vận Chuyển", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp) }
                    items(freeshipVouchers) { v ->
                        VoucherTicket(
                            voucher = v,
                            isSelected = selectedFreeship?.code == v.code,
                            currentSubTotal = currentSubTotal,
                            iconBgColor = TealFreeship,
                            icon = Icons.Default.LocalShipping,
                            onSelect = { onSelectVoucher(v) }
                        )
                    }
                }

                if (discountVouchers.isNotEmpty()) {
                    item { Text("Giảm Giá Đơn Hàng", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp) }
                    items(discountVouchers) { v ->
                        VoucherTicket(
                            voucher = v,
                            isSelected = selectedDiscount?.code == v.code,
                            currentSubTotal = currentSubTotal,
                            iconBgColor = GunplaBlue,
                            icon = Icons.Default.ConfirmationNumber,
                            onSelect = { onSelectVoucher(v) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun VoucherTicket(
    voucher: Voucher,
    isSelected: Boolean = false,
    currentSubTotal: Long = 0L,
    iconBgColor: Color,
    icon: ImageVector,
    onSelect: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val progress = if (voucher.usageLimit > 0) {
        (voucher.usedCount.toFloat() / voucher.usageLimit.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percentString = (progress * 100).toInt()

    val currentTime = System.currentTimeMillis()
    val isNotStarted = voucher.startDate > currentTime
    val isDeactivated = !voucher.isActive
    val isExpired = voucher.expirationDate < currentTime && voucher.expirationDate > 0L
    val isDepleted = voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit
    val isNotEnoughValue = currentSubTotal > 0L && currentSubTotal < voucher.minOrderValue
    val canUse = !isDeactivated && !isNotStarted && !isExpired && !isDepleted && !isNotEnoughValue
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    val timeLabel = when {
        isNotStarted -> "Có hiệu lực từ: ${sdf.format(java.util.Date(voucher.startDate))}"
        isExpired -> "Đã hết hạn"
        voucher.expirationDate > 0 -> "Có hiệu lực: ${sdf.format(java.util.Date(voucher.expirationDate))}"
        else -> "Không thời hạn"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
            .alpha(if (canUse) 1f else 0.5f)
            .clickable(enabled = canUse) { onSelect() }
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(96.dp)
                    .background(if (canUse) iconBgColor else Color(0xFF9E9E9E)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (voucher.type == "FREESHIP") "FREESHIP" else "DISCOUNT",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxHeight().width(1.dp)) {
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
                verticalAlignment = Alignment.CenterVertically // Căn giữa toàn bộ theo chiều dọc
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = voucher.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF222222),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Đơn tối thiểu ₫${formatter.format(voucher.minOrderValue)}",
                        color = Color(0xFF757575),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isNotStarted) AlertRed else Color(0xFF9E9E9E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeLabel,
                            color = if (isNotStarted) AlertRed else Color(0xFF9E9E9E),
                            fontSize = 11.sp
                        )
                    }
                    if (!isDeactivated && !isExpired && !isNotStarted && voucher.usageLimit > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.8f)) {
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100.dp)),
                                color = if (progress >= 0.9f) AlertRed else iconBgColor,
                                trackColor = Color(0xFFEEEEEE)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isDepleted) "Hết mã" else "Đã dùng $percentString%", fontSize = 9.sp, color = if (isDepleted) AlertRed else Color.Gray)
                        }
                    }
                }
                if (currentSubTotal == 0L && !isSelected) {
                    OutlinedButton(
                        onClick = { if (canUse) onSelect() },
                        enabled = canUse,
                        modifier = Modifier
                            .height(32.dp)
                            .widthIn(min = 70.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = iconBgColor,
                            disabledContentColor = Color(0xFFBDBDBD)
                        ),
                        border = BorderStroke(1.dp, if (canUse) iconBgColor else Color(0xFFE0E0E0))
                    ) {
                        Text(
                            text = if (isNotStarted) "Lưu" else "Dùng",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = iconBgColor),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun BillRow(label: String, amount: Long, isTotal: Boolean = false, color: Color = Color.Black) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isTotal) Color.Black else Color.Gray, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, fontSize = if (isTotal) 16.sp else 14.sp)
        Text("${if (amount < 0) "-" else ""}₫${DecimalFormat("#,###").format(if (amount < 0) -amount else amount)}", color = if (isTotal) AlertRed else color, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium, fontSize = if (isTotal) 16.sp else 14.sp)
    }
}

@Composable
fun SimpleTextField(state: String, onValue: (String) -> Unit, label: String, icon: ImageVector) {
    OutlinedTextField(
        value = state, onValueChange = onValue, label = { Text(label, color = Color.Gray, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, tint = GunplaBlue) },
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GunplaBlue, unfocusedBorderColor = Color.LightGray),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun PaymentOptionItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onClick() },
        border = BorderStroke(1.dp, if (selected) GunplaBlue else Color.LightGray),
        color = if (!enabled) Color(0xFFF5F5F5) else if (selected) Color(0xFFE3F2FD) else Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (!enabled) Color.LightGray else if (selected) GunplaBlue else Color.Gray)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (enabled) Color.Black else Color.Gray
            )
        }
    }
}

@Composable
fun OrderSuccessDialogLight(onGoHome: () -> Unit) {
    AlertDialog(
        onDismissRequest = {}, containerColor = Color.White,
        icon = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(56.dp)) },
        title = { Text("Đặt hàng thành công!", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        text = { Text("Cảm ơn bạn đã mua sắm. Đơn hàng sẽ được thanh toán bằng tiền mặt khi giao hàng.", textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        confirmButton = { Button(onClick = onGoHome, colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue), modifier = Modifier.fillMaxWidth()) { Text("Về trang chủ", fontWeight = FontWeight.Bold) } }
    )
}
@Composable
fun TransferSuccessDialog(onGoHome: () -> Unit) {
    AlertDialog(
        onDismissRequest = {}, containerColor = Color.White,
        icon = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(56.dp)) },
        title = { Text("Thanh toán thành công!", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        text = { Text("Hệ thống đã nhận được tiền chuyển khoản. Đơn hàng của bạn đang được chuẩn bị và sẽ sớm được giao!", textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        confirmButton = { Button(onClick = onGoHome, colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue), modifier = Modifier.fillMaxWidth()) { Text("Về trang chủ", fontWeight = FontWeight.Bold) } }
    )
}
