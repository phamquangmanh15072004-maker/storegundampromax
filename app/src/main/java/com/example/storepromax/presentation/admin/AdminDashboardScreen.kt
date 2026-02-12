package com.example.storepromax.presentation.admin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.storepromax.presentation.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

data class AdminMenuItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val adminName by viewModel.adminName.collectAsState()
    val menuItems = listOf(
        AdminMenuItem("Kho Hàng", Icons.Default.Inventory, Color(0xFF388E3C), "admin_products"),
        AdminMenuItem("Duyệt Bài", Icons.Default.VerifiedUser, Color(0xFF512DA8), "admin_feed_approval"),
        AdminMenuItem("Đơn Hàng", Icons.Default.ListAlt, Color(0xFF1976D2), "admin_orders"),
        AdminMenuItem("Thành Viên", Icons.Default.People, Color(0xFFF57C00), "admin_users"),
        AdminMenuItem("Thống Kê", Icons.Default.PieChart, Color(0xFFC2185B), "admin_stats"),
        AdminMenuItem("CSKH", Icons.Default.SupportAgent, Color(0xFF0097A7),"admin_chat_list"),
    )
    LaunchedEffect(Unit) {
        FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_Admin", "Đã đăng ký nhận đơn hàng thành công!")
                } else {
                    Log.e("FCM_Admin", "Đăng ký thất bại", task.exception)
                }
            }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("QUẢN TRỊ VIÊN", fontWeight = FontWeight.Bold)
                        Text("Xin chào, $adminName!", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Đăng xuất", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp) // Chỉnh lại padding tổng thể
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem("Đơn Mới", "${stats.newOrdersCount}", Color(0xFF4CAF50))
                    StatItem("Doanh Thu", viewModel.formatCurrency(stats.totalRevenue), Color(0xFF03A9F4))
                    StatItem("User Tổng", "${stats.totalUsers}", Color(0xFFFFC107))
                }
            }

            Text("CHỨC NĂNG QUẢN LÝ", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(menuItems) { item ->
                    AdminMenuCard(item) {
                        when (item.route) {
                            "admin_products" -> navController.navigate("admin_product_list")
                            "admin_feed_approval" -> navController.navigate("admin_feed_approval")
                            "admin_orders" -> navController.navigate("admin_order")
                            "admin_users" -> navController.navigate("admin_user")
                            "admin_stats" -> navController.navigate("admin_stats")
                            "admin_chat_list" -> navController.navigate("admin_chat_list")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMenuCard(item: AdminMenuItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .height(120.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(item.title, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color.DarkGray, fontSize = 12.sp)
    }
}
