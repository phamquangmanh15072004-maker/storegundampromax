package com.example.storepromax.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SupportButton(
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFF00C853),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
            .size(65.dp)
            .scale(scale)
    ) {
        Icon(
            imageVector = Icons.Default.SupportAgent,
            contentDescription = "Hỗ trợ khách hàng",
            modifier = Modifier.size(32.dp)
        )
    }
}