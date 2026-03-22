package com.example.storepromax.presentation.admin.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
fun AdminChatListScreen(
    navController: NavController,
    viewModel: AdminChatListViewModel = hiltViewModel()
) {
    val needsReplyChats by viewModel.needsReplyChannels.collectAsState()
    val allChats by viewModel.allChannels.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Cần phản hồi", "Tất cả")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý CSKH", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF007AFF)
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = if (index == 0) needsReplyChats.size else 0

                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = Color.Red) {
                                        Text("$count", color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            val currentList = if (selectedTabIndex == 0) needsReplyChats else allChats

            if (currentList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có tin nhắn nào", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.id }) { channel ->
                        AdminChatChannelItem(
                            channel = channel,
                            onClick = {
                                navController.navigate("chat_detail/${channel.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminChatChannelItem(channel: ChatChannel, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
    val timeString = try {
        timeFormat.format(Date(channel.lastUpdated))
    } catch (e: Exception) {
        ""
    }

    val displayImage = if (channel.userAvatar.isNotBlank()) {
        channel.userAvatar
    } else {
        val safeName = URLEncoder.encode(channel.userName.ifBlank { "User" }, "UTF-8")
        "https://ui-avatars.com/api/?name=$safeName&background=random"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = displayImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = channel.userName.ifBlank { "Khách hàng" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(text = timeString, fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = channel.lastMessage,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}