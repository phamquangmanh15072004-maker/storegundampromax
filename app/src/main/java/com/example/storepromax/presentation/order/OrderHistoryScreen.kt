package com.example.storepromax.presentation.order

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.VietQRBank
import com.example.storepromax.presentation.checkout.TransferSuccessDialog
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

data class PaymentPopupData(
    val url: String,
    val bin: String,
    val accNo: String,
    val amount: Long,
    val description: String,
    val orderId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    navController: NavController,
    viewModel: OrderViewModel = hiltViewModel(),
    initialTabIndex: Int = 0
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val allOrders by viewModel.orders.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val processingOrderId by viewModel.processingOrderId.collectAsState()
    val timeAllowedMillis = 5 * 60 * 1000L

    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex) }
    var orderToCancel by remember { mutableStateOf<Order?>(null) }
    var orderToReturn by remember { mutableStateOf<Order?>(null) }
    var orderToInputTracking by remember { mutableStateOf<Order?>(null) }
    var orderToViewReceipt by remember { mutableStateOf<Order?>(null) }

    var paymentPopupData by remember { mutableStateOf<PaymentPopupData?>(null) }
    var showTransferSuccessDialog by remember { mutableStateOf(false) }

    val statusCodes = listOf("ALL", "AWAITING_PAYMENT", "PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "RETURN_PENDING", "RETURN_APPROVED", "RETURNING", "RETURN_REJECTED", "CANCELLED", "REFUNDING", "REFUNDED")
    val tabTitles = listOf("Tất cả", "Chờ thanh toán", "Chờ xác nhận", "Chờ lấy hàng", "Đang giao", "Hoàn thành", "Chờ xử lý Trả", "Chờ gửi hàng", "Đang hoàn hàng", "Từ chối Trả", "Đã hủy", "Chờ hoàn tiền", "Đã hoàn tiền")

    val filteredOrders = remember(allOrders, selectedTabIndex) {
        if (selectedTabIndex == 0) allOrders else allOrders.filter { it.status == statusCodes[selectedTabIndex] }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            if (message == "PAYMENT_SUCCESS") {
                paymentPopupData = null
                showTransferSuccessDialog = true
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val primaryColor = Color(0xFF0D47A1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = primaryColor) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF2F4F7)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = primaryColor,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = primaryColor)
                    }
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val count = if (index == 0) allOrders.size else allOrders.count { it.status == statusCodes[index] }
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = title, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) primaryColor else Color.Gray)
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(color = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color(0xFFEEEEEE), shape = RoundedCornerShape(12.dp)) {
                                        Text(text = count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) primaryColor else Color.Gray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Text("Chưa có đơn hàng nào", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(filteredOrders, key = { it.id }) { order ->
                        val isPaymentExpired = order.status == "AWAITING_PAYMENT" && (System.currentTimeMillis() - normalizeTimestampMillis(order.createdAt)) > timeAllowedMillis
                        OrderItem(
                            order = order,
                            isThisOrderProcessing = processingOrderId == order.id,
                            isAnyProcessing = processingOrderId != null,
                            isPaymentExpired = isPaymentExpired,
                            onCancelClick = { orderToCancel = order },
                            onReturnClick = { orderToReturn = order },
                            onInputTrackingClick = { orderToInputTracking = order },
                            onViewReceiptClick = { orderToViewReceipt = it },
                            onPayAgainClick = { clickedOrder ->
                                viewModel.getPaymentDetails(clickedOrder) { success, bin, accNo, url, desc ->
                                    if (success) {
                                        paymentPopupData = PaymentPopupData(
                                            url = url,
                                            bin = bin,
                                            accNo = accNo,
                                            amount = clickedOrder.totalPrice,
                                            description = desc,
                                            orderId = clickedOrder.id
                                        )
                                    } else {
                                        Toast.makeText(context, "Lỗi kết nối thanh toán!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (paymentPopupData != null) {
        val data = paymentPopupData!!
        val currentOrder = allOrders.find { it.id == data.orderId }
        val createdAtMillis = currentOrder?.createdAt?.let { normalizeTimestampMillis(it) } ?: System.currentTimeMillis()
        val timeElapsed = System.currentTimeMillis() - createdAtMillis
        val timeLeft = (timeAllowedMillis - timeElapsed).coerceAtLeast(0L)

        if (timeLeft <= 0) {
            LaunchedEffect(data.orderId) {
                Toast.makeText(context, "Đơn hàng đã quá hạn. Hệ thống đang tự động hủy!", Toast.LENGTH_LONG).show()
                paymentPopupData = null
            }
        } else {
            RepayQRDialog(
                data = data,
                initialTimeLeftMillis = timeLeft,
                onDismiss = { paymentPopupData = null },
                onCancelOrder = {
                    viewModel.cancelOrder(data.orderId, "Khách đổi ý không muốn thanh toán", false)
                    paymentPopupData = null
                },
                onOpenWeb = {
                    if (data.url.isNotBlank() && data.url != "null") {
                        try { uriHandler.openUri(data.url) } catch (e: Exception) { Toast.makeText(context, "Không thể mở ứng dụng", Toast.LENGTH_SHORT).show() }
                    } else {
                        Toast.makeText(context, "Không có link thanh toán Web!", Toast.LENGTH_SHORT).show()
                    }
                },
                onAutoCancelWhenTimeout = {
                    Toast.makeText(context, "Mã QR đã hết hạn. Hệ thống đang tự động hủy!", Toast.LENGTH_LONG).show()
                    paymentPopupData = null
                }
            )
        }
    }

    if (orderToCancel != null) {
        UserCancelOrderDialog(isPaid = orderToCancel!!.paymentStatus == "PAID", banks = banks, onDismiss = { orderToCancel = null }, onConfirm = { r, b, s, num, n -> viewModel.cancelOrder(orderToCancel!!.id, r, orderToCancel!!.paymentStatus == "PAID", b, s, num, n); orderToCancel = null })
    }
    if (orderToReturn != null) {
        UserReturnOrderDialog(banks = banks, onDismiss = { orderToReturn = null }, onConfirm = { r, d, i, b, s, num, n -> viewModel.requestReturnRefund(orderToReturn!!.id, r, d, i, b, s, num, n) { success, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); if (success) orderToReturn = null } })
    }
    if (orderToInputTracking != null) {
        UserInputTrackingDialog(onDismiss = { orderToInputTracking = null }, onConfirm = { viewModel.submitReturnTrackingCode(orderToInputTracking!!.id, it) { s, m -> Toast.makeText(context, m, Toast.LENGTH_SHORT).show(); if (s) orderToInputTracking = null } })
    }
    if (orderToViewReceipt != null) {
        ReceiptImageDialog(order = orderToViewReceipt!!, onDismiss = { orderToViewReceipt = null })
    }

    if (showTransferSuccessDialog) {
        TransferSuccessDialog(
            onGoHome = {
                showTransferSuccessDialog = false
                selectedTabIndex = 2
            }
        )
    }
}

@Composable
fun OrderItem(
    order: Order,
    isThisOrderProcessing: Boolean,
    isAnyProcessing: Boolean,
    isPaymentExpired: Boolean,
    onCancelClick: () -> Unit,
    onReturnClick: () -> Unit,
    onInputTrackingClick: () -> Unit,
    onViewReceiptClick: (Order) -> Unit,
    onPayAgainClick: (Order) -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = try { dateFormat.format(order.createdAt) } catch (e: Exception) { "" }

    val (statusLabel, statusColor, statusBg) = when(order.status) {
        "AWAITING_PAYMENT" -> Triple("Chờ thanh toán", Color(0xFFC62828), Color(0xFFFFCDD2))
        "PENDING" -> Triple("Chờ xác nhận", Color(0xFFE65100), Color(0xFFFFE0B2))
        "CONFIRMED" -> Triple("Chờ lấy hàng", Color(0xFF5E35B1), Color(0xFFE1BEE7))
        "SHIPPING" -> Triple("Đang giao", Color(0xFF0277BD), Color(0xFFB3E5FC))
        "COMPLETED" -> Triple("Hoàn thành", Color(0xFF2E7D32), Color(0xFFC8E6C9))
        "RETURN_PENDING" -> Triple("Chờ xử lý Trả", Color(0xFFD84315), Color(0xFFFFE0B2))
        "RETURN_APPROVED" -> Triple("Chờ gửi hàng trả", Color(0xFF1565C0), Color(0xFFBBDEFB))
        "RETURNING" -> Triple("Đang hoàn hàng", Color(0xFF00838F), Color(0xFFB2EBF2))
        "RETURN_REJECTED" -> Triple("Từ chối Trả", Color(0xFFC62828), Color(0xFFFFCDD2))
        "CANCELLED" -> Triple("Đã hủy", Color(0xFFC62828), Color(0xFFFFCDD2))
        "REFUNDING" -> Triple("Chờ hoàn tiền", Color(0xFFE65100), Color(0xFFFFE0B2))
        "REFUNDED" -> Triple("Đã hoàn tiền", Color(0xFF2E7D32), Color(0xFFC8E6C9))
        else -> Triple("Không rõ", Color.Gray, Color(0xFFEEEEEE))
    }

    val isPaid = order.paymentStatus == "PAID"
    val paymentText = if (isPaid) "Đã thanh toán" else "Chưa thanh toán"
    val paymentColor = if (isPaid) Color(0xFF2E7D32) else Color(0xFFE65100)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Mã đơn: #${order.id.takeLast(8).uppercase()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dateString, fontSize = 12.sp, color = Color.Gray)
                }
                Surface(color = statusBg, shape = RoundedCornerShape(8.dp)) {
                    Text(text = statusLabel, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))

            order.items.take(2).forEach { item ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
                    AsyncImage(model = item.product.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.product.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("x${item.quantity}", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "₫${formatter.format(item.snapshotTotalPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            if (order.items.size > 2) {
                Text(text = "Xem thêm ${order.items.size - 2} sản phẩm khác...", fontSize = 13.sp, color = Color(0xFF0D47A1), modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium)
            }

            if ((order.status == "CANCELLED" || order.status == "REFUNDING" || order.status == "REFUNDED" || order.status == "RETURN_PENDING" || order.status == "RETURN_REJECTED")) {
                val reasonTitle = when(order.status) {
                    "CANCELLED" -> "Lý do hủy:"
                    "RETURN_PENDING", "RETURN_REJECTED" -> "Lý do khiếu nại:"
                    else -> "Lý do hoàn tiền:"
                }
                val reasonText = if (order.status.startsWith("RETURN")) order.returnReason else order.cancelReason

                if (!reasonText.isNullOrBlank()) {
                    Surface(
                        color = if (order.status == "CANCELLED" || order.status == "RETURN_REJECTED") Color(0xFFFFF0F0) else Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "$reasonTitle $reasonText", color = if (order.status == "CANCELLED" || order.status == "RETURN_REJECTED") Color(0xFFD32F2F) else Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (order.status == "RETURN_REJECTED" && !order.cancelReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Shop từ chối: ${order.cancelReason}", color = Color(0xFFC62828), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (!order.refundAccountNumber.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                Text(text = "Hoàn tiền về: ${order.refundBankShortName} - ${order.refundAccountNumber}", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                Text(text = "Chủ TK: ${order.refundAccountName}", fontSize = 12.sp, color = Color(0xFF1565C0))
                            }
                        }
                    }
                }
            }
            if (order.status == "REFUNDED" && !order.refundReceiptUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shop đã chuyển khoản", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Xem biên lai", color = Color(0xFF1565C0), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onViewReceiptClick(order) }.padding(4.dp))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if(isPaid) Icons.Default.CheckCircle else Icons.Default.Info, contentDescription = null, tint = paymentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(paymentText, fontSize = 12.sp, color = paymentColor, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Tổng thanh toán:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "₫${formatter.format(order.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD32F2F))
                    }
                }

                // CÁC NÚT ACTION TƯƠNG ỨNG VỚI TRẠNG THÁI
                if (order.status == "AWAITING_PAYMENT") {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isPaymentExpired) {
                        Text(
                            text = "Đã quá hạn thanh toán",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { onPayAgainClick(order) },
                                enabled = !isAnyProcessing,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                modifier = Modifier.height(40.dp)
                            ) {
                                if (isThisOrderProcessing) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text("Thanh toán ngay", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (order.status == "PENDING") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Hủy Đơn Hàng", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 🌟 ĐÃ KHÔI PHỤC NÚT ĐÁNH GIÁ/YÊU CẦU TRẢ HÀNG 3 NGÀY KHI ĐƠN COMPLETED
                if (order.status == "COMPLETED") {
                    val threeDaysInMillis = 3L * 24 * 60 * 60 * 1000
                    val isWithinReturnPeriod = (System.currentTimeMillis() - order.updatedAt) <= threeDaysInMillis
                    val hasReviewed = order.reviewedProducts.isNotEmpty()

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (hasReviewed) {
                            OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.LightGray), colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = Color.Gray), modifier = Modifier.height(40.dp)) {
                                Text("Đã đánh giá", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (isWithinReturnPeriod) {
                            OutlinedButton(onClick = onReturnClick, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFE65100)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)), modifier = Modifier.height(40.dp)) {
                                Text("Yêu cầu Trả hàng / Hoàn tiền", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(onClick = { }, enabled = false, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.LightGray), colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = Color.Gray), modifier = Modifier.height(40.dp)) {
                                Text("Hết hạn Trả hàng", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (order.status == "RETURN_APPROVED") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onInputTrackingClick, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)), modifier = Modifier.height(40.dp)) {
                            Text("Nhập Mã Vận Đơn Trả Hàng", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepayQRDialog(
    data: PaymentPopupData,
    initialTimeLeftMillis: Long,
    onDismiss: () -> Unit,
    onCancelOrder: () -> Unit,
    onOpenWeb: () -> Unit,
    onAutoCancelWhenTimeout: () -> Unit
) {
    val context = LocalContext.current
    var timeLeftSeconds by remember { mutableLongStateOf(initialTimeLeftMillis / 1000) }

    LaunchedEffect(Unit) {
        while (timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds--
        }
        onAutoCancelWhenTimeout()
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val isQRAvailable = data.bin.isNotBlank() && data.accNo.isNotBlank()
    val qrUrl = if (isQRAvailable) {
        val description = URLEncoder.encode(data.description, "UTF-8")
        val accountName = URLEncoder.encode("Gunpla Store", "UTF-8")
        "https://img.vietqr.io/image/${data.bin}-${data.accNo}-compact2.png?amount=${data.amount}&addInfo=$description&accountName=$accountName"
    } else ""

    AlertDialog(
        onDismissRequest = {},
        containerColor = Color.White,
        title = { Text("Thanh toán đơn hàng", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = Color(0xFF0D47A1)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = Color(0xFFFFF0F0), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text("Đơn hàng sẽ được lưu cho đến khi hết thời gian hiệu lực!!!", color = Color(0xFFD32F2F), fontSize = 12.sp, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                }

                Text("Thời gian tồn tại mã:", fontSize = 14.sp, color = Color.Gray)
                Text(timeString, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Red)

                Spacer(Modifier.height(12.dp))

                if (isQRAvailable) {
                    Text("Phương thức 1: Quét mã QR để thanh toán:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                        AsyncImage(model = qrUrl, contentDescription = "QR Code", modifier = Modifier.size(200.dp).padding(8.dp))
                    }
                } else {
                    Surface(color = Color(0xFFFFF0F0), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
                        Text("Mã QR tạm thời không khả dụng. Vui lòng bấm nút Mở App Ngân Hàng bên dưới!", color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                    }
                }

                Text("Số tiền: ₫${DecimalFormat("#,###").format(data.amount)}", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 22.sp)

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(16.dp))

                Text("Phương thức 2: Chuyển đến Ngân Hàng:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))

                Spacer(Modifier.height(12.dp))
                Button(onClick = onOpenWeb, colors = ButtonDefaults.buttonColors(Color(0xFF0D47A1)), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mở App Ngân Hàng Trên Máy", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = { Toast.makeText(context, "Đã ghi nhận! Đơn hàng sẽ được duyệt khi tiền vào tài khoản.", Toast.LENGTH_LONG).show(); onDismiss() }, colors = ButtonDefaults.buttonColors(Color(0xFF00C853)), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tôi đã chuyển khoản xong", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelOrder, modifier = Modifier.fillMaxWidth()) { Text("Đổi ý / Hủy đơn hàng này", color = Color.Red, fontWeight = FontWeight.Bold) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReturnOrderDialog(
    banks: List<VietQRBank>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>, String?, String?, String?, String?) -> Unit
) {
    val reasons = listOf("Hàng bị lỗi/vỡ", "Giao sai sản phẩm", "Thiếu hàng", "Hàng giả/nhái", "Khác")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var description by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    var selectedBank by remember { mutableStateOf<VietQRBank?>(null) }
    var accNum by remember { mutableStateOf("") }
    var accName by remember { mutableStateOf("") }
    var showBankSheet by remember { mutableStateOf(false) }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = (selectedImages + uris.map { it.toString() }).take(3)
        }
    }
    val canConfirm = selectedImages.isNotEmpty() && selectedBank != null && accNum.isNotBlank() && accName.isNotBlank() && !isUploading

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        containerColor = Color.White,
        title = { Text("Yêu cầu Trả hàng / Hoàn tiền", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Lưu ý: Vui lòng cung cấp hình ảnh rõ nét và thông tin nhận hoàn tiền chính xác để Shop duyệt nhanh nhất.",
                        color = Color(0xFFE65100),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Text("Lý do trả hàng:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                reasons.forEach { text ->
                    Row(
                        Modifier.fillMaxWidth().selectable(
                            selected = (text == selectedReason),
                            onClick = { selectedReason = text }
                        ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (text == selectedReason), onClick = { selectedReason = text }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF)))
                        Text(text, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả chi tiết lỗi") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Hình ảnh bằng chứng (Tối đa 3):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedImages.size < 3) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .clickable {
                                    multiplePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Thêm ảnh", tint = Color(0xFF007AFF))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedImages) { imageUrl ->
                            Box(modifier = Modifier.size(64.dp)) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .clickable { selectedImages = selectedImages.filter { it != imageUrl } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Thông tin nhận hoàn tiền:", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedCard(
                    onClick = { showBankSheet = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selectedBank != null) {
                            AsyncImage(model = selectedBank!!.logo, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Fit)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedBank!!.shortName, fontWeight = FontWeight.Medium, color = Color.Black)
                        } else {
                            Text("Chạm để chọn Ngân hàng...", color = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = accNum, onValueChange = { accNum = it }, label = { Text("Số tài khoản nhận tiền") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = accName, onValueChange = { accName = it.uppercase() }, label = { Text("Tên in trên thẻ (Tự viết hoa)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isUploading = true
                    onConfirm(selectedReason, description, selectedImages, selectedBank?.bin, selectedBank?.shortName, accNum, accName)
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text(if (isUploading) "Đang xử lý..." else "Gửi Yêu Cầu")
            }
        },
        dismissButton = {
            if (!isUploading) {
                TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
            }
        }
    )

    if (showBankSheet) {
        ModalBottomSheet(onDismissRequest = { showBankSheet = false }, containerColor = Color.White) {
            BankSearchContent(
                banks = banks,
                onBankSelected = {
                    selectedBank = it
                    showBankSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankSearchContent(banks: List<VietQRBank>, onBankSelected: (VietQRBank) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredBanks = remember(searchQuery, banks) {
        if (searchQuery.isBlank()) banks else banks.filter { it.shortName.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp)) {
        Text("Chọn Ngân hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Tìm kiếm ngân hàng...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredBanks) { bank ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onBankSelected(bank) }.padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = bank.logo,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(bank.shortName, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Divider(color = Color(0xFFF5F5F5))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCancelOrderDialog(
    isPaid: Boolean,
    banks: List<VietQRBank>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, String?, String?) -> Unit
) {
    val reasons = listOf("Thay đổi địa chỉ", "Thêm/bớt sản phẩm", "Quên áp mã", "Đổi ý", "Lý do khác")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var customReason by remember { mutableStateOf("") }

    var selectedBank by remember { mutableStateOf<VietQRBank?>(null) }
    var accNum by remember { mutableStateOf("") }
    var accName by remember { mutableStateOf("") }

    var showBankSheet by remember { mutableStateOf(false) }

    val canConfirm = if (isPaid) {
        selectedBank != null && accNum.isNotBlank() && accName.isNotBlank()
    } else true

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Xác nhận hủy đơn", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Lý do hủy đơn:", fontSize = 14.sp, color = Color.Gray)
                reasons.forEach { text ->
                    Row(
                        Modifier.fillMaxWidth().selectable(
                            selected = (text == selectedReason),
                            onClick = { selectedReason = text }
                        ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (text == selectedReason), onClick = { selectedReason = text }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF)))
                        Text(text, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (isPaid) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Thông tin nhận hoàn tiền:", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 14.sp)
                    Surface(
                        color = Color(0xFFFFF0F0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "⚠️ Lưu ý: Shop sẽ hoàn tiền ĐÚNG vào STK bạn cung cấp dưới đây. Shop không chịu trách nhiệm nếu bạn nhập sai thông tin.",
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedCard(
                        onClick = { showBankSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (selectedBank != null) {
                                AsyncImage(model = selectedBank!!.logo, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Fit)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedBank!!.shortName, fontWeight = FontWeight.Medium, color = Color.Black)
                            } else {
                                Text("Chạm để chọn Ngân hàng...", color = Color.Gray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = accNum, onValueChange = { accNum = it }, label = { Text("Số tài khoản nhận tiền") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = accName, onValueChange = { accName = it.uppercase() }, label = { Text("Tên in trên thẻ (Tự viết hoa)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val res = if (selectedReason == "Lý do khác") customReason.ifBlank { "Lý do khác" } else selectedReason
                    onConfirm(res, selectedBank?.bin, selectedBank?.shortName, accNum, accName)
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Đồng ý Hủy") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bỏ qua", color = Color.Gray) } }
    )
    if (showBankSheet) {
        ModalBottomSheet(onDismissRequest = { showBankSheet = false }, containerColor = Color.White) {
            BankSearchContent(
                banks = banks,
                onBankSelected = {
                    selectedBank = it
                    showBankSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInputTrackingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var trackingCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Mã vận đơn hoàn hàng", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Vui lòng ra bưu cục gửi trả hàng về địa chỉ dưới đây, sau đó nhập Mã Vận Đơn in trên phiếu gửi vào ô trống để Shop theo dõi.",
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Surface(
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("THÔNG TIN NHẬN HÀNG (SHOP):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Người nhận: Gunpla Hub Store", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Điện thoại: 0886.387.505", fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Địa chỉ: Số 89 ngõ 25, đường Phú Minh, Văn Trì, Tây Tựu, Hà Nội", fontSize = 13.sp, color = Color(0xFF475569), lineHeight = 18.sp)
                    }
                }

                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = { trackingCode = it.uppercase() },
                    label = { Text("Nhập mã vận đơn (VD: VN1234567)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trackingCode.trim()) },
                enabled = trackingCode.trim().length >= 5,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("Xác nhận đã gửi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color.Gray) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptImageDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Biên lai hoàn tiền", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Đã chuyển khoản đến:", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${order.refundBankShortName ?: order.refundBankBin} - ${order.refundAccountNumber}", fontSize = 14.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                        Text(text = "Chủ TK: ${order.refundAccountName}", fontSize = 13.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = order.refundReceiptUrl,
                        contentDescription = "Biên lai hoàn tiền",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
            ) {
                Text("Đóng")
            }
        }
    )
}

private fun normalizeTimestampMillis(raw: Long): Long {
    return if (raw in 1..9_999_999_999L) raw * 1000L else raw
}