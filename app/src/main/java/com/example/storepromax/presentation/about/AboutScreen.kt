package com.example.storepromax.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Về ứng dụng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = CircleShape,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Gunpla Store",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF)
                    )

                    Text(
                        text = "Version 1.0.0 (Pilot Build)",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mission
            SectionCard(title = "Sứ mệnh của chúng tôi") {

                Text(
                    text = "Gunpla Store là nền tảng thương mại điện tử chuyên về mô hình Gundam, Figure và các sản phẩm sưu tầm dành cho cộng đồng đam mê Gunpla. Chúng tôi hướng tới trải nghiệm mua sắm hiện đại, an toàn và thuận tiện cho cả người mua lẫn người bán.",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }

            // Contact
            SectionCard(title = "Liên hệ & Hỗ trợ") {

                AboutItem(
                    icon = Icons.Default.Language,
                    label = "Website",
                    value = "www.gunplastore.com"
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                AboutItem(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = "support@gunplastore.com"
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                AboutItem(
                    icon = Icons.Default.Phone,
                    label = "Hotline",
                    value = "1900 1000 (08:00 - 17:00)"
                )
            }
            SectionCard(title = "Thông tin phát triển") {

                AboutItem(
                    icon = Icons.Default.Code,
                    label = "Phát triển bởi",
                    value = "Mạnh Proplayer"
                )

                HorizontalDivider(color = Color(0xFFF0F0F0))

                AboutItem(
                    icon = Icons.Default.Verified,
                    label = "Công nghệ",
                    value = "Jetpack Compose, Firebase, Hilt, Room, Coil"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "© 2026 Gunpla Store. All rights reserved.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {

        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = 8.dp,
                bottom = 8.dp
            )
        )

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun AboutItem(
    icon: ImageVector,
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {

            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                fontSize = 15.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
    }
}