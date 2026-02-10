package com.example.storepromax.feature.product_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.ui.components.StarRatingBar

@Composable
fun CommentItem(
    comment: UserReview,
    currentUserId: String = "me",
    onReplyClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onEditClick: (String, String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf(comment.content) }

    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = comment.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    if (comment.userId == currentUserId) {
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
                if (comment.rating != null && comment.rating > 0) {
                    StarRatingBar(
                        rating = comment.rating,
                        isEditable = false,
                        maxStars = 5,
                        onRatingChanged = {} // Truyền hàm rỗng để tránh lỗi
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
                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        Box(modifier = Modifier.size(30.dp).background(Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(reply.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(reply.content, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}