package com.example.storepromax.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.ChatChannel
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserChatListScreen(
    navController: NavController,
    viewModel: UserChatViewModel = hiltViewModel()
) {
    val myChats by viewModel.myChats.collectAsState()
    val currentUserId = viewModel.currentUserId
    DisposableEffect(Unit) {
        ChatStateManager.isChatListOpen = true
        onDispose {
            ChatStateManager.isChatListOpen = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn", fontWeight = FontWeight.Bold, fontSize = 22.sp) }, // To hơn một xíu cho sang
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (myChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFE0E0E0))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chưa có cuộc trò chuyện nào", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(myChats, key = { it.id }) { channel ->
                    UserChatRowItem(
                        channel = channel,
                        currentUserId = currentUserId,
                        onClick = { navController.navigate("chat_detail/${channel.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun UserChatRowItem(
    channel: ChatChannel,
    currentUserId: String,
    onClick: () -> Unit
) {
    val isSupport = channel.type == "SUPPORT"
    val displayName = remember(channel, currentUserId) {
        when {
            isSupport -> "Hỗ trợ Gunpla Store"
            channel.userId == currentUserId -> channel.receiverName.ifBlank { "Người dùng" }
            else -> channel.userName.ifBlank { "Người dùng" }
        }
    }

    val rawAvatar = remember(channel, currentUserId) {
        if (channel.userId == currentUserId) channel.receiverAvatar else channel.userAvatar
    }

    val displayImage = remember(rawAvatar, displayName) {
        if (!rawAvatar.isNullOrBlank()) rawAvatar
        else {
            val safeName = URLEncoder.encode(displayName, "UTF-8")
            "https://ui-avatars.com/api/?name=$safeName&background=random"
        }
    }

    val timeString = remember(channel.lastUpdated) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        try { sdf.format(Date(channel.lastUpdated)) } catch (e: Exception) { "" }
    }

    val myUnreadCount = channel.unreadCounts[currentUserId] ?: 0
    val hasUnread = myUnreadCount > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (hasUnread) Color(0xFFF4F8FD) else Color.White) // Nền hơi xanh nhẹ nếu có tin mới
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSupport) {
                Surface(shape = CircleShape, color = Color(0xFFE0F2F1), modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SupportAgent, null, tint = Color(0xFF00695C), modifier = Modifier.padding(12.dp))
                }
            } else {
                AsyncImage(
                    model = displayImage, contentDescription = "Avatar", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = displayName,
                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (hasUnread) Color.Black else Color(0xFF333333),
                        fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeString,
                        fontSize = 12.sp,
                        color = if (hasUnread) Color(0xFF006AF5) else Color.Gray,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (channel.productName.isNotEmpty() && !isSupport) {
                            Text(text = "Về: ${channel.productName}", fontSize = 12.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            text = channel.lastMessage,
                            fontSize = 14.sp,
                            color = if (hasUnread) Color.Black else Color.Gray,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hasUnread) {
                        val badgeText = if (myUnreadCount > 9) "9+" else myUnreadCount.toString()

                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .offset(y = (-1).dp)
                                .defaultMinSize(minWidth = 20.dp)
                                .height(20.dp)
                                .background(Color(0xFFE53935), CircleShape)
                                .padding(horizontal = if (badgeText.length > 1) 6.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
        Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(start = 88.dp, end = 16.dp))
    }
}
