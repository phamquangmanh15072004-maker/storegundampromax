package com.example.storepromax.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (myChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Chưa có cuộc trò chuyện nào", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(myChats, key = { it.id }) { channel ->
                    UserChatRowItem(
                        channel = channel,
                        currentUserId = currentUserId,
                        onClick = {
                            navController.navigate("chat_detail/${channel.id}")
                        }
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
    val displayName = remember(channel, currentUserId) {
        when {
            channel.type == "SUPPORT" -> "Hỗ trợ StorePro"
            else -> {
                if (channel.userId == currentUserId) {
                    channel.receiverName.ifBlank { "Người dùng" }
                } else {
                    channel.userName.ifBlank { "Người dùng" }
                }
            }
        }
    }

    val rawAvatar = remember(channel, currentUserId) {
        if (channel.userId == currentUserId) {
            channel.receiverAvatar
        } else {
            channel.userAvatar
        }
    }

    val displayImage = remember(rawAvatar, displayName) {
        if (!rawAvatar.isNullOrBlank()) {
            rawAvatar
        } else {
            val safeName = URLEncoder.encode(displayName, "UTF-8")
            "https://ui-avatars.com/api/?name=$safeName&background=random"
        }
    }

    val productImage = channel.productImage

    val timeString = remember(channel.lastUpdated) {
        val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        try { sdf.format(Date(channel.lastUpdated)) } catch (e: Exception) { "" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channel.type == "SUPPORT") {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE0F2F1),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = Color(0xFF00695C),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = displayImage,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = timeString, fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(4.dp))
                if (channel.productName.isNotEmpty() && channel.type != "SUPPORT") {
                    Text(
                        text = "Về: ${channel.productName}",
                        fontSize = 12.sp,
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = channel.lastMessage,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (productImage.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = productImage,
                    contentDescription = "Product Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.LightGray)
                )
            }
        }
    }
}