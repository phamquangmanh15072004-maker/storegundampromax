package com.example.storepromax.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storepromax.domain.model.Voucher
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val GunplaBlue = Color(0xFF0D47A1)
val TealFreeship = Color(0xFF00BFA5)
val AlertRed = Color(0xFFFF3B30)

@Composable
fun VoucherHomeSection(
    vouchers: List<Voucher>,
    userVoucherIds: List<String>,
    onClaim: (Voucher) -> Unit,
    onSeeAllClick: () -> Unit
) {
    if (vouchers.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                text = "Siêu Voucher Hôm Nay",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Xem tất cả",
                color = GunplaBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(vouchers, key = { it.code }) { voucher ->
                val isClaimed = userVoucherIds.contains(voucher.id) || userVoucherIds.contains(voucher.code)
                VoucherTicketHome(
                    voucher = voucher,
                    isClaimed = isClaimed,
                    onClaim = { onClaim(voucher) }
                )
            }
        }
    }
}

@Composable
fun VoucherTicketHome(
    voucher: Voucher,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val currentTime = System.currentTimeMillis()

    val isNotStarted = voucher.startDate > currentTime
    val isExpired = voucher.expirationDate in 1..<currentTime
    val isDepleted = voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit

    val progress = if (voucher.usageLimit > 0) {
        (voucher.usedCount.toFloat() / voucher.usageLimit.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percentString = (progress * 100).toInt()

    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val timeLabel = when {
        isNotStarted -> "Mở lúc: ${sdf.format(Date(voucher.startDate))}"
        isExpired -> "Đã hết hạn"
        voucher.expirationDate > 0 -> "Có hiệu lực: ${sdf.format(Date(voucher.expirationDate))}"
        else -> "Không thời hạn"
    }

    val iconBgColor = if (voucher.type == "FREESHIP") TealFreeship else GunplaBlue
    val isUnusable = isExpired || isDepleted

    Card(
        modifier = Modifier
            .width(320.dp)
            .height(105.dp)
            .alpha(if (isUnusable) 0.5f else 1f),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(96.dp)
                    .background(if (isUnusable) Color(0xFF9E9E9E) else iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (voucher.type == "FREESHIP") Icons.Default.LocalShipping else Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (voucher.type == "FREESHIP") "FREESHIP" else "GIẢM GIÁ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
            Canvas(modifier = Modifier.fillMaxHeight().width(1.dp)) {
                drawLine(
                    color = Color(0xFFE0E0E0),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = voucher.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF222222),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đơn từ ₫${formatter.format(voucher.minOrderValue)}",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isNotStarted) AlertRed else Color(0xFF9E9E9E),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeLabel,
                            fontSize = 10.sp,
                            color = if (isNotStarted) AlertRed else Color(0xFF9E9E9E)
                        )
                    }

                    if (!isUnusable && !isNotStarted && voucher.usageLimit > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.9f)) {
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100.dp)),
                                color = if (progress >= 0.9f) AlertRed else iconBgColor,
                                trackColor = Color(0xFFEEEEEE)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đã dùng $percentString%", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { if (!isClaimed && !isUnusable) onClaim() },
                    enabled = !isClaimed && !isUnusable,
                    modifier = Modifier.height(32.dp).widthIn(min = 64.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = iconBgColor,
                        disabledContentColor = Color(0xFFBDBDBD)
                    ),
                    border = BorderStroke(1.dp, if (!isClaimed && !isUnusable) iconBgColor else Color(0xFFE0E0E0))
                ) {
                    Text(
                        text = when {
                            isClaimed -> "Đã lưu"
                            isDepleted -> "Hết"
                            isNotStarted -> "Sắp mở"
                            else -> "Lưu"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}