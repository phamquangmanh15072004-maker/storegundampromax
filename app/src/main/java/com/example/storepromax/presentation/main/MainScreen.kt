package com.example.storepromax.presentation.main

import androidx.activity.ComponentActivity // 🌟 THÊM IMPORT NÀY
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.storepromax.presentation.cart.CartScreen
import com.example.storepromax.presentation.chat.UserChatListScreen
import com.example.storepromax.presentation.feed.FeedScreen
import com.example.storepromax.presentation.home.HomeScreen
import com.example.storepromax.presentation.profile.ProfileScreen

val PrimaryBlue = Color(0xFF006AF5)
val IconGray = Color(0xFF757575)

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun MainScreen(
    rootNavController: NavController,
    mainViewModel: MainViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val navController = rememberNavController()

    val unreadChatCount by mainViewModel.unreadChatCount.collectAsState(initial = 0)

    val items = remember(unreadChatCount) {
        listOf(
            BottomNavItem("Trang chủ", "home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("Tin nhắn", "chat", Icons.Filled.Chat, Icons.Outlined.Chat, badgeCount = unreadChatCount),
            BottomNavItem("Khám phá", "feed", Icons.Filled.Newspaper, Icons.Outlined.Newspaper),
            BottomNavItem("Giỏ hàng", "cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
            BottomNavItem("Tài khoản", "profile", Icons.Filled.Person, Icons.Outlined.Person),
        )
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "home"

            CurvedBottomBar(
                items = items,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    if (item.route == currentRoute) {
                        // Gọi sự kiện cuộn trang từ cùng 1 ViewModel với HomeScreen
                        if (item.route == "home" || item.route == "feed") {
                            mainViewModel.triggerScrollToTop(item.route)
                        }
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController = rootNavController) }
            composable("feed") { FeedScreen(rootNavController) }
            composable("cart") { CartScreen(rootNavController) }
            composable("profile") { ProfileScreen(rootNavController) }
            composable("chat") { UserChatListScreen(rootNavController) }
        }
    }
}

@Composable
fun CurvedBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (BottomNavItem) -> Unit
) {
    val barHeight = 64.dp
    val fabSize = 56.dp
    val isCenterSelected = currentRoute == items[2].route
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = 8.dp,
                    shape = BottomNavCurveShape(radius = 32.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.1f),
                    shape = BottomNavCurveShape(radius = 32.dp)
                ),
            color = Color.White,
            shape = BottomNavCurveShape(radius = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    if (index == 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            StandardBottomNavItem(
                                item = item,
                                isSelected = currentRoute == item.route && !isCenterSelected,
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }

        val centerItem = items[2]
        val fabScale by animateFloatAsState(
            targetValue = if (isCenterSelected) 1.08f else 1f,
            animationSpec = if (isCenterSelected) tween(180, easing = FastOutSlowInEasing) else tween(220, easing = LinearOutSlowInEasing),
            label = "fabScale"
        )
        val fabElevation by animateDpAsState(
            targetValue = if (isCenterSelected) 10.dp else 6.dp,
            animationSpec = if (isCenterSelected) tween(180, easing = FastOutSlowInEasing) else tween(220, easing = LinearOutSlowInEasing),
            label = "fabElevation"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-fabSize / 2))
                .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                .shadow(elevation = fabElevation, shape = CircleShape)
                .size(fabSize + 8.dp)
                .background(Color.White, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (isCenterSelected) PrimaryBlue else Color(0xFFF5F5F5))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onItemClick(centerItem) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCenterSelected) centerItem.selectedIcon else centerItem.unselectedIcon,
                tint = if (isCenterSelected) Color.White else IconGray,
                modifier = Modifier.size(28.dp),
                contentDescription = null
            )
        }
    }
}

class BottomNavCurveShape(private val radius: androidx.compose.ui.unit.Dp) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        return androidx.compose.ui.graphics.Outline.Generic(
            path = Path().apply {
                val radiusPx = with(density) { radius.toPx() }
                val width = size.width
                val height = size.height
                val centerX = width / 2
                moveTo(0f, 0f)
                lineTo(centerX - radiusPx * 2, 0f)
                cubicTo(centerX - radiusPx, 0f, centerX - radiusPx, -radiusPx * 0.8f, centerX, -radiusPx * 0.8f)
                cubicTo(centerX + radiusPx, -radiusPx * 0.8f, centerX + radiusPx, 0f, centerX + radiusPx * 2, 0f)
                lineTo(width, 0f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
        )
    }
}

@Composable
fun StandardBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(targetValue = if (isSelected) PrimaryBlue else IconGray, animationSpec = tween(durationMillis = 300), label = "color")
    val iconScale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "scale")
    val iconTranslationY by animateDpAsState(targetValue = if (isSelected) (-2).dp else 0.dp, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "translationY")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = contentColor,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        translationY = iconTranslationY.toPx()
                    }
            )
            if (item.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-2).dp)
                        .defaultMinSize(minWidth = 16.dp)
                        .height(16.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.badgeCount > 9) "9+" else item.badgeCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .height(16.dp)
                .offset(y = (-4).dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Text(
                    text = item.title, color = contentColor, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, lineHeight = 10.sp
                )
            }
        }
    }
}