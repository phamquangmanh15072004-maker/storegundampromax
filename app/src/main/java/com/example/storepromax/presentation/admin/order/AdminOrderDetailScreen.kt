package com.example.storepromax.presentation.admin.order

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Order
import com.example.storepromax.feature.product_detail.components.SwipeToConfirmButton
import java.net.URLEncoder
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDetailScreen(
    navController: NavController,
    orderId: String,
    viewModel: AdminOrderDetailViewModel = hiltViewModel()
) {
    val order by viewModel.order.collectAsState()
    val isUploading by viewModel.isUploadingReceipt.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showCancelDialog by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.confirmRefundWithReceipt(uri)
            }
        }
    )

    val bgLight = Color(0xFFF5F5F5)

    if (order == null) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        return
    }

    val currentOrder = order!!
    val formatter = DecimalFormat("#,###")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chi tiết đơn hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "ID: ${currentOrder.id.uppercase()}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
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
            BottomActionBar(
                status = currentOrder.status,
                onUpdateStatus = { newStatus -> viewModel.updateStatus(newStatus) },
                onCancelClick = { showCancelDialog = true },
                onUploadReceipt = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if ((currentOrder.status == "REFUNDING" || currentOrder.status == "REFUNDED")
                    && currentOrder.refundAccountNumber != null
                ) {
                    RefundSectionCard(currentOrder, formatter, clipboardManager, context)
                }
                if (currentOrder.status == "REFUNDED" && !currentOrder.refundReceiptUrl.isNullOrBlank()) {
                    SectionCard(title = "Biên lai hoàn tiền", icon = Icons.Default.Receipt) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Đã chuyển khoản thành công",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            AsyncImage(
                                model = currentOrder.refundReceiptUrl,
                                contentDescription = "Biên lai chuyển khoản",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                SectionCard(title = "Thông tin giao hàng", icon = Icons.Default.LocalShipping) {
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Người nhận",
                        value = currentOrder.receiverName
                    )
                    Divider(color = Color(0xFFEEEEEE))
                    InfoRow(
                        icon = Icons.Default.Phone,
                        label = "Điện thoại",
                        value = currentOrder.receiverPhone,
                        isLink = true,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${currentOrder.receiverPhone}")
                            })
                        })
                    Divider(color = Color(0xFFEEEEEE))
                    InfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Địa chỉ",
                        value = currentOrder.address,
                        isCopyable = true,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentOrder.address))
                            Toast.makeText(context, "Đã copy địa chỉ!", Toast.LENGTH_SHORT).show()
                        })
                }
                SectionCard(
                    title = "Danh sách sản phẩm (${currentOrder.items.size})",
                    icon = Icons.Default.ShoppingCart
                ) {
                    currentOrder.items.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.product.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color.Gray, RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.product.name,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                                Text("x${item.quantity}", color = Color.Gray, fontSize = 12.sp)
                            }
                            Text(
                                "₫${formatter.format(item.totalPrice)}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (item != currentOrder.items.last()) Divider(color = Color(0xFFEEEEEE))
                    }
                }
                SectionCard(title = "Thanh toán", icon = Icons.Default.Payments) {
                    val isPaid = currentOrder.paymentStatus == "PAID"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Phương thức:", modifier = Modifier.weight(1f), color = Color.Gray)
                        Text(
                            if (currentOrder.paymentMethod == "BANKING") "Chuyển khoản (QR)" else "COD (Tiền mặt)",
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trạng thái:", modifier = Modifier.weight(1f), color = Color.Gray)
                        StatusBadge(status = currentOrder.status)
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFFEEEEEE)
                    )
                    Row {
                        Text(
                            "Tổng cộng:",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "₫${formatter.format(currentOrder.totalPrice)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            fontSize = 18.sp
                        )
                    }
                }
                if ((currentOrder.status == "CANCELLED" || currentOrder.status == "REFUNDING" || currentOrder.status == "REFUNDED") && !currentOrder.cancelReason.isNullOrBlank()) {
                    Surface(
                        color = if (currentOrder.status == "CANCELLED") Color(0xFFFFF0F0) else Color(
                            0xFFFFF3E0
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (currentOrder.status == "CANCELLED") Color(0xFFD32F2F) else Color(
                                        0xFFF57C00
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (currentOrder.status == "CANCELLED") "Thông tin hủy đơn" else "Lý do hủy & Hoàn tiền",
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentOrder.status == "CANCELLED") Color(0xFFD32F2F) else Color(
                                        0xFFF57C00
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${currentOrder.cancelReason}",
                                color = Color.DarkGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF007AFF))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Đang tải biên lai lên...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        CancelOrderDetailDialog(
            isPaid = currentOrder.paymentStatus == "PAID",
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason ->
                viewModel.cancelOrder(reason)
                showCancelDialog = false
            }
        )
    }
}

@Composable
fun RefundSectionCard(
    order: Order,
    formatter: DecimalFormat,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: Context
) {
    val bin = order.refundBankBin ?: ""
    val stk = order.refundAccountNumber ?: ""
    val accountName = order.refundAccountName ?: ""
    val amount = order.totalPrice
    val description = "Hoan tien don ${order.id}"

    val encodedName = URLEncoder.encode(accountName, "UTF-8").replace("+", "%20")
    val encodedDesc = URLEncoder.encode(description, "UTF-8").replace("+", "%20")
    val qrUrl =
        "https://img.vietqr.io/image/$bin-$stk-compact2.jpg?amount=$amount&addInfo=$encodedDesc&accountName=$encodedName"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "YÊU CẦU HOÀN TIỀN KHÁCH HÀNG",
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF57C00),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(200.dp)
            ) {
                AsyncImage(
                    model = qrUrl,
                    contentDescription = "QR Hoàn Tiền",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Quét mã trên bằng App Ngân Hàng", fontSize = 12.sp, color = Color.Gray)
            Text("hoặc copy thông tin bên dưới:", fontSize = 12.sp, color = Color.Gray)

            Divider(color = Color(0xFFFFCC80), modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    clipboardManager.setText(AnnotatedString(stk))
                    Toast.makeText(context, "Đã copy Số Tài Khoản!", Toast.LENGTH_SHORT).show()
                }, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ngân hàng: ${order.refundBankShortName}", fontSize = 14.sp)
                    Text(
                        "Số tài khoản: $stk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text("Chủ TK: $accountName", fontSize = 14.sp)
                    Text(
                        "Số tiền: ₫${formatter.format(amount)}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFF57C00))
            }
        }
    }
}

@Composable
fun BottomActionBar(
    status: String,
    onUpdateStatus: (String) -> Unit,
    onCancelClick: () -> Unit,
    onUploadReceipt: () -> Unit
) {
    Surface(shadowElevation = 16.dp, color = Color.White) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            when (status) {
                "PENDING" -> {
                    SwipeToConfirmButton(
                        text = "Trượt để Xác nhận đơn >>",
                        onConfirm = { onUpdateStatus("CONFIRMED") },
                        backgroundColor = Color(0xFFE3F2FD),
                        thumbColor = Color(0xFF007AFF),
                        textColor = Color(0xFF007AFF)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Hủy đơn hàng", color = Color.Red, fontWeight = FontWeight.Medium)
                    }
                }

                "CONFIRMED" -> {
                    SwipeToConfirmButton(
                        text = "Trượt để Giao GHN >>",
                        onConfirm = { onUpdateStatus("SHIPPING") },
                        backgroundColor = Color(0xFFE0F7FA),
                        thumbColor = Color(0xFF0097A7),
                        textColor = Color(0xFF0097A7)
                    )
                }

                "SHIPPING" -> {
                    SwipeToConfirmButton(
                        text = "Trượt để Hoàn Thành >>",
                        onConfirm = { onUpdateStatus("DELIVERED") },
                        backgroundColor = Color(0xFFE8F5E9),
                        thumbColor = Color(0xFF2E7D32),
                        textColor = Color(0xFF2E7D32)
                    )
                }

                "REFUNDING" -> {
                    ActionButton(
                        text = "TẢI LÊN BIÊN LAI HOÀN TIỀN",
                        color = Color(0xFFF57C00),
                        modifier = Modifier.fillMaxWidth()
                    ) { onUploadReceipt() }
                }

                "DELIVERED" -> Text(
                    "Đơn hàng đã hoàn tất",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                "CANCELLED" -> Text(
                    "Đơn hàng đã bị hủy",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                "REFUNDED" -> Text(
                    "Đã hoàn tiền cho khách thành công",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CancelOrderDetailDialog(isPaid: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val reasons = listOf(
        "Hết hàng / Lỗi kho",
        "Sản phẩm hư hỏng",
        "Khu vực không hỗ trợ",
        "Nghi ngờ gian lận",
        "Khách hàng yêu cầu hủy",
        "Khác"
    )
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Lý do hủy đơn", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column {
                if (isPaid) {
                    Surface(
                        color = Color(0xFFFFF0F0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            "⚠️ Đơn này ĐÃ THANH TOÁN. Nếu hủy, đơn sẽ chuyển sang trạng thái chờ Hoàn Tiền.",
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Text(
                        "Vui lòng chọn lý do hủy đơn. Thông báo sẽ được gửi đến khách hàng.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                reasons.forEach { text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (text == selectedReason),
                                onClick = { selectedReason = text }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (text == selectedReason),
                            onClick = { selectedReason = text },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF))
                        )
                        Text(
                            text = text,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (selectedReason == "Khác") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        placeholder = { Text("Nhập lý do hủy...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason =
                        if (selectedReason == "Khác") customReason.ifBlank { "Lý do khác" } else selectedReason
                    onConfirm(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Hủy Đơn") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng", color = Color.Gray) } }
    )
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    isOutlined: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, color),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
        ) { Text(text) }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) { Text(text) }
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF007AFF))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLink: Boolean = false,
    isCopyable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(
                text = value,
                fontWeight = FontWeight.Medium,
                color = if (isLink) Color(0xFF007AFF) else Color.Black,
                fontSize = 15.sp
            )
        }
        if (isCopyable) Icon(
            Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = Color.LightGray
        )
    }
}
