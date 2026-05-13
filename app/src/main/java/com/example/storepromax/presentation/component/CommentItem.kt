import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.ui.components.StarRatingBar

@Composable
fun CommentItem(
    comment: UserReview,
    currentUserId: String,
    onReplyClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onEditClick: (String, String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf(comment.content) }

    val isMyComment = comment.userId == currentUserId
    val displayName = if (isMyComment) "${comment.userName} (Tôi)" else comment.userName
    val nameColor = if (isMyComment) Color(0xFF0D47A1) else Color.Black // Xanh đậm chuyên nghiệp hơn

    Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
        // --- PHẦN BÌNH LUẬN GỐC ---
        Row(modifier = Modifier.fillMaxWidth()) {
            // AVATAR
            if (comment.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = comment.avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFEEEEEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = comment.userName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // TÊN & MENU 3 CHẤM
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = nameColor
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 🌟 MENU 3 CHẤM CHO OWNER
                    if (isMyComment) {
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Chỉnh sửa") },
                                    onClick = { isEditing = true; showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa bình luận", color = Color.Red) },
                                    onClick = { onDeleteClick(comment.id); showMenu = false }
                                )
                            }
                        }
                    }
                }

                // SỐ SAO
                if (comment.rating > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    StarRatingBar(rating = comment.rating, isEditable = false, maxStars = 5, onRatingChanged = {})
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 🌟 LOGIC CHỈNH SỬA THÔNG MINH
                if (isEditing) {
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isEditing = false; editContent = comment.content }) {
                            Text("Hủy", color = Color.Gray)
                        }
                        Button(
                            onClick = { onEditClick(comment.id, editContent); isEditing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                        ) {
                            Text("Lưu")
                        }
                    }
                } else {
                    // HIỂN THỊ TEXT
                    Text(text = comment.content, fontSize = 14.sp, color = Color(0xFF333333), lineHeight = 20.sp)

                    // 🌟 HIỂN THỊ ẢNH/VIDEO (MEDIA)
                    val mediaUrls = /* comment.mediaUrls */ listOf<String>() // Thay bằng comment.mediaUrls khi model đã có
                    if (mediaUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(mediaUrls) { url ->
                                val isVideo = url.contains(".mp4") || url.contains("video")
                                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    if (isVideo) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayCircleOutline, contentDescription = "Play", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // NÚT TRẢ LỜI
                    Text(
                        text = "Trả lời",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp).clickable { onReplyClick(comment.id) }
                    )
                }
            }
        }

        // --- PHẦN DANH SÁCH TRẢ LỜI (REPLIES) ---
        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 52.dp, top = 12.dp)
                    .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp)) // Bọc nền xám nhạt cho list reply nhìn tách biệt
                    .padding(12.dp)
            ) {
                comment.replies.forEachIndexed { index, reply ->
                    val isMyReply = reply.userId == currentUserId
                    val replyDisplayName = if (isMyReply) "${reply.userName} (Tôi)" else reply.userName
                    val replyNameColor = if (isMyReply) Color(0xFF0D47A1) else Color.DarkGray

                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = if (index == comment.replies.size - 1) 0.dp else 12.dp)) {
                        if (reply.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = reply.avatarUrl,
                                contentDescription = "Reply Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(28.dp).clip(CircleShape)
                            )
                        } else {
                            Box(modifier = Modifier.size(28.dp).background(Color(0xFFE0E0E0), CircleShape), contentAlignment = Alignment.Center) {
                                Text(text = reply.userName.take(1).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = replyDisplayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = replyNameColor)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = reply.content, fontSize = 13.sp, color = Color(0xFF444444), lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}