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
                title = { Text("Điều khoản & Chính sách", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("StoreProMax Policies", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    Text("Cập nhật lần cuối: 28/05/2024", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {

                PolicySection(
                    title = "1. Điều khoản sử dụng",
                    content = """
                        Chào mừng bạn đến với StoreProMax. Khi sử dụng ứng dụng này, bạn đồng ý tuân thủ các quy định sau:
                        
                        - Không sử dụng ứng dụng cho mục đích phi pháp.
                        - Không cố ý tấn công, phá hoại hệ thống.
                        - Tôn trọng cộng đồng người dùng StoreProMax.
                    """.trimIndent()
                )

                PolicySection(
                    title = "2. Chính sách bảo mật (Privacy)",
                    content = """
                        Chúng tôi cam kết bảo vệ thông tin cá nhân của bạn:
                        
                        - Dữ liệu cá nhân (Email, Tên) chỉ được dùng để xác thực và giao hàng.
                        - StoreProMax KHÔNG chia sẻ dữ liệu của bạn cho bên thứ ba nếu không có sự đồng ý.
                        - Lịch sử mua hàng được lưu trữ để phục vụ việc bảo hành và hỗ trợ.
                    """.trimIndent()
                )

                PolicySection(
                    title = "3. Chính sách đổi trả & Hoàn tiền",
                    content = """
                        - Thời gian đổi trả: Trong vòng 7 ngày kể từ khi nhận hàng.
                        - Điều kiện: Sản phẩm còn nguyên seal, chưa qua sử dụng (đối với hàng NEW) hoặc đúng tình trạng mô tả (đối với hàng USED).
                        - Hoàn tiền: Tiền sẽ được hoàn về ví hoặc tài khoản ngân hàng trong 3-5 ngày làm việc.
                    """.trimIndent()
                )

                PolicySection(
                    title = "4. Miễn trừ trách nhiệm",
                    content = "StoreProMax là nền tảng trung gian. Chúng tôi không chịu trách nhiệm về các giao dịch diễn ra bên ngoài ứng dụng hoặc các tranh chấp cá nhân giữa người mua và người bán không thông qua hệ thống."
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Bằng việc tiếp tục sử dụng, bạn đồng ý với ")
                        withStyle(style = SpanStyle(color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)) {
                            append("Điều khoản dịch vụ")
                        }
                        append(" của chúng tôi.")
                    },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                color = Color(0xFF424242),
                lineHeight = 22.sp // Tăng khoảng cách dòng cho dễ đọc
            )
        }
    }
}