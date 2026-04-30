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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Order
import com.example.storepromax.domain.model.VietQRBank
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    navController: NavController,
    viewModel: OrderViewModel = hiltViewModel(),
    initialTabIndex: Int = 0
) {
    val context = LocalContext.current
    val allOrders by viewModel.orders.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex) }
    val banks by viewModel.banks.collectAsState()

    var orderToCancel by remember { mutableStateOf<Order?>(null) }
    var orderToReturn by remember { mutableStateOf<Order?>(null) }
    var orderToInputTracking by remember { mutableStateOf<Order?>(null) }
    var orderToViewReceipt by remember { mutableStateOf<Order?>(null) }

    val statusCodes = listOf("ALL", "PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "RETURN_PENDING", "RETURN_APPROVED", "RETURNING", "RETURN_REJECTED", "CANCELLED", "REFUNDING", "REFUNDED")
    val tabTitles = listOf("Tất cả", "Chờ xác nhận", "Chờ lấy hàng", "Đang giao", "Hoàn thành", "Chờ xử lý Trả", "Chờ gửi hàng", "Đang hoàn hàng", "Từ chối Trả", "Đã hủy", "Chờ hoàn tiền", "Đã hoàn tiền")

    val filteredOrders = remember(allOrders, selectedTabIndex) {
        if (selectedTabIndex == 0) allOrders else allOrders.filter { it.status == statusCodes[selectedTabIndex] }
    }

    val bgLight = Color(0xFFF5F5F5)
    val primaryColor = Color(0xFF007AFF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = bgLight
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
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
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) primaryColor else Color.Gray
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color(0xFFEEEEEE),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) primaryColor else Color.Gray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
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
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Chưa có đơn hàng nào", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredOrders) { order ->
                        OrderItem(
                            order = order,
                            onCancelClick = { orderToCancel = order },
                            onReturnClick = { orderToReturn = order },
                            onInputTrackingClick = { orderToInputTracking = order },
                            onViewReceiptClick = { orderToViewReceipt = it }
                        )
                    }
                }
            }
        }
    }

    if (orderToCancel != null) {
        val isPaid = orderToCancel!!.paymentStatus == "PAID"
        UserCancelOrderDialog(
            isPaid = isPaid,
            banks = banks,
            onDismiss = { orderToCancel = null },
            onConfirm = { reason, bin, shortName, accNum, accName ->
                viewModel.cancelOrder(
                    orderId = orderToCancel!!.id,
                    reason = reason,
                    isPaid = isPaid,
                    bankBin = bin,
                    bankShortName = shortName,
                    accountNumber = accNum,
                    accountName = accName
                )
                orderToCancel = null
            }
        )
    }

    if (orderToReturn != null) {
        UserReturnOrderDialog(
            banks = banks,
            onDismiss = { orderToReturn = null },
            onConfirm = { reason, description, images, bankBin, bankShortName, accNum, accName ->
                viewModel.requestReturnRefund(
                    orderId = orderToReturn!!.id,
                    reason = reason,
                    description = description,
                    localMediaUris = images,
                    bankBin = bankBin,
                    bankShortName = bankShortName,
                    accountNumber = accNum,
                    accountName = accName
                ) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) {
                        orderToReturn = null
                    }
                }
            }
        )
    }

    if (orderToInputTracking != null) {
        UserInputTrackingDialog(
            onDismiss = { orderToInputTracking = null },
            onConfirm = { trackingCode ->
                viewModel.submitReturnTrackingCode(orderToInputTracking!!.id, trackingCode) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (success) {
                        orderToInputTracking = null
                    }
                }
            }
        )
    }

    if (orderToViewReceipt != null) {
        ReceiptImageDialog(
            order = orderToViewReceipt!!,
            onDismiss = { orderToViewReceipt = null }
        )
    }
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

@Composable
fun OrderItem(
    order: Order,
    onCancelClick: () -> Unit,
    onReturnClick: () -> Unit,
    onInputTrackingClick: () -> Unit,
    onViewReceiptClick: (Order) -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = try { dateFormat.format(order.createdAt) } catch (e: Exception) { "" }

    val (statusLabel, statusColor, statusBg) = when(order.status) {
        "PENDING" -> Triple("Chờ xác nhận", Color(0xFFE65100), Color(0xFFFFE0B2))
        "CONFIRMED" -> Triple("Chờ lấy hàng", Color(0xFF5E35B1), Color(0xFFE1BEE7))
        "SHIPPING" -> Triple("Đang giao", Color(0xFF0277BD), Color(0xFFB3E5FC))
        "COMPLETED" -> Triple("Hoàn thành", Color(0xFF2E7D32), Color(0xFFC8E6C9))
        "RETURN_PENDING" -> Triple("Chờ xử lý Trả hàng", Color(0xFFD84315), Color(0xFFFFE0B2))
        "RETURN_APPROVED" -> Triple("Chờ gửi hàng trả", Color(0xFF1565C0), Color(0xFFBBDEFB))
        "RETURNING" -> Triple("Đang hoàn hàng", Color(0xFF00838F), Color(0xFFB2EBF2))
        "RETURN_REJECTED" -> Triple("Từ chối Trả hàng", Color(0xFFC62828), Color(0xFFFFCDD2))
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
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Mã đơn: ${order.id.takeLast(8).uppercase()}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dateString, fontSize = 12.sp, color = Color.Gray)
                }
                Surface(color = statusBg, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = statusLabel, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))
            order.items.take(2).forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
                    AsyncImage(model = item.product.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.product.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("x${item.quantity}", fontSize = 13.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "₫${formatter.format(item.snapshotTotalPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (order.items.size > 2) {
                Text(text = "Xem thêm ${order.items.size - 2} sản phẩm khác...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
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
                            Text(
                                text = "$reasonTitle $reasonText",
                                color = if (order.status == "CANCELLED" || order.status == "RETURN_REJECTED") Color(0xFFD32F2F) else Color.DarkGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (order.status == "RETURN_REJECTED" && !order.cancelReason.isNullOrBlank()) { // Dùng trường cancelReason làm reject reason trên web
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Shop từ chối: ${order.cancelReason}", color = Color(0xFFC62828), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            if (!order.refundAccountNumber.isNullOrBlank()) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                Text(text = "Hoàn tiền về: ${order.refundBankShortName} - ${order.refundAccountNumber}", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                Text(text = "Chủ TK: ${order.refundAccountName}", fontSize = 12.sp, color = Color(0xFF1565C0))
                            }
                        }
                    }
                }
            }
            if (order.status == "REFUNDED" && !order.refundReceiptUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shop đã chuyển khoản", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Xem biên lai",
                            color = Color(0xFF1565C0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onViewReceiptClick(order) }.padding(4.dp)
                        )
                    }
                }
            }
            Divider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if(isPaid) Icons.Default.CheckCircle else Icons.Default.Info, contentDescription = null, tint = paymentColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(paymentText, fontSize = 12.sp, color = paymentColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Thành tiền:", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "₫${formatter.format(order.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD32F2F))
                    }
                }

                if (order.status == "PENDING") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Hủy Đơn Hàng", fontSize = 13.sp)
                        }
                    }
                }

                // 🌟 LỖ HỔNG HOÀN HÀNG ĐÃ ĐƯỢC VÁ TẠI ĐÂY
                if (order.status == "COMPLETED") {
                    val threeDaysInMillis = 3L * 24 * 60 * 60 * 1000
                    val isWithinReturnPeriod = (System.currentTimeMillis() - order.updatedAt) <= threeDaysInMillis
                    val hasReviewed = order.reviewedProducts.isNotEmpty() // Kiểm tra xem đã đánh giá chưa

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (hasReviewed) {
                            // Ưu tiên 1: Đã đánh giá -> Khóa quyền trả hàng
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color.LightGray),
                                colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = Color.Gray),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Đã đánh giá - Không thể trả hàng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (isWithinReturnPeriod) {
                            // Ưu tiên 2: Chưa đánh giá và còn hạn -> Cho phép Trả hàng
                            OutlinedButton(
                                onClick = onReturnClick,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFE65100)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Yêu cầu Trả hàng / Hoàn tiền", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Ưu tiên 3: Quá hạn 3 ngày
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color.LightGray),
                                colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = Color.Gray),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Đã hết hạn Trả hàng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (order.status == "RETURN_APPROVED") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = onInputTrackingClick,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF1565C0)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Nhập Mã Vận Đơn Trả Hàng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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

// 🌟 ĐỊA CHỈ TRẢ HÀNG ĐƯỢC GHIM CỨNG CỰC ĐẸP TẠI ĐÂY
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