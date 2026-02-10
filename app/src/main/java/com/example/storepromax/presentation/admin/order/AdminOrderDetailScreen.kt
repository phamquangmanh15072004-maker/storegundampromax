package com.example.storepromax.presentation.admin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Order
import com.example.storepromax.presentation.admin.order.AdminOrderDetailViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDetailScreen(
    navController: NavController,
    orderId: String,
    viewModel: AdminOrderDetailViewModel = hiltViewModel()
) {
    val order by viewModel.order.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Màu sắc Light Theme
    val bgLight = Color(0xFFF5F5F5)
    val primaryColor = Color(0xFF007AFF)

    if (order == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val currentOrder = order!!
    val formatter = DecimalFormat("#,###")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chi tiết đơn hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("ID: ${currentOrder.id.uppercase()}", fontSize = 12.sp, color = Color.Gray)
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
                onUpdateStatus = { newStatus -> viewModel.updateStatus(newStatus) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SectionCard(title = "Thông tin giao hàng", icon = Icons.Default.LocalShipping) {
                // Tên khách
                InfoRow(icon = Icons.Default.Person, label = "Người nhận", value = currentOrder.receiverName)
                Divider(color = Color(0xFFEEEEEE))

                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "Điện thoại",
                    value = currentOrder.receiverPhone,
                    isLink = true,
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${currentOrder.receiverPhone}")
                        }
                        context.startActivity(intent)
                    }
                )
                Divider(color = Color(0xFFEEEEEE))

                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Địa chỉ",
                    value = currentOrder.address,
                    isCopyable = true,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(currentOrder.address))
                        Toast.makeText(context, "Đã copy địa chỉ!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            SectionCard(title = "Danh sách sản phẩm (${currentOrder.items.size})", icon = Icons.Default.ShoppingCart) {
                currentOrder.items.forEach { item ->
                    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = item.product.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).background(Color.Gray, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, fontWeight = FontWeight.Medium, maxLines = 2)
                            Text("x${item.quantity}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text("₫${formatter.format(item.totalPrice)}", fontWeight = FontWeight.Bold)
                    }
                    if (item != currentOrder.items.last()) Divider(color = Color(0xFFEEEEEE))
                }
            }

            SectionCard(title = "Thanh toán", icon = Icons.Default.Payments) {
                val isPaid = currentOrder.paymentStatus == "PAID"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Phương thức:", modifier = Modifier.weight(1f), color = Color.Gray)
                    Text(if (currentOrder.paymentMethod == "BANKING") "Chuyển khoản (QR)" else "COD (Tiền mặt)", fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trạng thái:", modifier = Modifier.weight(1f), color = Color.Gray)
                    Surface(
                        color = if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isPaid) "ĐÃ THANH TOÁN" else "CHƯA THANH TOÁN",
                            color = if (isPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                Row {
                    Text("Tổng cộng:", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("₫${formatter.format(currentOrder.totalPrice)}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Padding cho BottomBar
        }
    }
}

// --- COMPONENTS ---

@Composable
fun BottomActionBar(status: String, onUpdateStatus: (String) -> Unit) {
    Surface(shadowElevation = 16.dp, color = Color.White) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Logic nút bấm thay đổi theo trạng thái
            when (status) {
                "PENDING" -> {
                    ActionButton(text = "Hủy đơn", color = Color.Red, isOutlined = true, modifier = Modifier.weight(1f)) { onUpdateStatus("CANCELLED") }
                    ActionButton(text = "Xác nhận đơn", color = Color(0xFF007AFF), modifier = Modifier.weight(1f)) { onUpdateStatus("CONFIRMED") }
                }
                "CONFIRMED" -> {
                    ActionButton(text = "Bắt đầu giao hàng", color = Color(0xFF0097A7), modifier = Modifier.weight(1f)) { onUpdateStatus("SHIPPING") }
                }
                "SHIPPING" -> {
                    ActionButton(text = "Xác nhận đã giao", color = Color(0xFF2E7D32), modifier = Modifier.weight(1f)) { onUpdateStatus("DELIVERED") }
                }
                "DELIVERED" -> {
                    Text("Đơn hàng đã hoàn tất", modifier = Modifier.fillMaxWidth(), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                "CANCELLED" -> {
                    Text("Đơn hàng đã bị hủy", modifier = Modifier.fillMaxWidth(), color = Color.Red, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, isOutlined: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color),
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
fun InfoRow(icon: ImageVector, label: String, value: String, isLink: Boolean = false, isCopyable: Boolean = false, onClick: (() -> Unit)? = null) {
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
        if (isCopyable) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.LightGray)
        }
    }
}