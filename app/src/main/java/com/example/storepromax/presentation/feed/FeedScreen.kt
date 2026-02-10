package com.example.storepromax.presentation.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Post
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun Long.toVietnameseCurrency(): String {
    return when {
        this >= 1_000_000_000 -> String.format("%.1f tỷ", this / 1_000_000_000.0).replace(".0", "")
        this >= 1_000_000 -> String.format("%.1f tr", this / 1_000_000.0).replace(".0", "")
        this >= 1_000 -> String.format("%d k", this / 1_000)
        else -> DecimalFormat("#,###").format(this) + " đ"
    }
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "Vừa xong"
        diff < 3600_000 -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} phút trước"
        diff < 86400_000 -> "${TimeUnit.MILLISECONDS.toHours(diff)} giờ trước"
        diff < 172800_000 -> "Hôm qua"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val currentUserId = viewModel.currentUserId

    var showImageDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CHỢ GUNDAM", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_post") },
                containerColor = Color(0xFF00D4FF),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Đăng bài", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = Color(0xFFF2F4F8)
    ) { padding ->
        if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Text("Chưa có bài viết nào hãy tạo bài viết cuả bạn!!!", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(posts) { post ->
                    FeedPostItem(
                        post = post,
                        currentUserId = currentUserId,
                        onDeleteClick = {
                            postToDelete = post
                            showDeleteConfirmDialog = true
                        },
                        onLikeClick = { viewModel.toggleLike(post.id) },
                        onImageClick = { url ->
                            selectedImageUrl = url
                            showImageDialog = true
                        },
                        onChatClick = {
                            viewModel.contactSeller(post) { channelId ->
                                navController.navigate("chat_detail/$channelId")
                            }
                        },
                        onUserClick = { userId ->
                            navController.navigate("profile_detail/$userId")
                        }
                    )
                }
            }
        }

        if (showDeleteConfirmDialog && postToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
                title = { Text("Xóa bài viết?") },
                text = { Text("Bạn có chắc chắn muốn xóa bài viết \"${postToDelete?.title}\" không? Hành động này không thể hoàn tác.") },
                confirmButton = {
                    Button(
                        onClick = {
                            postToDelete?.let { viewModel.deletePost(it.id) }
                            showDeleteConfirmDialog = false
                            postToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Xóa ngay")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Hủy", color = Color.Gray)
                    }
                },
                containerColor = Color.White
            )
        }

        if (showImageDialog && selectedImageUrl.isNotEmpty()) {
            ImagePreviewDialog(imageUrl = selectedImageUrl) {
                showImageDialog = false
            }
        }
    }
}


@Composable
fun FeedPostItem(
    post: Post,
    currentUserId: String,
    onDeleteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onChatClick: () -> Unit,
    onUserClick:(String) ->Unit
) {
    val isOwner = post.userId == currentUserId

    var isLiked by remember(post.id, post.likedByUsers) {
        mutableStateOf(post.likedByUsers.contains(currentUserId))
    }

    var localLikeCount by remember(post.id, post.likeCount) {
        mutableIntStateOf(post.likeCount)
    }

    val heartColor by animateColorAsState(targetValue = if (isLiked) Color(0xFFFF424F) else Color.Gray, label = "")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUserClick(post.userId) }) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF5F5F5))) {
                    if (post.userAvatar.isNotEmpty()) {
                        AsyncImage(model = post.userAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = post.userName.ifBlank { "Thành viên ẩn danh" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF333333)
                    )
                    Text(text = post.createdAt.toRelativeTime(), color = Color.Gray, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isOwner) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa", tint = Color.LightGray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp)
            Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = post.price.toVietnameseCurrency(), color = Color(0xFFFF424F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(4.dp)) {
                    Text(post.grade, fontSize = 10.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                val conditionColor = if (post.condition == "NEW") Color(0xFF22C55E) else Color(0xFFF97316)
                Surface(color = conditionColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(post.condition, fontSize = 10.sp, color = conditionColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(text = post.content, fontSize = 13.sp, color = Color(0xFF555555), maxLines = 3, overflow = TextOverflow.Ellipsis)

            if (post.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostImageCarousel(
                    images = post.images,
                    onImageClick = onImageClick
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF0F0F0))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isLiked = !isLiked
                            if (isLiked) localLikeCount++ else localLikeCount--
                            onLikeClick()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = heartColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (localLikeCount > 0) "$localLikeCount" else "Thích",
                        fontSize = 13.sp,
                        color = if (isLiked) Color(0xFFFF424F) else Color.Gray,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (!isOwner) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChatClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhắn tin", fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("Quản lý bài viết", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePreviewDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() } // Bấm ra ngoài là đóng
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                contentScale = ContentScale.FillWidth
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostImageCarousel(
    images: List<String>,
    onImageClick: (String) -> Unit
) {
    if (images.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onImageClick(images[page]) }
            )
        }
        if (images.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}