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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.storepromax.presentation.main.MainViewModel
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
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val isSearching by viewModel.isSearching.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val mainViewModel: MainViewModel = hiltViewModel(context as androidx.activity.ComponentActivity)
    LaunchedEffect(Unit) {
        mainViewModel.scrollToTopEvent.collect {
            listState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.loadInitialFeed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier.fillMaxWidth().height(40.dp).padding(end = 8.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = Color.Black),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) Text("Tìm Gundam, người bán...", color = Color.Gray, fontSize = 14.sp)
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        Text("CHỢ GUNDAM", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF0D47A1))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.onSearchQueryChange("")
                    }) {
                        Icon(imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Tìm kiếm", tint = Color.DarkGray)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = isFabExpanded,
                onClick = { navController.navigate("create_post") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Đăng bài") },
                text = { Text("Đăng bán", fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF0D47A1),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = Color(0xFFF2F4F8)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0D47A1), strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshFeed() },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                if (isRefreshing && posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0D47A1)) }
                } else if (posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.ShoppingBag, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(if (searchQuery.isNotEmpty()) "Không tìm thấy kết quả" else "Chợ đang trống!", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Hãy là người đầu tiên tạo bài viết.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
                    ) {
                        items(posts, key = { it.id }) { post ->
                            FeedPostItem(
                                post = post,
                                currentUserId = currentUserId,
                                onDeleteClick = { postToDelete = post; showDeleteConfirmDialog = true },
                                onLikeClick = { viewModel.toggleLike(post.id, context) },
                                onImageClick = { url -> selectedImageUrl = url; showImageDialog = true },
                                onUserClick = { userId -> navController.navigate("profile_detail/$userId") },
                                onCommentClick = { post.id.let { postId -> navController.navigate("post_detail/$postId") } }
                            )
                        }
                    }
                }
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF0D47A1), strokeWidth = 3.dp)
                    }
                }
            }
        }
        if (showDeleteConfirmDialog && postToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Xóa bài viết?") },
                text = { Text("Bạn có chắc chắn muốn xóa bài viết \"${postToDelete?.title}\" không?") },
                confirmButton = {
                    Button(onClick = { postToDelete?.let { viewModel.deletePost(it.id) }; showDeleteConfirmDialog = false; postToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Xóa ngay") }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Hủy", color = Color.Gray) } }
            )
        }
        if (showImageDialog && selectedImageUrl.isNotEmpty()) {
            ImagePreviewDialog(imageUrl = selectedImageUrl) { showImageDialog = false }
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
    onUserClick: (String) -> Unit,
    onCommentClick: () -> Unit
) {
    val isOwner = post.userId == currentUserId
    val isLiked = post.likedByUsers.contains(currentUserId)
    val heartColor by animateColorAsState(targetValue = if (isLiked) Color(0xFFD32F2F) else Color.Gray, label = "color")
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val explosionAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(isLiked) {
        if (isLiked) {
            explosionAnim.snapTo(0f)
            explosionAnim.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
            )
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onUserClick(post.userId) }.padding(horizontal = 16.dp)
            ) {
                AsyncImage(
                    model = post.userAvatar.takeIf { it.isNotBlank() } ?: "https://ui-avatars.com/api/?name=${post.userName}",
                    contentDescription = "Avatar",
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFEEEEEE)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.userName.ifBlank { "Thành viên ẩn danh" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Text(post.createdAt.toRelativeTime(), color = Color.Gray, fontSize = 12.sp)
                }
                if (isOwner) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa", tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(post.price.toVietnameseCurrency(), color = Color(0xFFD32F2F), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    val (condText, condBg, condTextCol) = when (post.condition) {
                        "NEW" -> Triple("Mới", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                        "LIKE NEW" -> Triple("Như mới", Color(0xFFE3F2FD), Color(0xFF1565C0))
                        "USED" -> Triple("Đã ráp", Color(0xFFFFF3E0), Color(0xFFEF6C00))
                        "JUNK" -> Triple("Xác/Junk", Color(0xFFFFEBEE), Color(0xFFC62828))
                        else -> Triple(post.condition, Color(0xFFF5F5F5), Color.DarkGray)
                    }

                    Surface(color = condBg, shape = RoundedCornerShape(4.dp)) {
                        Text(condText, fontSize = 10.sp, color = condTextCol, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = Color(0xFFF3E5F5), shape = RoundedCornerShape(4.dp)) {
                        Text("Grade: ${post.grade}", fontSize = 10.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Text(post.content, fontSize = 14.sp, color = Color(0xFF444444), maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
            }
            if (post.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostImageCarousel(images = post.images, onImageClick = onImageClick)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFFF0F0F0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onLikeClick() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (explosionAnim.value > 0f && explosionAnim.value < 1f) {
                            val numParticles = 6
                            val radius = 45.dp.value * explosionAnim.value
                            for (i in 0 until numParticles) {
                                val angle = (i * (360 / numParticles)) * (Math.PI / 180)
                                val offsetX = (radius * kotlin.math.cos(angle)).dp
                                val offsetY = (radius * kotlin.math.sin(angle)).dp
                                val alpha = 1f - explosionAnim.value
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F).copy(alpha = alpha),
                                    modifier = Modifier
                                        .offset(offsetX, offsetY)
                                        .size(14.dp)
                                        .scale(1f - explosionAnim.value)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = heartColor,
                            modifier = Modifier
                                .size(24.dp)
                                .scale(heartScale)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.likeCount > 0) "${post.likeCount}" else "Thích",
                        fontSize = 14.sp,
                        color = if (isLiked) Color(0xFFD32F2F) else Color.Gray,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onCommentClick() }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = "Comment", tint = Color.Gray, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (post.commentCount > 0) "${post.commentCount}" else "Bình luận", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentScale = ContentScale.FillWidth
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
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
            .height(300.dp)
            .background(Color.Black)
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
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
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