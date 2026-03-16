package com.example.storepromax.feature.product_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.ui.components.StarRatingBar

@Composable
fun ReviewSection(
    currentUserId: String,
    averageRating: Double,
    totalRatings: Int,
    reviews: List<UserReview>,
    currentUserRating: Int,
    isReadOnly: Boolean = false,
    onCommentSubmit: (String, String?, Int) -> Unit,
    onDeleteComment: (String) -> Unit,
    onEditComment: (String, String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingToId by remember { mutableStateOf<String?>(null) }
    var tempRating by remember { mutableIntStateOf(0) }

    var selectedMediaUrl by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (totalRatings > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Đánh giá: ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFD700))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f", averageRating),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFD32F2F)
                )
                Text(
                    text = " / 5 ($totalRatings lượt)",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (!isReadOnly) {
            if (currentUserRating == 0 && replyingToId == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bạn thấy sản phẩm này thế nào?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        StarRatingBar(rating = tempRating, onRatingChanged = { tempRating = it }, isEditable = true, maxStars = 5)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text(if (replyingToId == null) "Chia sẻ trải nghiệm..." else "Đang trả lời...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0D47A1), unfocusedBorderColor = Color.LightGray),
                    leadingIcon = if (replyingToId != null) { { IconButton(onClick = { replyingToId = null }) { Icon(Icons.Default.Close, null, tint = Color.Red) } } } else null,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (commentText.isNotBlank() || (tempRating > 0 && replyingToId == null)) {
                                onCommentSubmit(commentText, replyingToId, tempRating)
                                commentText = ""; replyingToId = null; tempRating = 0
                            }
                        }) { Icon(Icons.Default.Send, null, tint = Color(0xFF0D47A1)) }
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
        }

        if (reviews.isEmpty()) {
            Text(
                text = "Chưa có đánh giá nào cho sản phẩm này.",
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                reviews.forEach { review ->
                    CommentItem(
                        comment = review,
                        currentUserId = currentUserId,
                        onDeleteClick = onDeleteComment,
                        onMediaClick = { clickedUrl ->
                            selectedMediaUrl = clickedUrl
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }

    if (selectedMediaUrl != null) {
        FullScreenMediaViewer(
            mediaUrl = selectedMediaUrl!!,
            onDismiss = { selectedMediaUrl = null }
        )
    }
}

@Composable
fun CommentItem(
    comment: UserReview,
    currentUserId: String,
    onDeleteClick: (String) -> Unit,
    onMediaClick: (String) -> Unit
) {
    val isOwner = comment.userId == currentUserId
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val avatarUrl = comment.avatarUrl

        if (!avatarUrl.isNullOrEmpty()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE0E0E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (comment.userName.isNotBlank()) comment.userName.take(1).uppercase() else "U",
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }
        }


        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (comment.userName.isNotBlank()) comment.userName else "Người dùng ẩn danh", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (comment.rating > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    repeat(comment.rating) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = comment.content, fontSize = 14.sp, color = Color(0xFF333333), lineHeight = 20.sp)

            Spacer(modifier = Modifier.height(8.dp))

            if (comment.mediaUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(comment.mediaUrls) { url ->
                        val isVideo = url.contains(".mp4") || url.contains(".mov") || url.contains("video")

                        val thumbnailUrl = if (isVideo && url.contains("res.cloudinary.com")) {
                            url.substringBeforeLast(".") + ".jpg"
                        } else {
                            url
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                                .clickable {
                                    onMediaClick(url)
                                }
                        ) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = "Review Media",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isVideo) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = "Play", tint = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOwner) {
                    Text(
                        text = "Xóa đánh giá",
                        fontSize = 12.sp,
                        color = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onDeleteClick(comment.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenMediaViewer(
    mediaUrl: String,
    onDismiss: () -> Unit
) {
    val isVideo = mediaUrl.contains(".mp4") || mediaUrl.contains(".mov") || mediaUrl.contains("video")
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Để Dialog chiếm Fullscreen
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isVideo) {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(mediaUrl))
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = "Full Screen Image",
                    contentScale = ContentScale.Fit, // Đảm bảo không bị cắt mất góc
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}