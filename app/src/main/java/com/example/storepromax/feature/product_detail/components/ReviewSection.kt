package com.example.storepromax.feature.product_detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.storepromax.domain.model.UserReview
import com.example.storepromax.ui.components.StarRatingBar

@Composable
fun ReviewSection(
    reviews: List<UserReview>,
    currentUserRating: Int,
    onCommentSubmit: (String, String?, Int) -> Unit,
    onDeleteComment: (String) -> Unit,
    onEditComment: (String, String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    var replyingToId by remember { mutableStateOf<String?>(null) }

    var tempRating by remember(currentUserRating) { mutableIntStateOf(currentUserRating) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Đánh giá sản phẩm", style = MaterialTheme.typography.titleMedium)

                StarRatingBar(
                    rating = tempRating,
                    onRatingChanged = { newRating ->
                        tempRating = newRating
                    },
                    isEditable = currentUserRating == 0
                )

                if (currentUserRating > 0) {
                    Text("Bạn đã đánh giá sản phẩm này.", style = MaterialTheme.typography.bodySmall)
                } else if (tempRating > 0) {
                    Text("Bạn đang chọn $tempRating sao", style = MaterialTheme.typography.bodySmall)
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
                trailingIcon = {
                    IconButton(onClick = {
                        if (commentText.isNotBlank() || (tempRating > 0 && replyingToId == null)) {

                            onCommentSubmit(commentText, replyingToId, tempRating)

                            commentText = ""
                            replyingToId = null
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Gửi")
                    }
                }
            )
        }

        Divider()
        Column(modifier = Modifier.fillMaxWidth()) {
            reviews.forEach { review ->
                CommentItem(
                    comment = review,
                    onReplyClick = { parentId -> replyingToId = parentId },
                    onDeleteClick = onDeleteComment,
                    onEditClick = onEditComment
                )
            }
        }
    }
}