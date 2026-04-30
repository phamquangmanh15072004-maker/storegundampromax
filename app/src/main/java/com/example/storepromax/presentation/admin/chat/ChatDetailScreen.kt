package com.example.storepromax.presentation.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    navController: NavController,
    channelId: String,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(channelId) { viewModel.loadMessages(channelId) }

    val uploadingMedia by viewModel.uploadingMedia.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()

    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForOptions by remember { mutableStateOf<ChatMessage?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mediaPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
            viewModel.sendMedia(channelId, uri, isVideo)
        }
    }

    LaunchedEffect(messages.size, uploadingMedia) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + (if (uploadingMedia != null) 1 else 0))
        }
    }

    val blockedByList = currentChannel?.blockedBy ?: emptyList()
    val isBlocked = blockedByList.isNotEmpty()
    val amIBlocking = blockedByList.contains(viewModel.currentUserId)

    Scaffold(
        topBar = {
            val partnerAvatarUrl by viewModel.partnerAvatarUrl.collectAsState()
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF007AFF)) }
                    val isSupport = currentChannel?.type == "SUPPORT"
                    val chatPartnerName = if (isSupport) "Hỗ trợ Gunpla Store" else if (currentChannel?.userId == viewModel.currentUserId) currentChannel?.receiverName else currentChannel?.userName
                    val chatPartnerId = if (currentChannel?.userId == viewModel.currentUserId) currentChannel?.receiverId else currentChannel?.userId

                    LaunchedEffect(chatPartnerId) {
                        if (!chatPartnerId.isNullOrBlank() && !isSupport) {
                            viewModel.fetchPartnerInfo(chatPartnerId)
                        }
                    }
                    val partnerLastActive = System.currentTimeMillis() - (2 * 60 * 1000)
                    val (statusText, isOnline) = formatActiveStatus(partnerLastActive)

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { if (!chatPartnerId.isNullOrBlank() && !isSupport) navController.navigate("profile_detail/$chatPartnerId") }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            if (isSupport) {
                                Surface(shape = CircleShape, color = Color(0xFFE0F2F1), modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.SupportAgent, null, tint = Color(0xFF00695C), modifier = Modifier.padding(8.dp))
                                }
                            } else {
                                AsyncImage(
                                    model = if (!partnerAvatarUrl.isNullOrBlank()) partnerAvatarUrl else "https://ui-avatars.com/api/?name=${chatPartnerName ?: "User"}",
                                    contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.size(40.dp).clip(CircleShape)
                                )
                            }
                            if (isOnline && !isBlocked && !isSupport) {
                                Box(modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).background(Color(0xFF4CAF50), CircleShape).border(2.dp, Color.White, CircleShape))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = chatPartnerName ?: "Khách hàng", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (isBlocked) "Không khả dụng" else if (isSupport) "Luôn sẵn sàng hỗ trợ" else statusText, fontSize = 12.sp, color = if (isOnline && !isBlocked) Color(0xFF4CAF50) else Color.Gray)
                        }
                    }

                    if (!isSupport) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color(0xFF007AFF))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.White)) {
                if (isBlocked) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F2F5)).padding(16.dp), contentAlignment = Alignment.Center) {
                        if (amIBlocking) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Bạn đã chặn người này.", color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.unblockUser(channelId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    border = BorderStroke(1.dp, Color.LightGray)
                                ) { Text("Bỏ chặn", fontWeight = FontWeight.Bold) }
                            }
                        } else {
                            Text("Bạn không thể trả lời cuộc trò chuyện này.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    EmojiBar(onEmojiClick = { textState += it })

                    if (replyingToMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(4.dp).height(36.dp).background(Color(0xFF007AFF), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Đang trả lời ${if (replyingToMessage!!.senderId == viewModel.currentUserId) "chính mình" else "đối phương"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF007AFF))
                                Text(replyingToMessage!!.content, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                        }
                    }

                    ChatInputBar(
                        text = textState, onTextChange = { textState = it },
                        onSend = { viewModel.sendMessage(channelId, textState, replyingToMessage?.id); textState = ""; replyingToMessage = null },
                        onAttachClick = { mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
                    )
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC)), // Nền chat hơi ngà cho nổi bật bong bóng
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == viewModel.currentUserId
                val repliedMsg = if (msg.replyToId != null) messages.find { it.id == msg.replyToId } else null
                MessageBubble(message = msg, isMe = isMe, repliedMessage = repliedMsg, currentUserId = viewModel.currentUserId, onImageClick = { url -> previewImageUrl = url }, onLongPress = { selectedMessageForOptions = msg })
            }
            if (uploadingMedia != null) {
                item { UploadingBubble(media = uploadingMedia!!) }
            }
        }
    }

    if (previewImageUrl != null) FullImageDialog(imageUrl = previewImageUrl!!) { previewImageUrl = null }

    if (selectedMessageForOptions != null) {
        ModalBottomSheet(onDismissRequest = { selectedMessageForOptions = null }, sheetState = bottomSheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                if (selectedMessageForOptions?.content != "Tin nhắn đã bị thu hồi" && !isBlocked) {
                    ListItem(headlineContent = { Text("Trả lời", fontWeight = FontWeight.Medium) }, leadingContent = { Icon(Icons.Default.Reply, null, tint = Color.Black) }, modifier = Modifier.clickable { replyingToMessage = selectedMessageForOptions; selectedMessageForOptions = null })
                }
                if (selectedMessageForOptions?.senderId == viewModel.currentUserId && selectedMessageForOptions?.content != "Tin nhắn đã bị thu hồi") {
                    ListItem(headlineContent = { Text("Thu hồi", fontWeight = FontWeight.Medium, color = Color.Red) }, leadingContent = { Icon(Icons.Default.Undo, null, tint = Color.Red) }, modifier = Modifier.clickable { viewModel.revokeMessage(channelId, selectedMessageForOptions!!.id); selectedMessageForOptions = null })
                }
                ListItem(headlineContent = { Text("Gỡ ở phía bạn", fontWeight = FontWeight.Medium) }, leadingContent = { Icon(Icons.Default.DeleteOutline, null, tint = Color.Black) }, modifier = Modifier.clickable { viewModel.deleteMessageForMe(channelId, selectedMessageForOptions!!.id); selectedMessageForOptions = null })
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }, sheetState = bottomSheetState, containerColor = Color.White) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                if (amIBlocking) {
                    ListItem(headlineContent = { Text("Bỏ chặn người dùng này", fontWeight = FontWeight.Medium, color = Color(0xFF007AFF)) }, leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF007AFF)) }, modifier = Modifier.clickable { viewModel.unblockUser(channelId); showSettingsSheet = false })
                } else {
                    ListItem(headlineContent = { Text("Chặn người dùng", fontWeight = FontWeight.Medium, color = Color.Red) }, leadingContent = { Icon(Icons.Default.Block, null, tint = Color.Red) }, modifier = Modifier.clickable { viewModel.blockUser(channelId); showSettingsSheet = false })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean, repliedMessage: ChatMessage?, currentUserId: String, onImageClick: (String) -> Unit, onLongPress: () -> Unit) {
    val context = LocalContext.current
    val timeString = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)) }
    val textBubbleShape = if (isMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    val bubbleColor = if (isMe) Color(0xFF007AFF) else Color(0xFFE4E6EB)
    val textColor = if (isMe) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        if (message.content == "Tin nhắn đã bị thu hồi") {
            Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = message.content, color = Color.Gray, fontSize = 14.sp, fontStyle = FontStyle.Italic)
            }
            return
        }

        if (repliedMessage != null) {
            val replySender = if (repliedMessage.senderId == currentUserId) "Bạn" else "Đối phương"
            Column(modifier = Modifier.padding(bottom = 2.dp).alpha(0.75f), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                Text("Đã trả lời $replySender", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                Box(modifier = Modifier.background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(text = repliedMessage.content, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Color.DarkGray)
                }
            }
        }

        Box(
            modifier = Modifier.widthIn(max = 280.dp).clip(if (message.type == "TEXT") textBubbleShape else RoundedCornerShape(16.dp)).background(if (message.type == "TEXT") bubbleColor else Color.Transparent).combinedClickable(onClick = { }, onLongClick = { onLongPress() })
        ) {
            when (message.type) {
                "IMAGE" -> { AsyncImage(model = message.mediaUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.heightIn(max = 300.dp).clickable { onImageClick(message.mediaUrl) }) }
                "VIDEO" -> {
                    Box(modifier = Modifier.height(200.dp).background(Color.Black).clickable {
                        try { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.mediaUrl)); intent.setDataAndType(Uri.parse(message.mediaUrl), "video/*"); context.startActivity(intent) } catch (e: Exception) {}
                    }, contentAlignment = Alignment.Center) {
                        AsyncImage(model = message.mediaUrl.replace(".mp4", ".jpg"), contentDescription = null, modifier = Modifier.alpha(0.6f), contentScale = ContentScale.Crop)
                        Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
                else -> { Text(text = message.content, color = textColor, fontSize = 15.sp, modifier = Modifier.padding(12.dp)) }
            }
        }
        Text(text = timeString, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp))
    }
}

@Composable
fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, onAttachClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAttachClick) { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Gửi media", tint = Color(0xFF007AFF)) }
        TextField(
            value = text, onValueChange = onTextChange, placeholder = { Text("Nhắn tin...", color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp), shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedContainerColor = Color(0xFFF0F2F5), unfocusedContainerColor = Color(0xFFF0F2F5), cursorColor = Color(0xFF007AFF)),
            maxLines = 4, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() })
        )
        val canSend = text.isNotBlank()
        IconButton(onClick = onSend, enabled = canSend) { Icon(Icons.Default.Send, contentDescription = "Gửi", tint = if (canSend) Color(0xFF007AFF) else Color.Gray) }
    }
}

@Composable
fun EmojiBar(onEmojiClick: (String) -> Unit) {
    val emojis = listOf("👍", "❤️", "😂", "😭", "😡", "🥰", "✅", "👋")
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items(emojis) { emoji -> Text(text = emoji, fontSize = 24.sp, modifier = Modifier.clip(CircleShape).clickable { onEmojiClick(emoji) }.padding(8.dp)) }
    }
}

@Composable
fun FullImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().align(Alignment.Center), contentScale = ContentScale.Fit)
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
    }
}

@Composable
fun UploadingBubble(media: UploadingMedia) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.End) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(model = media.uri, contentDescription = "Uploading", modifier = Modifier.width(150.dp).height(200.dp).clip(RoundedCornerShape(16.dp)).alpha(0.5f).background(Color.Gray), contentScale = ContentScale.Crop)
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
        }
        Text(text = "Đang gửi...", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, end = 4.dp))
    }
}