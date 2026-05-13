package com.example.storepromax.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.presentation.main.MainViewModel
import com.example.storepromax.presentation.navigation.Screen
import com.google.firebase.auth.FirebaseAuth

val BrandBlue = Color(0xFF006AF5)
val BrandBlueDark = Color(0xFF004ECB)
val BgColor = Color(0xFFF5F7FA)
val CardColor = Color.White
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF757575)

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val name = currentUser?.displayName ?: "Khách hàng thân thiết"
    val email = currentUser?.email ?: "Chưa cập nhật email"
    val avatarUrl = currentUser?.photoUrl?.toString()

    val pendingCount by viewModel.pendingCount.collectAsState()
    val shippingCount by viewModel.shippingCount.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(scrollState)
    ) {
        // --- HEADER CÁ NHÂN ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BrandBlue, BrandBlueDark)
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { navController.navigate("profile_detail") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = email,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-40).dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Đơn mua của tôi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "Lịch sử mua hàng >",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.clickable { navController.navigate(Screen.OrderHistory.createRoute(0)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFF5F5F5))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OrderStatusItemModern(Icons.Outlined.Assignment, "Chờ duyệt", pendingCount, Color(0xFFFF9800)) {
                            navController.navigate(Screen.OrderHistory.createRoute(1))
                        }
                        OrderStatusItemModern(Icons.Outlined.LocalShipping, "Vận chuyển", shippingCount, BrandBlue) {
                            navController.navigate(Screen.OrderHistory.createRoute(3))
                        }
                        OrderStatusItemModern(Icons.Outlined.CheckCircle, "Đánh giá", 0, Color(0xFF4CAF50)) {
                            navController.navigate(Screen.OrderHistory.createRoute(4)) // 🌟 Nút này trỏ đến Đơn hàng chờ đánh giá
                        }
                        OrderStatusItemModern(Icons.Outlined.AssignmentReturn, "Đổi trả", 0, TextSecondary) {
                            navController.navigate(Screen.OrderHistory.createRoute(5))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            ModernSectionTitle("TIỆN ÍCH CỦA TÔI")
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ModernMenuItem(Icons.Default.Favorite, Color(0xFFFF3B30), "Sản phẩm yêu thích") {
                        navController.navigate("wishlist")
                    }
                    ModernDivider()
                    ModernMenuItem(Icons.Default.ConfirmationNumber, Color(0xFFFF9800), "Kho Voucher") {
                        navController.navigate("my_voucher_screen")
                    }
                    ModernDivider()
                    ModernMenuItem(Icons.Default.Star, Color(0xFFFFC107), "Đánh giá của tôi") {
                        navController.navigate("my_reviews")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            ModernSectionTitle("TÀI KHOẢN")
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ModernMenuItem(Icons.Default.Person, Color(0xFF2196F3), "Thông tin cá nhân") {
                        navController.navigate("profile_detail/${currentUser?.uid}")
                    }
                    ModernDivider()
                    ModernMenuItem(Icons.Default.LockReset, Color(0xFFE91E63), "Đổi mật khẩu") {
                        navController.navigate("change_password")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            ModernSectionTitle("ỨNG DỤNG")
            Card(
                colors = CardDefaults.cardColors(containerColor = CardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ModernMenuItem(Icons.Default.Security, Color(0xFF607D8B), "Chính sách & Bảo mật") {
                        navController.navigate("privacy_policy")
                    }
                    ModernDivider()
                    ModernMenuItem(Icons.Default.Info, Color(0xFF9C27B0), "Về ứng dụng") {
                        navController.navigate("about_us")
                    }
                }
            }
            if (isAdmin) {
                Spacer(modifier = Modifier.height(24.dp))
                ModernSectionTitle("QUẢN TRỊ VIÊN")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModernMenuItem(Icons.Default.Dashboard, Color(0xFF2E7D32), "Admin Dashboard") {
                        navController.navigate("admin_dashboard")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    mainViewModel.logout {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B30)),
                border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
@Composable
fun ModernSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun ModernDivider() {
    Divider(
        color = Color(0xFFF5F5F5),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 68.dp)
    )
}

@Composable
fun ModernMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFE0E0E0),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusItemModern(
    icon: ImageVector,
    label: String,
    badge: Int,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null
        ) { onClick() }
    ) {
        BadgedBox(
            badge = {
                if (badge > 0) {
                    Badge(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White,
                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                    ) { Text(if (badge > 99) "99+" else "$badge", fontWeight = FontWeight.Bold) }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}