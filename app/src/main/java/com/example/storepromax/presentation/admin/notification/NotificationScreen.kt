package com.example.storepromax.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.storepromax.domain.model.UserNotification
import com.example.storepromax.presentation.admin.notification.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

val GunplaBlue = Color(0xFF0D47A1)
val BgColor = Color(0xFFF2F4F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông báo", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Color.White
                        ) {
                            DropdownMenuItem(
                                text = { Text("Đánh dấu đã đọc tất cả", color = Color.Black) },
                                leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null, tint = GunplaBlue) },
                                onClick = {
                                    viewModel.markAllAsRead()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa tất cả", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    viewModel.deleteAllNotifications()
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GunplaBlue)
            )
        },
        containerColor = BgColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Bạn chưa có thông báo nào", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationItem(
                            item = notification,
                            onClick = {
                                if (!notification.isRead) {
                                    viewModel.markAsRead(notification.id)
                                }
                                when {
                                    (notification.type == "COMMENT" || notification.type == "LIKE") && notification.postId != null -> {
                                        navController.navigate("post_detail/${notification.postId}")
                                    }
                                    notification.action == "NAVIGATE_TO_REVIEW" && notification.orderId != null -> {
                                        navController.navigate("write_review_screen/${notification.orderId}")
                                    }
                                    notification.orderId != null -> {
                                        val tabIndex = when {
                                            notification.title.contains("xác nhận", ignoreCase = true) -> 2
                                            notification.title.contains("vận chuyển", ignoreCase = true) || notification.title.contains("giao hàng", ignoreCase = true) -> 3
                                            notification.title.contains("thành công", ignoreCase = true) -> 4
                                            notification.title.contains("hủy", ignoreCase = true) -> 5
                                            notification.title.contains("hoàn tiền", ignoreCase = true) -> 6
                                            else -> 0
                                        }
                                        navController.navigate("order_history_screen/$tabIndex")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    item: UserNotification,
    onClick: () -> Unit
) {
    val backgroundColor = if (!item.isRead) Color(0xFFE3F2FD) else Color.White
    val (icon, iconColor, iconBgColor) = when (item.type) {
        "ORDER_UPDATE" -> {
            if (item.title.contains("thành công")) {
                Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), Color(0xFFE8F5E9))
            } else if (item.title.contains("hủy")) {
                Triple(Icons.Default.Cancel, Color.Red, Color(0xFFFFEBEE))
            } else {
                Triple(Icons.Default.LocalShipping, GunplaBlue, Color(0xFFE3F2FD))
            }
        }
        "NEW_ORDER" -> Triple(Icons.Default.Receipt, Color(0xFFFF9800), Color(0xFFFFF3E0))
        "COMMENT" -> Triple(Icons.Default.ChatBubbleOutline, Color(0xFF0288D1), Color(0xFFE1F5FE))
        "LIKE" -> Triple(Icons.Default.Favorite, Color(0xFFFF424F), Color(0xFFFFEBEE))
        "CHAT", "CHAT_ADMIN" -> Triple(Icons.Default.Message, Color(0xFF9C27B0), Color(0xFFF3E5F5))
        else -> Triple(Icons.Default.Notifications, Color.Gray, Color(0xFFF5F5F5))
    }

    val formattedTime = remember(item.timestamp) {
        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.body,
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formattedTime,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
}