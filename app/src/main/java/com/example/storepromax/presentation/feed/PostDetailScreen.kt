package com.example.storepromax.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.utils.formatVietnameseCurrency // Nhớ import hàm format tiền của bạn
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val post by viewModel.post.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết bài đăng", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (post != null) {
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Giá pass lại:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = formatVietnameseCurrency(post!!.price),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722)
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.contactSeller(post!!) { channelId ->
                                    navController.navigate("chat_detail/$channelId")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nhắn người bán", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0D47A1))
            }
        } else if (post == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy bài đăng hoặc bài đã bị xóa.", color = Color.Gray)
            }
        } else {
            val p = post!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                item {
                    if (p.images.isNotEmpty()) {
                        LazyRow(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                            items(p.images) { imgUrl ->
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Post Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .height(350.dp)
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.LightGray))
                    }
                }
                item {
                    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(p.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tagColor = if (p.condition == "USED") Color.Red else Color(0xFF4CAF50)
                                val tagText = if (p.condition == "USED") "Đã ráp" else "Mới tinh"
                                Surface(color = tagColor, shape = RoundedCornerShape(4.dp)) {
                                    Text(tagText, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(color = Color.DarkGray, shape = RoundedCornerShape(4.dp)) {
                                    Text("Grade: ${p.grade}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.weight(1f))
                                val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(p.createdAt))
                                Text(dateString, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            AsyncImage(
                                model = p.userAvatar.ifEmpty { "https://ui-avatars.com/api/?name=${p.userName}&background=random" },
                                contentDescription = "Avatar",
                                modifier = Modifier.size(50.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(p.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Chủ bài đăng", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Mô tả chi tiết", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = p.content,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}