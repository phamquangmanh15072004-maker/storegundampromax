package com.example.storepromax.feature.product_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
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
fun ReviewSection(
    currentUserId: String, // 👉 Nhận ID từ DetailScreen
    averageRating: Double, // 👉 Nhận điểm trung bình
    totalRatings: Int,     // 👉 Nhận tổng số đánh giá
    reviews: List<UserReview>,
    currentUserRating: Int,
    onCommentSubmit: (String, String?, Int) -> Unit,
    onDeleteComment: (String) -> Unit,
    onEditComment: (String, String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingToId by remember { mutableStateOf<String?>(null) }
    var tempRating by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {

        if (totalRatings > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

        if (currentUserRating == 0 && replyingToId == null) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Đánh giá sản phẩm", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    StarRatingBar(
                        rating = tempRating,
                        onRatingChanged = { newRating -> tempRating = newRating },
                        isEditable = true,
                        maxStars = 5
                    )

                    if (tempRating > 0) {
                        Text("Bạn đang chọn $tempRating sao", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text(if (replyingToId == null) "Viết bình luận..." else "Đang trả lời...") },
                modifier = Modifier.weight(1f),
                leadingIcon = if (replyingToId != null) {
                    {
                        IconButton(onClick = { replyingToId = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Hủy trả lời", tint = Color.Red)
                        }
                    }
                } else null,
                trailingIcon = {
                    IconButton(onClick = {
                        if (commentText.isNotBlank() || (tempRating > 0 && replyingToId == null)) {
                            onCommentSubmit(commentText, replyingToId, tempRating)
                            commentText = ""
                            replyingToId = null
                            tempRating = 0
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Gửi", tint = Color(0xFF007AFF))
                    }
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            reviews.forEach { review ->
                CommentItem(
                    comment = review,
                    currentUserId = currentUserId,
                    onReplyClick = { parentId -> replyingToId = parentId },
                    onDeleteClick = onDeleteComment,
                    onEditClick = onEditComment
                )
            }
        }
    }
}