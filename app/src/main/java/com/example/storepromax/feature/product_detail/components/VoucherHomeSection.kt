package com.example.storepromax.feature.product_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.presentation.cart.TealFreeship
import io.github.sceneview.math.Box
import java.text.DecimalFormat

@Composable
fun VoucherHomeSection(
    vouchers: List<Voucher>,
    userVoucherIds: List<String>,
    onClaim: (Voucher) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            "Siêu Voucher Hôm Nay",
            fontWeight = FontWeight.Bold, fontSize = 18.sp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(vouchers) { voucher ->
                val isClaimed = userVoucherIds.contains(voucher.id)

                // Thẻ Voucher nhỏ xinh trên Home
                Surface(
                    modifier = Modifier.width(280.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(modifier = Modifier.height(80.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.fillMaxHeight().width(70.dp)
                                .background(if (voucher.type == "FREESHIP") TealFreeship else GunplaBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (voucher.type == "FREESHIP") Icons.Default.LocalShipping else Icons.Default.ConfirmationNumber,
                                null, tint = Color.White, modifier = Modifier.size(24.dp)
                            )
                        }

                        // Cột giữa: Nội dung
                        Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                            Text(voucher.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Đơn từ ₫${DecimalFormat("#,###").format(voucher.minOrderValue)}", fontSize = 11.sp, color = Color.Gray)
                        }

                        TextButton(
                            onClick = { if (!isClaimed) onClaim(voucher) },
                            enabled = !isClaimed,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                if (isClaimed) "Đã lưu" else "LƯU",
                                fontWeight = FontWeight.Bold,
                                color = if (isClaimed) Color.Gray else GunplaBlue,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}