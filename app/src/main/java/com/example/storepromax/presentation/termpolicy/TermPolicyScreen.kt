package com.example.storepromax.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsPolicyScreen(
    navController: NavController
) {

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(
                        text = "Điều khoản & Chính sách",
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
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE3F2FD))
                    .padding(24.dp),

                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Gunpla Store Policies",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Cập nhật lần cuối: 12/05/2026",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                PolicySection(
                    title = "1. Điều khoản sử dụng",

                    content = """
                        Chào mừng bạn đến với Gunpla Store. Khi sử dụng ứng dụng này, bạn đồng ý tuân thủ các quy định sau:
                        
                        • Không sử dụng ứng dụng cho mục đích phi pháp.
                        
                        • Không cố ý tấn công, phá hoại hệ thống hoặc gian lận giao dịch.
                        
                        • Tôn trọng cộng đồng người dùng Gunpla Store.
                        
                        • Không đăng tải sản phẩm giả mạo hoặc nội dung vi phạm bản quyền.
                    """.trimIndent()
                )

                PolicySection(
                    title = "2. Chính sách bảo mật",

                    content = """
                        Chúng tôi cam kết bảo vệ thông tin cá nhân của bạn:
                        
                        • Dữ liệu cá nhân (Email, Tên, Số điện thoại) chỉ được sử dụng cho mục đích xác thực và hỗ trợ giao dịch.
                        
                        • Gunpla Store KHÔNG chia sẻ dữ liệu của bạn cho bên thứ ba nếu không có sự đồng ý.
                        
                        • Lịch sử mua hàng được lưu trữ nhằm phục vụ hỗ trợ khách hàng, bảo hành và xử lý khiếu nại.
                        
                        • Mọi dữ liệu đều được bảo mật thông qua hệ thống xác thực an toàn.
                    """.trimIndent()
                )

                PolicySection(
                    title = "3. Chính sách đổi trả & hoàn tiền",

                    content = """
                        • Thời gian đổi trả: Trong vòng 7 ngày kể từ khi nhận hàng.
                        
                        • Điều kiện đổi trả:
                        - Sản phẩm còn nguyên seal đối với hàng NEW.
                        - Đúng tình trạng mô tả đối với hàng USED.
                        
                        • Không áp dụng đổi trả đối với sản phẩm bị hư hỏng do người dùng.
                        
                        • Hoàn tiền sẽ được xử lý trong vòng 3 - 5 ngày làm việc.
                    """.trimIndent()
                )
                PolicySection(
                    title = "4. Miễn trừ trách nhiệm",

                    content = """
                        Gunpla Store là nền tảng trung gian kết nối người mua và người bán.
                        
                        Chúng tôi không chịu trách nhiệm đối với:
                        
                        • Các giao dịch thực hiện ngoài hệ thống ứng dụng.
                        
                        • Tranh chấp cá nhân giữa người mua và người bán.
                        
                        • Thiệt hại phát sinh do người dùng cung cấp sai thông tin giao dịch.
                    """.trimIndent()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = buildAnnotatedString {

                        append("Bằng việc tiếp tục sử dụng ứng dụng, bạn đồng ý với ")

                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Điều khoản dịch vụ")
                        }

                        append(" và ")

                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF007AFF),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Chính sách bảo mật")
                        }

                        append(" của chúng tôi.")
                    },

                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PolicySection(
    title: String,
    content: String
) {

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,

        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = content,
                fontSize = 14.sp,
                color = Color(0xFF424242),
                lineHeight = 24.sp
            )
        }
    }
}