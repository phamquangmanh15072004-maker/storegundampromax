package com.example.storepromax.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.presentation.feed.FeedPostItem
import com.example.storepromax.presentation.feed.ImagePreviewDialog
import com.example.storepromax.ultils.ToastUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    navController: NavController,
    targetUserId: String,
    viewModel: ProfileDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(targetUserId) {
        viewModel.loadProfileData(targetUserId)
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showImageDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<String?>(null) }

    val isMe = viewModel.currentUserId == targetUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isMe) "Trang cá nhân" else userProfile?.name ?: "Trang cá nhân",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF4F6F8)
    ) { padding ->
        if (isLoading || userProfile == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Brush.horizontalGradient(colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))))
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .border(4.dp, Color.White, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                            ) {
                                if (userProfile!!.avatarUrl.isNotEmpty()) {
                                    AsyncImage(model = userProfile!!.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp).align(Alignment.Center), tint = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = userProfile!!.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (isMe) {
                                Text(text = userProfile!!.email, fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isMe) {
                            OutlinedButton(
                                onClick = { navController.navigate("edit_profile") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chỉnh sửa trang cá nhân", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.contactUser { channelId -> navController.navigate("chat_detail/$channelId") }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Nhắn tin ngay", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if(isMe) "Bài viết của bạn" else "Bài viết của ${userProfile!!.name}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 18.sp
                    )
                }

                if (userPosts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Chưa có bài viết nào", color = Color.Gray, fontSize = 16.sp)

                            if (isMe) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { navController.navigate("create_post") }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                                    Text("Đăng bài ngay")
                                }
                            }
                        }
                    }
                } else {
                    items(userPosts) { post ->
                        val isRejected = post.status == "REJECTED"
                        val isPending = post.status == "PENDING"
                        val isApproved = post.status == "APPROVED"

                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

                            if (isMe && isRejected) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Bài viết BỊ TỪ CHỐI", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Lý do: ${post.rejectionReason ?: "Vi phạm chính sách cộng đồng"}", color = Color(0xFFC62828), fontSize = 13.sp)

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { navController.navigate("edit_post/${post.id}") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            modifier = Modifier.height(36.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Text("Sửa & Đăng lại", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (isMe && isPending) {
                                Surface(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Đang chờ Admin duyệt", color = Color(0xFFE65100), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (isRejected || isPending) 0.5f else 1f)
                            ) {
                                FeedPostItem(
                                    post = post,
                                    currentUserId = viewModel.currentUserId,
                                    onDeleteClick = {
                                        if (isMe) {
                                            postToDelete = post.id
                                            showDeleteDialog = true
                                        }
                                    },
                                    onLikeClick = {
                                        if (isApproved) {
                                            viewModel.toggleLike(post.id)
                                        } else {
                                            ToastUtils.showToast(context, "Bài viết chưa được duyệt, không thể thả tim!")
                                        }
                                    },
                                    onImageClick = { imageUrl ->
                                        selectedImageUrl = imageUrl
                                        showImageDialog = true
                                    },
                                    onUserClick = { },
                                    onCommentClick = {
                                        if (isApproved) {
                                            post.id.let { postId -> navController.navigate("post_detail/$postId") }
                                        } else {
                                            ToastUtils.showToast(context, "Bài viết chưa được duyệt, không thể bình luận!")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showDeleteDialog && postToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Xác nhận xóa", fontWeight = FontWeight.Bold) },
                text = { Text("Bạn có chắc chắn muốn xóa bài viết này không? Hành động này không thể hoàn tác.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePost(postToDelete!!)
                            showDeleteDialog = false
                            postToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Xóa bài")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        postToDelete = null
                    }) {
                        Text("Hủy", color = Color.Gray)
                    }
                }
            )
        }
    }

    if (showImageDialog && selectedImageUrl.isNotEmpty()) {
        ImagePreviewDialog(imageUrl = selectedImageUrl) {
            showImageDialog = false
        }
    }
}