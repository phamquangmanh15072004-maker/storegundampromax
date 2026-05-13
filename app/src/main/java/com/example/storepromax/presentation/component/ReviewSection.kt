package com.example.storepromax.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewSection(
    averageRating: Double,
    totalRatings: Int,
    reviews: List<UserReview>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 🌟 KHỐI TÓM TẮT ĐIỂM (Sang trọng, viền cam nhạt, nền kem)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFDF5), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", averageRating),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9800)
                )
                Text(text = "trên 5", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < averageRating.toInt()) Color(0xFFFF9800) else Color(0xFFE2E8F0),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "$totalRatings lượt đánh giá", fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color(0xFFF1F5F9))

        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có đánh giá nào cho sản phẩm này.", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                reviews.forEach { review ->
                    ReviewItem(review = review)
                    Divider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: UserReview) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(review.timestamp))

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
        // 1. Header (Avatar, Tên, Sao)
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = if (review.avatarUrl.isNotEmpty()) review.avatarUrl else "https://ui-avatars.com/api/?name=${review.userName}&background=F1F5F9&color=64748B",
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.userName.take(2) + "***" + review.userName.takeLast(1),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFF9800) else Color(0xFFE2E8F0),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Text(text = dateString, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Nội dung text
        if (review.content.isNotEmpty()) {
            Text(
                text = review.content,
                fontSize = 14.sp,
                color = Color(0xFF334155),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Ảnh đính kèm
        if (!review.mediaUrls.isNullOrEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(review.mediaUrls) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Review Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (!review.adminReply.isNullOrBlank()) {
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Shop",
                            tint = Color(0xFFF97316), // Cam chuẩn
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phản hồi của Shop",
                            color = Color(0xFF334155),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = review.adminReply ?: "",
                        fontSize = 14.sp,
                        color = Color(0xFF475569),
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}