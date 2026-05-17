package com.example.storepromax.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Product
import java.text.DecimalFormat

private val CurrencyFormatter = DecimalFormat("#,###")

@Composable
fun ProductItem(
    product: Product,
    modifier: Modifier = Modifier,
    showAddToCart: Boolean = true,
    onClick: () -> Unit,
    onAddToCart: (Offset) -> Unit
) {
    val isOutOfStock = remember(product.stock) { product.stock <= 0 }
    val isNew = remember(product.createdAt) { product.isNewProduct() }
    val isHot = remember(product.sold, product.isFeatured) { product.isHotProduct() }
    val discountPercent = remember(product.price, product.originalPrice) { product.getDiscountPercentage() }

    var cartButtonOffset by remember { mutableStateOf(Offset.Zero) }

    val formattedPrice = remember(product.price) { "₫${CurrencyFormatter.format(product.price)}" }
    val formattedOriginalPrice = remember(product.originalPrice) { "₫${CurrencyFormatter.format(product.originalPrice)}" }
    val formattedRating = remember(product.rating) { String.format("%.1f", product.rating) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFF8FAFC))
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )

                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.48f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HẾT HÀNG",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (discountPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFFFF3D00), Color(0xFFFF8F00))
                                        ),
                                        shape = RoundedCornerShape(
                                            topStart = 8.dp,
                                            topEnd = 2.dp,
                                            bottomStart = 2.dp,
                                            bottomEnd = 8.dp
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "-$discountPercent%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        when {
                            isHot -> ProductFlagHot()
                            isNew -> ProductFlagNew()
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = product.name,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOutOfStock) Color(0xFF8A8F98) else Color(0xFF1F2937),
                    modifier = Modifier.height(42.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isOutOfStock) Color(0xFFD1D5DB) else Color(0xFFFFB300),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedRating,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B5563)
                    )
                    Text(
                        text = " • Đã bán ${if (product.sold > 999) "999+" else product.sold}",
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (discountPercent > 0) {
                            Text(
                                text = formattedOriginalPrice,
                                fontSize = 10.sp,
                                color = Color(0xFF9CA3AF),
                                textDecoration = TextDecoration.LineThrough,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Spacer(modifier = Modifier.height(13.dp))
                        }
                        Text(
                            text = formattedPrice,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            color = if (isOutOfStock) Color(0xFF8A8F98) else Color(0xFFE53935),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (showAddToCart && !isOutOfStock) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .onGloballyPositioned { coordinates ->
                                    cartButtonOffset = coordinates.positionInRoot()
                                }
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                                .clickable { onAddToCart(cartButtonOffset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddShoppingCart,
                                contentDescription = "Thêm vào giỏ",
                                tint = Color(0xFF1F2937),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductFlagHot() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text("HOT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProductFlagNew() {
    Box(
        modifier = Modifier
            .background(Color(0xFF00ACC1), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("NEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
