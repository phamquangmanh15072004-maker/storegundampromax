package com.example.storepromax.presentation.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.ChatMessage
import com.example.storepromax.presentation.admin.chat.ChatDetailViewModel
import com.example.storepromax.presentation.admin.chat.PendingChatMessage
import com.example.storepromax.presentation.admin.chat.PendingMessageStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private sealed class ChatTimelineItem {
    abstract val id: String
    abstract val timestamp: Long

    data class Sent(val message: ChatMessage) : ChatTimelineItem() {
        override val id: String = "sent-${message.id}"
        override val timestamp: Long = message.timestamp
    }

    data class Pending(val message: PendingChatMessage) : ChatTimelineItem() {
        override val id: String = "pending-${message.id}"
        override val timestamp: Long = message.timestamp
    }
}

fun formatActiveStatus(lastActive: Long?): Pair<String, Boolean> {
    if (lastActive == null || lastActive == 0L) return Pair("Không rõ", false)
    val diffMinutes = (System.currentTimeMillis() - lastActive) / (1000 * 60)
    return when {
        diffMinutes < 5 -> Pair("Đang hoạt động", true)
        diffMinutes < 60 -> Pair("Hoạt động $diffMinutes phút trước", false)
        diffMinutes < 1440 -> Pair("Hoạt động ${diffMinutes / 60} giờ trước", false)
        else -> Pair("Hoạt động ${diffMinutes / 1440} ngày trước", false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    channelId: String,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    DisposableEffect(channelId) {
        ChatStateManager.activeChannelId = channelId
        onDispose { ChatStateManager.activeChannelId = null }
    }
    LaunchedEffect(channelId) { viewModel.loadMessages(channelId) }

    val messages by viewModel.messages.collectAsState()
    val pendingMessages by viewModel.pendingMessages.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val partnerAvatarUrl by viewModel.partnerAvatarUrl.collectAsState()

    val timelineItems = remember(messages, pendingMessages) {
        val sentMessageIds = messages.map { it.id }.toSet()
        val visiblePendingMessages = pendingMessages.filterNot { it.id in sentMessageIds }
        (messages.map { ChatTimelineItem.Sent(it) } + visiblePendingMessages.map { ChatTimelineItem.Pending(it) })
            .sortedBy { it.timestamp }
    }

    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForOptions by remember { mutableStateOf<ChatMessage?>(null) }
    var expandedMessageId by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mediaPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
            viewModel.sendMedia(channelId, uri, isVideo)
        }
    }

    LaunchedEffect(timelineItems.size) {
        if (timelineItems.isNotEmpty()) {
            listState.animateScrollToItem(timelineItems.lastIndex)
        }
    }

    val blockedByList = currentChannel?.blockedBy ?: emptyList()
    val isBlocked = blockedByList.isNotEmpty()
    val amIBlocking = blockedByList.contains(viewModel.currentUserId)
    val isSupport = currentChannel?.type == "SUPPORT"
    val chatPartnerName = if (isSupport) {
        "Hỗ trợ Gunpla Store"
    } else if (currentChannel?.userId == viewModel.currentUserId) {
        currentChannel?.receiverName
    } else {
        currentChannel?.userName
    }
    val chatPartnerId = if (currentChannel?.userId == viewModel.currentUserId) currentChannel?.receiverId else currentChannel?.userId

    LaunchedEffect(chatPartnerId, isSupport) {
        if (!chatPartnerId.isNullOrBlank() && !isSupport) {
            viewModel.fetchPartnerInfo(chatPartnerId)
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color(0xFF007AFF))
                    }
                    ChatHeader(
                        isSupport = isSupport,
                        isBlocked = isBlocked,
                        chatPartnerName = chatPartnerName ?: "Khách hàng",
                        partnerAvatarUrl = partnerAvatarUrl,
                        onClick = {
                            if (!chatPartnerId.isNullOrBlank() && !isSupport) {
                                navController.navigate("profile_detail/$chatPartnerId")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSupport) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Outlined.Info, contentDescription = "Thông tin", tint = Color(0xFF007AFF))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.White)) {
                if (isBlocked) {
                    BlockedFooter(
                        amIBlocking = amIBlocking,
                        onUnblock = { viewModel.unblockUser(channelId) }
                    )
                } else {
                    EmojiBar(onEmojiClick = { textState += it })
                    if (replyingToMessage != null) {
                        ReplyPreview(
                            message = replyingToMessage!!,
                            currentUserId = viewModel.currentUserId,
                            onClear = { replyingToMessage = null }
                        )
                    }
                    ChatInputBar(
                        text = textState,
                        onTextChange = { textState = it },
                        onSend = {
                            val contentToSend = textState.trim()
                            if (contentToSend.isBlank()) return@ChatInputBar
                            val replyId = replyingToMessage?.id
                            textState = ""
                            replyingToMessage = null
                            viewModel.sendMessage(channelId, contentToSend, replyId)
                        },
                        onAttachClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(timelineItems, key = { _, item -> item.id }) { index, item ->
                val previous = timelineItems.getOrNull(index - 1)
                if (previous == null || !isSameMessageDay(previous.timestamp, item.timestamp)) {
                    DateSeparator(timestamp = item.timestamp)
                }

                when (item) {
                    is ChatTimelineItem.Sent -> {
                        val msg = item.message
                        val isMe = msg.senderId == viewModel.currentUserId
                        val repliedMsg = msg.replyToId?.let { replyId -> messages.find { it.id == replyId } }
                        MessageBubble(
                            message = msg,
                            isMe = isMe,
                            repliedMessage = repliedMsg,
                            currentUserId = viewModel.currentUserId,
                            showTime = expandedMessageId == item.id,
                            onClick = {
                                expandedMessageId = if (expandedMessageId == item.id) null else item.id
                            },
                            onImageClick = { url -> previewImageUrl = url },
                            onLongPress = { selectedMessageForOptions = msg }
                        )
                    }

                    is ChatTimelineItem.Pending -> {
                        val pending = item.message
                        val repliedMsg = pending.replyToId?.let { replyId -> messages.find { it.id == replyId } }
                        PendingMessageBubble(
                            message = pending,
                            repliedMessage = repliedMsg,
                            currentUserId = viewModel.currentUserId,
                            showTime = expandedMessageId == item.id,
                            onClick = {
                                expandedMessageId = if (expandedMessageId == item.id) null else item.id
                            },
                            onRetry = { viewModel.retryPendingMessage(pending.id) },
                            onDismissFailed = { viewModel.removePendingMessage(pending.id) }
                        )
                    }
                }
            }
        }
    }

    if (previewImageUrl != null) {
        FullImageDialog(imageUrl = previewImageUrl!!) { previewImageUrl = null }
    }

    if (selectedMessageForOptions != null) {
        ModalBottomSheet(onDismissRequest = { selectedMessageForOptions = null }, sheetState = bottomSheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                val selected = selectedMessageForOptions!!
                if (selected.content != REVOKED_MESSAGE && !isBlocked) {
                    ListItem(
                        headlineContent = { Text("Trả lời", fontWeight = FontWeight.Medium) },
                        leadingContent = { Icon(Icons.Default.Reply, null, tint = Color.Black) },
                        modifier = Modifier.clickable {
                            replyingToMessage = selected
                            selectedMessageForOptions = null
                        }
                    )
                }
                if (selected.senderId == viewModel.currentUserId && selected.content != REVOKED_MESSAGE) {
                    ListItem(
                        headlineContent = { Text("Thu hồi", fontWeight = FontWeight.Medium, color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Undo, null, tint = Color.Red) },
                        modifier = Modifier.clickable {
                            viewModel.revokeMessage(channelId, selected.id)
                            selectedMessageForOptions = null
                        }
                    )
                }
                ListItem(
                    headlineContent = { Text("Gỡ ở phía bạn", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Default.DeleteOutline, null, tint = Color.Black) },
                    modifier = Modifier.clickable {
                        viewModel.deleteMessageForMe(channelId, selected.id)
                        selectedMessageForOptions = null
                    }
                )
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }, sheetState = bottomSheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                if (amIBlocking) {
                    ListItem(
                        headlineContent = { Text("Bỏ chặn người dùng này", fontWeight = FontWeight.Medium, color = Color(0xFF007AFF)) },
                        leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF007AFF)) },
                        modifier = Modifier.clickable {
                            viewModel.unblockUser(channelId)
                            showSettingsSheet = false
                        }
                    )
                } else {
                    ListItem(
                        headlineContent = { Text("Chặn người dùng", fontWeight = FontWeight.Medium, color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.Block, null, tint = Color.Red) },
                        modifier = Modifier.clickable {
                            viewModel.blockUser(channelId)
                            showSettingsSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(timestamp: Long) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
        Text(
            text = formatDateSeparator(timestamp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF6B7280),
            modifier = Modifier.background(Color(0xFFE5E7EB), RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ChatHeader(
    isSupport: Boolean,
    isBlocked: Boolean,
    chatPartnerName: String,
    partnerAvatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partnerLastActive = System.currentTimeMillis() - (2 * 60 * 1000)
    val (statusText, isOnline) = formatActiveStatus(partnerLastActive)
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            if (isSupport) {
                Surface(shape = CircleShape, color = Color(0xFFE0F2F1), modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.SupportAgent, null, tint = Color(0xFF00695C), modifier = Modifier.padding(8.dp))
                }
            } else {
                AsyncImage(
                    model = partnerAvatarUrl?.takeIf { it.isNotBlank() } ?: "https://ui-avatars.com/api/?name=$chatPartnerName",
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            }
            if (isOnline && !isBlocked && !isSupport) {
                Box(modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).background(Color(0xFF4CAF50), CircleShape).border(2.dp, Color.White, CircleShape))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(chatPartnerName, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (isBlocked) "Không khả dụng" else if (isSupport) "Luôn sẵn sàng hỗ trợ" else statusText,
                fontSize = 12.sp,
                color = if (isOnline && !isBlocked) Color(0xFF4CAF50) else Color.Gray
            )
        }
    }
}

@Composable
private fun BlockedFooter(amIBlocking: Boolean, onUnblock: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).padding(16.dp), contentAlignment = Alignment.Center) {
        if (amIBlocking) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bạn đã chặn người này.", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUnblock,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) { Text("Bỏ chặn", fontWeight = FontWeight.Bold) }
            }
        } else {
            Text("Bạn không thể trả lời cuộc trò chuyện này.", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReplyPreview(message: ChatMessage, currentUserId: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(4.dp).height(36.dp).background(Color(0xFF007AFF), RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Đang trả lời ${if (message.senderId == currentUserId) "chính mình" else "đối phương"}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF007AFF)
            )
            Text(message.content, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Color.Gray)
        }
        IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    repliedMessage: ChatMessage?,
    currentUserId: String,
    showTime: Boolean,
    onClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val timeString = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)) }
    val textBubbleShape = if (isMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    val bubbleColor = if (isMe) Color(0xFF007AFF) else Color(0xFFE4E6EB)
    val textColor = if (isMe) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        if (message.content == REVOKED_MESSAGE) {
            Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = message.content, color = Color.Gray, fontSize = 14.sp, fontStyle = FontStyle.Italic)
            }
            if (showTime) MessageTime(timeString)
            return
        }

        ReplyBlock(repliedMessage = repliedMessage, currentUserId = currentUserId, isMe = isMe)

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(if (message.type == "TEXT") textBubbleShape else RoundedCornerShape(16.dp))
                .background(if (message.type == "TEXT") bubbleColor else Color.Transparent)
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
        ) {
            when (message.type) {
                "IMAGE" -> {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.heightIn(max = 300.dp).clickable { onImageClick(message.mediaUrl) }
                    )
                }
                "VIDEO" -> {
                    Box(
                        modifier = Modifier.height(200.dp).background(Color.Black).clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.mediaUrl))
                                intent.setDataAndType(Uri.parse(message.mediaUrl), "video/*")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Không mở được video", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(model = message.mediaUrl.replace(".mp4", ".jpg"), contentDescription = null, modifier = Modifier.alpha(0.6f), contentScale = ContentScale.Crop)
                        Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
                else -> {
                    Text(text = message.content, color = textColor, fontSize = 15.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }

        if (showTime) MessageTime(timeString)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PendingMessageBubble(
    message: PendingChatMessage,
    repliedMessage: ChatMessage?,
    currentUserId: String,
    showTime: Boolean,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDismissFailed: () -> Unit
) {
    val timeString = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)) }
    val shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    val statusText = when (message.status) {
        PendingMessageStatus.SENDING -> "Đang gửi..."
        PendingMessageStatus.FAILED -> message.errorMessage.ifBlank { "Gửi lỗi. Nhấn để thử lại." }
    }
    val statusColor = if (message.status == PendingMessageStatus.FAILED) Color(0xFFD32F2F) else Color.Gray

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        ReplyBlock(repliedMessage = repliedMessage, currentUserId = currentUserId, isMe = true)

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(if (message.type == "TEXT") shape else RoundedCornerShape(16.dp))
                .background(if (message.type == "TEXT") Color(0xFF007AFF).copy(alpha = 0.82f) else Color.Transparent)
                .combinedClickable(onClick = onClick, onLongClick = { if (message.status == PendingMessageStatus.FAILED) onDismissFailed() })
        ) {
            when (message.type) {
                "IMAGE", "VIDEO" -> {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = message.localUri,
                            contentDescription = null,
                            modifier = Modifier.heightIn(max = 300.dp).alpha(if (message.status == PendingMessageStatus.SENDING) 0.62f else 0.85f),
                            contentScale = ContentScale.Crop
                        )
                        if (message.status == PendingMessageStatus.SENDING) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                        }
                        if (message.type == "VIDEO") {
                            Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(46.dp))
                        }
                    }
                }
                else -> Text(text = message.content, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(12.dp))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp, end = 4.dp)) {
            if (showTime) {
                Text(timeString, fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = statusText,
                fontSize = 10.sp,
                color = statusColor,
                fontWeight = if (message.status == PendingMessageStatus.FAILED) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clickable(enabled = message.status == PendingMessageStatus.FAILED) { onRetry() }
            )
        }
    }
}

@Composable
private fun ReplyBlock(repliedMessage: ChatMessage?, currentUserId: String, isMe: Boolean) {
    if (repliedMessage == null) return

    val replySender = if (repliedMessage.senderId == currentUserId) "Bạn" else "Đối phương"
    Column(modifier = Modifier.padding(bottom = 2.dp).alpha(0.75f), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Text("Đã trả lời $replySender", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        Box(modifier = Modifier.background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(text = repliedMessage.content, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun MessageTime(timeString: String) {
    Text(text = timeString, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAttachClick) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Gửi media", tint = Color(0xFF007AFF))
        }
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Nhắn tin...", color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color(0xFFF0F2F5),
                unfocusedContainerColor = Color(0xFFF0F2F5),
                cursorColor = Color(0xFF007AFF)
            ),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() })
        )
        val canSend = text.isNotBlank()
        IconButton(onClick = onSend, enabled = canSend) {
            Icon(Icons.Default.Send, contentDescription = "Gửi", tint = if (canSend) Color(0xFF007AFF) else Color.Gray)
        }
    }
}

@Composable
fun EmojiBar(onEmojiClick: (String) -> Unit) {
    val emojis = listOf("👍", "❤️", "😂", "😭", "😡", "🥰", "✅", "👋")
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items(emojis) { emoji ->
            Text(text = emoji, fontSize = 24.sp, modifier = Modifier.clip(CircleShape).clickable { onEmojiClick(emoji) }.padding(8.dp))
        }
    }
}

@Composable
fun FullImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().align(Alignment.Center), contentScale = ContentScale.Fit)
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
    }
}

private fun isSameMessageDay(first: Long, second: Long): Boolean {
    val firstCalendar = Calendar.getInstance().apply { timeInMillis = first }
    val secondCalendar = Calendar.getInstance().apply { timeInMillis = second }
    return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR) &&
        firstCalendar.get(Calendar.DAY_OF_YEAR) == secondCalendar.get(Calendar.DAY_OF_YEAR)
}

private fun formatDateSeparator(timestamp: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        isSameMessageDay(timestamp, today.timeInMillis) -> "Hôm nay"
        isSameMessageDay(timestamp, yesterday.timeInMillis) -> "Hôm qua"
        target.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private const val REVOKED_MESSAGE = "Tin nhắn đã bị thu hồi"
