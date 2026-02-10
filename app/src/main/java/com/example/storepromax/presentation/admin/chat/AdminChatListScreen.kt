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
import androidx.compose.material.icons.filled.Person
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
import androidx.navigation.NavController
import com.example.storepromax.domain.model.ChatChannel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatListScreen(
    navController: NavController,
    viewModel: AdminChatListViewModel = hiltViewModel()
) {
    val pendingChats by viewModel.pendingChannels.collectAsState()
    val processingChats by viewModel.processingChannels.collectAsState()
    val solvedChats by viewModel.solvedChannels.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Chờ xử lý", "Đang chat", "Lịch sử")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CSKH - Tin nhắn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
                    val count = when(index) {
                        0 -> pendingChats.size
                        1 -> processingChats.size
                        else -> 0
                    }

                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(containerColor = if(index==0) Color.Red else Color.Blue) {
                                        Text("$count", color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Danh sách Chat
            val currentList = when (selectedTabIndex) {
                0 -> pendingChats
                1 -> processingChats
                else -> solvedChats
            }

            if (currentList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có tin nhắn nào", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList) { channel ->
                        ChatChannelItem(
                            channel = channel,
                            onClick = {
                                navController.navigate("admin_chat_detail/${channel.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatChannelItem(channel: ChatChannel, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
    val timeString = try { timeFormat.format(Date(channel.lastUpdated)) } catch (e: Exception) { "" }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE0E0E0),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(10.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = channel.userName.ifBlank { "Khách hàng ẩn danh" },
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
            if (channel.status == "PENDING") {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red)
                )
            }
        }
    }
}