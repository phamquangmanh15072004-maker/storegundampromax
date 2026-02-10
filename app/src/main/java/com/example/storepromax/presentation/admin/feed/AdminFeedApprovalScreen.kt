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
    val context = LocalContext.current

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
                        Text("${pendingPosts.size} bài cần xử lý", fontSize = 12.sp, color = Color.Gray)
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
        if (pendingPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tuyệt vời! Đã hết bài chờ duyệt.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pendingPosts) { post ->
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

        if (showRejectDialog && selectedPostToReject != null) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("Từ chối bài đăng", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Vui lòng chọn lý do từ chối cho bài: '${selectedPostToReject?.title}'")
                        Spacer(modifier = Modifier.height(12.dp))

                        val reasons = listOf(
                            "Nội dung không phù hợp",
                            "Hình ảnh vi phạm/Mờ",
                            "Spam/Tin rác",
                            "Sai danh mục",
                            "Giá không thực tế"
                        )

                        reasons.forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rejectionReason = reason }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (reason == rejectionReason),
                                    onClick = { rejectionReason = reason }
                                )
                                Text(text = reason, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedPostToReject?.let { viewModel.rejectPost(it, rejectionReason) }
                            showRejectDialog = false
                            rejectionReason = "Nội dung không phù hợp" // Reset về mặc định
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Xác nhận TỪ CHỐI")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = false }) { Text("Hủy") }
                }
            )
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
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.userAvatar.isNotEmpty()) {
                        AsyncImage(model = post.userAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = post.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Đăng lúc: ${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(post.createdAt))}", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

            Row {
                if (post.images.isNotEmpty()) {
                    AsyncImage(
                        model = post.images.first(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEEEEE)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(text = post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = "${formatter.format(post.price)} ₫", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) {
                        Text(" ${post.grade} - ${post.condition} ", fontSize = 11.sp, color = Color(0xFF1976D2), modifier = Modifier.padding(2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = post.content, fontSize = 14.sp, color = Color.DarkGray, maxLines = 3, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TỪ CHỐI")
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)) // Màu xanh lá
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DUYỆT BÀI")
                }
            }
        }
    }
}