package com.example.storepromax.presentation.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    channelId: String,
    isAdminView: Boolean = false,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(channelId) {
        viewModel.loadMessages(channelId)
    }
    val uploadingMedia by viewModel.uploadingMedia.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()

    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }

    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            val isVideo = mimeType?.startsWith("video") == true

            viewModel.sendMedia(
                channelId = channelId,
                uri = uri,
                isVideo = isVideo,
                isSenderAdmin = isAdminView
            )
        }
    }
    LaunchedEffect(uploadingMedia, messages.size) {
        if (messages.isNotEmpty() || uploadingMedia != null) {
            val targetIndex = if (uploadingMedia != null) messages.size else messages.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val titleText = when {
                            isAdminView -> currentChannel?.userName ?: "Khách hàng"
                            currentChannel?.type == "SUPPORT" -> "Hỗ trợ viên StorePro"
                            else -> if (currentChannel?.userId == viewModel.currentUserId)
                                currentChannel?.receiverName ?: "Chat"
                            else currentChannel?.userName ?: "Chat"
                        }

                        Text(titleText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (isAdminView) {
                            val status = if(currentChannel?.status == "SOLVED") "Đã xong" else "Đang hỗ trợ"
                            Text("Ticket: #${channelId.take(6).uppercase()} • $status", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (isAdminView && currentChannel?.status != "SOLVED") {
                        TextButton(onClick = { viewModel.closeTicket(channelId) }) {
                            Text("HOÀN TẤT", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (currentChannel?.status != "SOLVED") {
                Column {
                    EmojiBar(onEmojiClick = { textState += it })

                    ChatInputBar(
                        text = textState,
                        onTextChange = { textState = it },
                        onSend = {
                            viewModel.sendMessage(channelId, textState, isSenderAdmin = isAdminView)
                            textState = ""
                        },
                        onAttachClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Phiên hỗ trợ này đã kết thúc.", color = Color.Gray)
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = if (isAdminView) msg.isAdmin
                else msg.senderId == viewModel.currentUserId && !msg.isAdmin
                MessageBubble(
                    message = msg,
                    isMe = isMe,
                    onImageClick = { url -> previewImageUrl = url }
                )
            }
            if (uploadingMedia != null) {
                item {
                    UploadingBubble(media = uploadingMedia!!)
                }
            }
        }
    }

    if (previewImageUrl != null) {
        FullImageDialog(imageUrl = previewImageUrl!!) {
            previewImageUrl = null
        }
    }
}


@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val timeString = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        try { sdf.format(Date(message.timestamp)) } catch (e: Exception) { "" }
    }

    val textBubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val mediaShape = RoundedCornerShape(16.dp)

    val bubbleColor = if (isMe) Color(0xFF007AFF) else Color.White
    val textColor = if (isMe) Color.White else Color.Black

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        when (message.type) {
            "IMAGE" -> {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Gửi ảnh",
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .heightIn(max = 350.dp)
                        .padding(bottom = 4.dp)
                        .clip(mediaShape)
                        .clickable { onImageClick(message.mediaUrl) },
                    contentScale = ContentScale.Crop
                )
            }
            "VIDEO" -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .height(200.dp)
                        .padding(bottom = 4.dp)
                        .clip(mediaShape)
                        .background(Color.Black)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.mediaUrl))
                                intent.setDataAndType(Uri.parse(message.mediaUrl), "video/*")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val thumbUrl = if (message.mediaUrl.contains("cloudinary"))
                        message.mediaUrl.replace(".mp4", ".jpg")
                    else message.mediaUrl

                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(0.6f),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(textBubbleShape)
                        .background(bubbleColor)
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
        Text(
            text = timeString,
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit
) {
    Surface(
        shadowElevation = 10.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút Attach (Ghim)
            IconButton(onClick = onAttachClick) {
                Icon(Icons.Default.AttachFile, contentDescription = "Gửi media", tint = Color(0xFF007AFF))
            }

            // Ô nhập liệu bo tròn
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Nhập tin nhắn...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF0F2F5), // Màu xám nhạt nền input
                    unfocusedContainerColor = Color(0xFFF0F2F5),
                    cursorColor = Color(0xFF007AFF)
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() })
            )

            // Nút gửi
            val canSend = text.isNotBlank()
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .background(if (canSend) Color(0xFF007AFF) else Color(0xFFE0E0E0), CircleShape)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Gửi",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmojiBar(onEmojiClick: (String) -> Unit) {
    val emojis = listOf("👍", "❤️", "😂", "😭", "😡", "🥰", "✅", "👋", "📦")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        items(emojis) { emoji ->
            Text(
                text = emoji,
                fontSize = 22.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onEmojiClick(emoji) }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun FullImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full màn hình
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() } // Bấm ra ngoài là đóng
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
            // Nút đóng ở góc
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }
    }
}
@Composable
fun UploadingBubble(media: UploadingMedia) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = media.uri,
                contentDescription = "Uploading",
                modifier = Modifier
                    .width(150.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .alpha(0.5f)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
        Text(
            text = "Đang gửi...",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, end = 4.dp)
        )
    }
}