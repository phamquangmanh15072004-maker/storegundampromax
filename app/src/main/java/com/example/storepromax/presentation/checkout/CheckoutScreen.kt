package com.example.storepromax.presentation.checkout

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.domain.model.ProvinceGHN
import com.example.storepromax.domain.model.DistrictGHN
import com.example.storepromax.domain.model.WardGHN
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
        if (productId != null && quantity != null) viewModel.loadSingleProductForCheckout(productId, quantity)
        else viewModel.loadSelectedCartItems()
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
                            Text("₫${DecimalFormat("#,###").format(item.totalPrice)}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
                    PaymentOptionItem("Chuyển khoản an toàn qua PayOS", Icons.Default.QrCode, paymentMethod == "BANKING") { viewModel.onPaymentMethodChange("BANKING") }
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
    onOpenWeb: (String) -> Unit
) {
    val context = LocalContext.current

    var timeLeft by remember { mutableIntStateOf(5 * 60) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        Toast.makeText(context, "Đã hết thời gian thanh toán!", Toast.LENGTH_SHORT).show()
        onCancelOrder()
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
                Text("Mã QR sẽ hết hạn sau:", fontSize = 14.sp, color = Color.Gray)
                Text(timeString, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AlertRed)

                Spacer(Modifier.height(8.dp))

                Text("Cách 1: Quét mã bằng máy khác", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GunplaBlue)
                Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                    AsyncImage(model = qrUrl, contentDescription = "QR Code", modifier = Modifier.size(200.dp).padding(8.dp))
                }
                Text("Số tiền: ₫${DecimalFormat("#,###").format(data.amount)}", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(16.dp))

                Text("Cách 2: Thanh toán trên máy này", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GunplaBlue)
                Text("(Tự động mở App ngân hàng của bạn)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onOpenWeb(data.url) },
                    colors = ButtonDefaults.buttonColors(GunplaBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mở App Ngân Hàng", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Đã ghi nhận. Đơn hàng sẽ cập nhật khi tiền vào tài khoản!", Toast.LENGTH_LONG).show()
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
    availableVouchers: List<Voucher>, selectedDiscount: Voucher?, selectedFreeship: Voucher?,
    currentSubTotal: Long, onDismiss: () -> Unit, onApplyCode: (String) -> Unit, onSelectVoucher: (Voucher) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputCode by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val freeshipVouchers = availableVouchers.filter { it.type == "FREESHIP" }
    val discountVouchers = availableVouchers.filter { it.type == "DISCOUNT" }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgLight) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Text("Chọn Voucher", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputCode, onValueChange = { inputCode = it.uppercase() }, placeholder = { Text("Nhập mã voucher...") },
                    singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { focusManager.clearFocus(); if (inputCode.isNotBlank()) onApplyCode(inputCode) },
                    enabled = inputCode.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.height(56.dp)
                ) { Text("ÁP DỤNG", fontWeight = FontWeight.Bold) }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (freeshipVouchers.isNotEmpty()) {
                    item { Text("Miễn Phí Vận Chuyển", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp) }
                    items(freeshipVouchers) { v ->
                        val isEligible = currentSubTotal >= v.minOrderValue && v.usedCount < v.usageLimit
                        val isSelected = selectedFreeship?.code == v.code
                        VoucherTicket(v, isSelected, isEligible, currentSubTotal, TealFreeship, Icons.Default.LocalShipping) { onSelectVoucher(v) }
                    }
                }
                if (discountVouchers.isNotEmpty()) {
                    item { Text("Giảm Giá Đơn Hàng", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp) }
                    items(discountVouchers) { v ->
                        val isEligible = currentSubTotal >= v.minOrderValue && v.usedCount < v.usageLimit
                        val isSelected = selectedDiscount?.code == v.code
                        VoucherTicket(v, isSelected, isEligible, currentSubTotal, GunplaBlue, Icons.Default.ConfirmationNumber) { onSelectVoucher(v) }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun VoucherTicket(voucher: Voucher, isSelected: Boolean, isEligible: Boolean, currentSubTotal: Long, iconBgColor: Color, icon: ImageVector, onSelect: () -> Unit) {
    val formatter = DecimalFormat("#,###")
    val progress = if (voucher.usageLimit > 0) (voucher.usedCount.toFloat() / voucher.usageLimit.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isEligible) 1f else 0.5f).clickable(enabled = isEligible) { onSelect() }.padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(90.dp).background(if (isEligible) iconBgColor else Color.Gray), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (voucher.type == "FREESHIP") "FREESHIP" else "DISCOUNT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(voucher.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (isEligible) Text("Đơn tối thiểu ₫${formatter.format(voucher.minOrderValue)}", color = Color.Gray, fontSize = 11.sp)
                else Text(if (voucher.usedCount >= voucher.usageLimit) "Đã hết lượt sử dụng" else "Mua thêm ₫${formatter.format(voucher.minOrderValue - currentSubTotal)} để áp dụng", color = AlertRed, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(progress = progress, modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (progress >= 0.9f) AlertRed else iconBgColor, trackColor = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đã dùng ${(progress * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = iconBgColor), modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp))
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
fun PaymentOptionItem(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable { onClick() },
        border = BorderStroke(1.dp, if (selected) GunplaBlue else Color.LightGray),
        color = if (selected) Color(0xFFE3F2FD) else Color.White, shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) GunplaBlue else Color.Gray)
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
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