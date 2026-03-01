package com.example.storepromax.feature.product_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
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
    val nameColor = if (isMyComment) Color(0xFF007AFF) else Color.Black // Xanh nước biển nếu là "Tôi"

    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (comment.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = comment.avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = nameColor
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (isMyComment) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sửa") },
                                    onClick = {
                                        isEditing = true
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa", color = Color.Red) },
                                    onClick = {
                                        onDeleteClick(comment.id)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (comment.rating > 0) {
                    StarRatingBar(
                        rating = comment.rating,
                        isEditable = false,
                        maxStars = 5,
                        onRatingChanged = {}
                    )
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                onEditClick(comment.id, editContent)
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Green)
                            }
                        }
                    )
                } else {
                    Text(text = comment.content, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }

                if (!isEditing) {
                    TextButton(
                        onClick = { onReplyClick(comment.id) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Trả lời", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        if (comment.replies.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 48.dp, top = 8.dp)) {
                comment.replies.forEach { reply ->

                    val isMyReply = reply.userId == currentUserId
                    val replyDisplayName = if (isMyReply) "${reply.userName} (Tôi)" else reply.userName
                    val replyNameColor = if (isMyReply) Color(0xFF007AFF) else Color.Black

                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        if (reply.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = reply.avatarUrl,
                                contentDescription = "Reply Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(30.dp).background(Color.Gray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = replyDisplayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = replyNameColor
                            )
                            Text(text = reply.content, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}