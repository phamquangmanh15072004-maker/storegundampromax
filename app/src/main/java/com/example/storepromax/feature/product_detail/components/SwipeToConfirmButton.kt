package com.example.storepromax.feature.product_detail.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToConfirmButton(
    text: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFE8F5E9),
    thumbColor: Color = Color(0xFF2E7D32),
    textColor: Color = Color(0xFF2E7D32)
) {
    val coroutineScope = rememberCoroutineScope()
    var isConfirmed by remember { mutableStateOf(false) }
    val dragOffset = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (isConfirmed) thumbColor else backgroundColor)
    ) {
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val thumbSizePx = with(LocalDensity.current) { 56.dp.toPx() }
        val maxDragPx = maxWidthPx - thumbSizePx
        if (!isConfirmed) {
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(1f - (dragOffset.value / maxDragPx))
                    .padding(start = 24.dp)
            )
        } else {
            Text(
                text = "Đã xác nhận",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (!isConfirmed) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                    .size(56.dp)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (dragOffset.value > maxDragPx * 0.75f) {
                                        dragOffset.animateTo(maxDragPx, tween(200))
                                        isConfirmed = true
                                        onConfirm()
                                    } else {
                                        dragOffset.animateTo(0f, tween(300))
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (dragOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                                dragOffset.snapTo(newOffset)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardDoubleArrowRight,
                    contentDescription = "Swipe",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}