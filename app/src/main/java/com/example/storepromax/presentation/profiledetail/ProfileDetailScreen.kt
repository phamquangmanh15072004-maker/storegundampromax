package com.example.storepromax.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.storepromax.presentation.feed.FeedPostItem
import com.example.storepromax.presentation.feed.ImagePreviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    navController: NavController,
    targetUserId: String,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(targetUserId) {
        viewModel.loadProfileData(targetUserId)
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showImageDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    val isMe = viewModel.currentUserId == targetUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isMe) "Trang cá nhân của bạn" else userProfile?.name ?: "Trang cá nhân",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF0F2F5)
    ) { padding ->
        if (isLoading || userProfile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        ) {
                            if (userProfile!!.avatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = userProfile!!.avatarUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).align(Alignment.Center),
                                    tint = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = userProfile!!.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (isMe) {
                            Text(text = userProfile!!.email, fontSize = 14.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isMe) {
                            Button(
                                onClick = { navController.navigate("edit_profile") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE4E6EB),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chỉnh sửa trang cá nhân", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.contactUser { channelId ->
                                        navController.navigate("chat_detail/$channelId")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0084FF),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Nhắn tin", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(thickness = 1.dp, color = Color.LightGray)
                    Text(
                        text = if(isMe) "Bài viết của bạn" else "Bài viết của ${userProfile!!.name}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp
                    )
                }

                if (userPosts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Chưa có bài viết nào", color = Color.Gray)

                            if (isMe) {
                                TextButton(onClick = { navController.navigate("create_post") }) {
                                    Text("Đăng bài ngay")
                                }
                            }
                        }
                    }
                } else {
                    items(userPosts) { post ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            FeedPostItem(
                                post = post,
                                currentUserId = viewModel.currentUserId,
                                onDeleteClick = {
                                    if (isMe) viewModel.deletePost(post.id)
                                },
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onImageClick = { imageUrl ->
                                    selectedImageUrl = imageUrl
                                    showImageDialog = true
                                },
                                onChatClick = {
                                    if (!isMe) {
                                        viewModel.contactUser { cid ->
                                            navController.navigate("chat_detail/$cid")
                                        }
                                    }
                                },
                                onUserClick = { userId ->
                                    if (userId != targetUserId) {
                                        navController.navigate("profile_detail/$userId")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (showImageDialog && selectedImageUrl.isNotEmpty()) {
        ImagePreviewDialog(imageUrl = selectedImageUrl) {
            showImageDialog = false
        }
    }
}