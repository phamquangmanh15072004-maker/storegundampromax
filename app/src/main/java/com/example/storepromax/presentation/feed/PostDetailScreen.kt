package com.example.storepromax.presentation.feed

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Comment
import com.example.storepromax.domain.utils.formatVietnameseCurrency
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val post by viewModel.post.collectAsState()
    val currentUserId = viewModel.currentUserId
    val isLoading by viewModel.isLoading.collectAsState()
    var replyingToCommentId by remember { mutableStateOf("") }
    var replyingToUserName by remember { mutableStateOf("") }
    var replyingToUserId by remember { mutableStateOf("") }
    var editingComment by remember { mutableStateOf<Comment?>(null) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
        viewModel.loadPostAndComments(postId)
    }

    val comments by viewModel.comments.collectAsState()
    var commentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết bài đăng", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (post != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (editingComment != null || replyingToCommentId.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFF0F2F5),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (editingComment != null) {
                                        Text("Đang chỉnh sửa bình luận", fontSize = 12.sp, color = Color(0xFF0D47A1), fontWeight = FontWeight.Bold)
                                        Text(editingComment!!.content, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    } else {
                                        Text("Đang trả lời", fontSize = 12.sp, color = Color.Gray)
                                        Text(replyingToUserName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingComment = null
                                        replyingToCommentId = ""
                                        replyingToUserName = ""
                                        commentText = ""
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) { Icon(Icons.Default.Close, contentDescription = "Hủy", tint = Color.Gray, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                    Surface(color = Color.White, shadowElevation = 8.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text(if (editingComment != null) "Sửa bình luận..." else "Viết bình luận...", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF0F2F5),
                                    unfocusedContainerColor = Color(0xFFF0F2F5),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (editingComment != null) {
                                        val finalUpdatedContent = if (editingComment!!.replyingToName.isNotEmpty()) {
                                            "@${editingComment!!.replyingToName} $commentText"
                                        } else { commentText }
                                        viewModel.updateComment(
                                            commentId = editingComment!!.id,
                                            newContent = finalUpdatedContent,
                                            onSuccess = { Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show() },
                                            onError = { errorMsg -> Toast.makeText(context, "Thất bại: $errorMsg", Toast.LENGTH_LONG).show() }
                                        )
                                        editingComment = null
                                        commentText = ""
                                    } else {
                                        val finalContent = if (replyingToCommentId.isNotEmpty()) { "@$replyingToUserName $commentText" } else { commentText }
                                        viewModel.sendComment(
                                            postId = postId,
                                            content = finalContent,
                                            parentId = replyingToCommentId,
                                            replyingToUserId = replyingToUserId,
                                            replyingToName = replyingToUserName
                                        ) { Toast.makeText(context, "Đã gửi bình luận!", Toast.LENGTH_SHORT).show() }

                                        commentText = ""
                                        replyingToCommentId = ""
                                        replyingToUserName = ""
                                        replyingToUserId = ""
                                    }
                                },
                                modifier = Modifier.background(if (commentText.isNotBlank()) Color(0xFF0D47A1) else Color.LightGray, CircleShape).size(48.dp),
                                enabled = commentText.isNotBlank()
                            ) { Icon(Icons.Default.Send, contentDescription = "Gửi", tint = Color.White) }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF0D47A1)) }
        } else if (post == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Không tìm thấy bài đăng hoặc bài đã bị xóa.", color = Color.Gray) }
        } else {
            val p = post!!
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

                item {
                    if (p.images.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { p.images.size })
                        Box(modifier = Modifier.fillMaxWidth().height(350.dp).background(Color.Black)) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                AsyncImage(
                                    model = p.images[page],
                                    contentDescription = "Post Image",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (p.images.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("${pagerState.currentPage + 1}/${p.images.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.LightGray))
                    }
                }

                item {
                    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val priceStr = try { DecimalFormat("#,###").format(p.price.toLong()) + " đ" } catch (e: Exception) { "${p.price} đ" }
                            Text(text = priceStr, color = Color(0xFFD32F2F), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(p.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (condText, condBg, condTextCol) = when (p.condition) {
                                    "NEW" -> Triple("Mới tinh", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                    "LIKE NEW" -> Triple("Như mới", Color(0xFFE3F2FD), Color(0xFF1565C0))
                                    "USED" -> Triple("Đã ráp", Color(0xFFFFF3E0), Color(0xFFEF6C00))
                                    "JUNK" -> Triple("Xác/Junk", Color(0xFFFFEBEE), Color(0xFFC62828))
                                    else -> Triple(p.condition, Color(0xFFF5F5F5), Color.DarkGray)
                                }

                                Surface(color = condBg, shape = RoundedCornerShape(8.dp)) {
                                    Text(condText, color = condTextCol, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(color = Color(0xFFF3E5F5), shape = RoundedCornerShape(8.dp)) {
                                    Text("Grade: ${p.grade}", color = Color(0xFF6A1B9A), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = p.userAvatar.takeIf { it.isNotBlank() } ?: "https://ui-avatars.com/api/?name=${p.userName}",
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFEEEEEE)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    val dateString = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(p.createdAt))
                                    Text(dateString, color = Color.Gray, fontSize = 12.sp)
                                }
                                if (p.userId != currentUserId) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.contactSeller(p) { channelId ->
                                                navController.navigate("chat_detail/$channelId")
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D47A1)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Mô tả chi tiết", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Text(p.content, fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF333333))
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bình luận (${p.commentCount})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                val topLevelComments = comments.filter { it.parentId.isEmpty() }
                items(topLevelComments) { parentComment ->
                    CommentItemView(
                        comment = parentComment, isReply = false, currentUserId = currentUserId,
                        onReplyClick = { replyingToCommentId = parentComment.id; replyingToUserName = parentComment.userName; replyingToUserId = parentComment.userId },
                        onEditClick = { editingComment = parentComment; commentText = parentComment.content.replace("@${parentComment.replyingToName} ", "") },
                        onDeleteClick = { commentToDelete = parentComment }
                    )

                    val replies = comments.filter { it.parentId == parentComment.id }
                    Column {
                        replies.forEach { reply ->
                            CommentItemView(
                                comment = reply, isReply = true, currentUserId = currentUserId,
                                onReplyClick = { replyingToCommentId = parentComment.id; replyingToUserName = reply.userName; replyingToUserId = reply.userId },
                                onEditClick = { editingComment = reply; commentText = reply.content.replace("@${reply.replyingToName} ", "") },
                                onDeleteClick = { commentToDelete = reply }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
    if (commentToDelete != null) {
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Xóa bình luận?") },
            text = { Text("Bạn có chắc muốn xóa bình luận này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        val cId = commentToDelete!!.id
                        commentToDelete = null
                        viewModel.deleteComment(postId = postId, commentId = cId,
                            onSuccess = { Toast.makeText(context, "Đã xóa bình luận", Toast.LENGTH_SHORT).show() },
                            onError = { errorMsg -> Toast.makeText(context, "Lỗi xóa: $errorMsg", Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { commentToDelete = null }) { Text("Hủy", color = Color.Gray) } }
        )
    }
}

@Composable
fun CommentItemView(
    comment: Comment,
    isReply: Boolean,
    currentUserId: String,
    onReplyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                start = if (isReply) 56.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
        ) {
            AsyncImage(
                model = comment.userAvatar.takeIf { it.isNotBlank() && it != "null" }
                    ?: "https://ui-avatars.com/api/?name=${comment.userName.replace(" ", "+")}&background=random",
                contentDescription = "Avatar",
                modifier = Modifier.size(if (isReply) 28.dp else 40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .background(
                            color = if (isReply) Color(0xFFF5F5F5) else Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if(isReply) Color.Black else Color(0xFF0D47A1), modifier = Modifier.weight(1f))
                        if (comment.userId == currentUserId) {
                            Box {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp).clickable { expandedMenu = true }
                                )
                                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                                    DropdownMenuItem(text = { Text("Chỉnh sửa", fontSize = 14.sp) }, onClick = { expandedMenu = false; onEditClick() })
                                    DropdownMenuItem(text = { Text("Xóa", color = Color.Red, fontSize = 14.sp) }, onClick = { expandedMenu = false; onDeleteClick() })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    val annotatedString = buildAnnotatedString {
                        val mentionTag = "@${comment.replyingToName}"

                        if (comment.replyingToName.isNotEmpty() && comment.content.startsWith(mentionTag)) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))) {
                                append(mentionTag)
                            }
                            append(comment.content.substring(mentionTag.length))
                        } else {
                            append(comment.content)
                        }
                    }
                    Text(text = annotatedString, fontSize = 14.sp, color = Color.Black)
                }

                Row(modifier = Modifier.padding(start = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.createdAt.toRelativeTime(), color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Trả lời", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.clickable { onReplyClick() })
                }
            }
        }
    }
}