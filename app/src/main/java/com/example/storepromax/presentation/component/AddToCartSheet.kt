package com.example.storepromax.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Product
import java.text.DecimalFormat

fun Long.toVietnameseCurrency(): String {
    return DecimalFormat("#,###").format(this).replace(",", ".") + " đ"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCartSheet(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    confirmButtonText: String = "THÊM VÀO GIỎ HÀNG"
) {
    var quantityText by remember { mutableStateOf("1") }
    val safeQuantity = quantityText.toIntOrNull() ?: 1
    val isOutOfStock = product.stock <= 0
    val canSubmitQuantity = !isOutOfStock && (quantityText.toIntOrNull() ?: 0) in 1..product.stock

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = "Product Image",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = product.name,
                            fontSize = 15.sp,
                            color = Color(0xFF333333),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp).offset(x = 8.dp, y = (-8).dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = product.price.toVietnameseCurrency(),
                        color = Color(0xFFFF424F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Kho: ${product.stock}", color = Color.Gray, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Số lượng", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = { if (safeQuantity > 1) quantityText = (safeQuantity - 1).toString() },
                        modifier = Modifier.size(32.dp),
                        enabled = safeQuantity > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = if (safeQuantity > 1) Color.Black else Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                    BasicTextField(
                        value = quantityText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty()) {
                                quantityText = ""
                            } else if (newValue.all { it.isDigit() }) {
                                val entered = newValue.toIntOrNull() ?: 1
                                quantityText = entered.coerceIn(1, product.stock.coerceAtLeast(1)).toString()
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black),
                        modifier = Modifier.width(48.dp).padding(vertical = 4.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.Center) {
                                if (quantityText.isEmpty()) Text("1", color = Color.LightGray)
                                innerTextField()
                            }
                        }
                    )
                    IconButton(
                        onClick = { if (safeQuantity < product.stock) quantityText = (safeQuantity + 1).toString() },
                        modifier = Modifier.size(32.dp),
                        enabled = safeQuantity < product.stock
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tăng", tint = if (safeQuantity < product.stock) Color.Black else Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (isOutOfStock) {
                Text(
                    text = "Sản phẩm này tạm hết hàng",
                    color = Color(0xFFFF424F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val finalQty = (quantityText.toIntOrNull() ?: 1).coerceIn(1, product.stock.coerceAtLeast(1))
                    onConfirm(finalQty)
                },
                enabled = canSubmitQuantity,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF424F),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    text = if (isOutOfStock) "HẾT HÀNG" else confirmButtonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutOfStock) Color.Gray else Color.White
                )
            }
        }
    }
}
