package com.example.storepromax.presentation.admin.feed

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Post
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedApprovalScreen(
    navController: NavController,
    viewModel: AdminFeedApprovalViewModel = hiltViewModel()
) {
    val pendingPosts by viewModel.pendingPosts.collectAsState()
    val processedPosts by viewModel.processedPosts.collectAsState() // 🌟 Lấy data lịch sử
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 🌟 Biến quản lý Tab
    var selectedPostDetail by remember { mutableStateOf<Post?>(null) }
    var customRejectionReason by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedPostToReject by remember { mutableStateOf<Post?>(null) }
    var rejectionReason by remember { mutableStateOf("Nội dung không phù hợp") }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            Toast.makeText(context, event, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DUYỆT BÀI ĐĂNG", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${pendingPosts.size} bài cần xử lý",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "Chờ duyệt (${pendingPosts.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Lịch sử xử lý", fontWeight = FontWeight.Bold) }
                )
            }
            if (selectedTabIndex == 0) {
                if (pendingPosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tuyệt vời! Đã hết bài chờ duyệt.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pendingPosts) { post ->
                            Box(modifier = Modifier.clickable {
                                selectedPostDetail = post
                            }) { // Bấm vào để xem chi tiết
                                ApprovalPostItem(
                                    post = post,
                                    onApprove = { viewModel.approvePost(post) },
                                    onReject = {
                                        selectedPostToReject = post
                                        showRejectDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                val searchQuery by viewModel.searchQuery.collectAsState()
                val filteredPosts by viewModel.filteredProcessedPosts.collectAsState() // Dùng list đã lọc
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        placeholder = { Text("Tìm theo tên bài hoặc người đăng...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.LightGray,
                            unfocusedIndicatorColor = Color.LightGray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    if (filteredPosts.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isBlank()) "Chưa có lịch sử xử lý." else "Không tìm thấy bài viết nào.",
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredPosts) { post ->
                                Card(
                                    modifier = Modifier.clickable { selectedPostDetail = post },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                        Text(text = post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(text = "Người đăng: ${post.userName}", color = Color.Gray, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (post.status == "APPROVED") {
                                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                                Text(" ✅ ĐÃ DUYỆT ", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                                            }
                                        } else {
                                            Column {
                                                Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                                                    Text(" ❌ ĐÃ TỪ CHỐI ", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Lý do: ${post.rejectionReason ?: "Không rõ"}", color = Color(0xFFC62828), fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showRejectDialog && selectedPostToReject != null) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("Từ chối bài đăng") },
                text = {
                    Column {
                        val reasons = listOf(
                            "Nội dung không phù hợp",
                            "Hình ảnh vi phạm/Mờ",
                            "Spam/Tin rác",
                            "Giá không thực tế",
                            "Lý do khác"
                        )
                        reasons.forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rejectionReason = reason }) {
                                RadioButton(
                                    selected = (reason == rejectionReason),
                                    onClick = { rejectionReason = reason })
                                Text(text = reason)
                            }
                        }
                        if (rejectionReason == "Lý do khác") {
                            OutlinedTextField(
                                value = customRejectionReason,
                                onValueChange = { customRejectionReason = it },
                                placeholder = { Text("Nhập lý do chi tiết...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalReason =
                                if (rejectionReason == "Lý do khác") customRejectionReason else rejectionReason
                            selectedPostToReject?.let { viewModel.rejectPost(it, finalReason) }
                            showRejectDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) { Text("Xác nhận TỪ CHỐI") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRejectDialog = false
                    }) { Text("Hủy") }
                }
            )
        }

        // 🌟 DIALOG XEM CHI TIẾT ĐÃ FIX LỖI CRASH + CÓ LOADING ẢNH
        selectedPostDetail?.let { postDetail ->
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { selectedPostDetail = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        IconButton(onClick = {
                            selectedPostDetail = null
                        }) { Icon(Icons.Default.Close, contentDescription = "Đóng") }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Text(
                                    postDetail.title, // Thay dấu !! bằng postDetail an toàn
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Nội dung: ${postDetail.content}")
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            items(postDetail.images) { imgUrl ->
                                coil.compose.SubcomposeAsyncImage(
                                    model = imgUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEEEEE)),
                                    contentScale = ContentScale.Fit,
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = Color.Gray)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApprovalPostItem(
    post: Post,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val formatter = DecimalFormat("#,###")

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.userAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = post.userAvatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = post.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Đăng lúc: ${
                            java.text.SimpleDateFormat("dd/MM HH:mm")
                                .format(java.util.Date(post.createdAt))
                        }", color = Color.Gray, fontSize = 11.sp
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

            Row {
                if (post.images.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEEEEE))
                    ) {
                        // 🌟 Xoay Loading cho ảnh nhỏ
                        coil.compose.SubcomposeAsyncImage(
                            model = post.images.first(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        )

                        // 🌟 Lớp phủ đen hiện số "+ X" nếu có nhiều hơn 1 ảnh
                        if (post.images.size > 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)), // Lớp mờ đen
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${post.images.size - 1}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }


            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = Color.DarkGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TỪ CHỐI")
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)) // Màu xanh lá
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DUYỆT BÀI")
                }
            }
        }
    }
}