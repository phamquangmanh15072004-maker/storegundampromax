package com.example.storepromax

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cloudinary.android.MediaManager
import com.example.storepromax.presentation.admin.AdminDashboardScreen
import com.example.storepromax.presentation.admin.AdminOrderDetailScreen
import com.example.storepromax.presentation.admin.AdminOrderScreen
import com.example.storepromax.presentation.admin.AdminStatsScreen
import com.example.storepromax.presentation.admin.chat.AdminChatListScreen
import com.example.storepromax.presentation.admin.feed.AdminFeedApprovalScreen
import com.example.storepromax.presentation.admin.product.AddProductScreen
import com.example.storepromax.presentation.admin.product.AdminProductListScreen
import com.example.storepromax.presentation.admin.user.AdminUserScreen
import com.example.storepromax.presentation.cart.CartScreen
import com.example.storepromax.presentation.chat.ChatDetailScreen
import com.example.storepromax.presentation.chat.UserChatListScreen
import com.example.storepromax.presentation.checkout.CheckoutScreen
import com.example.storepromax.presentation.detail.DetailScreen
import com.example.storepromax.presentation.detail.Model3DScreen
import com.example.storepromax.presentation.feed.CreatePostScreen
import com.example.storepromax.presentation.feed.FeedScreen
import com.example.storepromax.presentation.login.LoginScreen
import com.example.storepromax.presentation.main.MainScreen
import com.example.storepromax.presentation.navigation.Screen
import com.example.storepromax.presentation.order.OrderHistoryScreen
import com.example.storepromax.presentation.profile.AboutScreen
import com.example.storepromax.presentation.profile.ChangePasswordScreen
import com.example.storepromax.presentation.profile.ProfileDetailScreen
import com.example.storepromax.presentation.profile.RecentlyViewedScreen
import com.example.storepromax.presentation.profile.TermsPolicyScreen
import com.example.storepromax.presentation.profile.edit.EditProfileScreen
import com.example.storepromax.presentation.register.RegisterScreen
import com.example.storepromax.presentation.search.SearchScreen
import com.example.storepromax.presentation.welcome.WelcomeScreen
import com.example.storepromax.presentation.wishlist.WishlistScreen
import com.example.storepromax.ui.theme.StorePromaxTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.jar.Manifest

data class DeepLinkData(val type: String?, val orderId: String?)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLinkData by mutableStateOf<DeepLinkData?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StorePromaxTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val currentUser = FirebaseAuth.getInstance().currentUser
                val mainViewModel: MainViewModel = hiltViewModel()
                val isBanned by mainViewModel.isUserBanned.collectAsState()
                LaunchedEffect(deepLinkData) {
                    deepLinkData?.let { data ->
                        if (data.type == "ORDER_UPDATE" && !data.orderId.isNullOrEmpty()) {
                            try {
                                navController.navigate("order_detail/${data.orderId}")
                                deepLinkData = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                if(currentUser != null){
                    LaunchedEffect(isBanned) {
                        if (isBanned) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                            Toast.makeText(context, "Phiên đăng nhập hết hạn hoặc tài khoản bị khóa!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                    Screen.Home.route
                } else {
                    Screen.Welcome.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(400)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(400)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    }
                ) {
                    composable(Screen.Welcome.route) { WelcomeScreen(navController) }
                    composable(Screen.Login.route) { LoginScreen(navController) }
                    composable(Screen.Register.route) { RegisterScreen(navController) }
                    composable(Screen.Home.route) { MainScreen(navController) }

                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { DetailScreen(navController = navController) }

                    composable("search") { SearchScreen(navController = navController) }
                    composable("admin_dashboard") { AdminDashboardScreen(navController = navController) }
                    composable("admin_feed_approval") { AdminFeedApprovalScreen(navController = navController) }
                    composable("create_post") { CreatePostScreen(navController = navController) }
                    composable("feed") { FeedScreen(navController = navController) }
                    composable("admin_product_list") { AdminProductListScreen(navController) }
                    composable("add_product") { AddProductScreen(navController) }

                    composable(
                        route = "add_product?productId={productId}",
                        arguments = listOf(
                            navArgument("productId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString("productId")
                        AddProductScreen(navController = navController, productId = productId)
                    }

                    composable(
                        route = Screen.OrderHistory.route,
                        arguments = listOf(
                            navArgument("tabIndex") { type = NavType.IntType; defaultValue = 0 }
                        )
                    ) { backStackEntry ->
                        val tabIndex = backStackEntry.arguments?.getInt("tabIndex") ?: 0
                        OrderHistoryScreen(navController = navController, initialTabIndex = tabIndex)
                    }
                    composable(
                        route = "order_detail/{orderId}",
                        arguments = listOf(
                            navArgument("orderId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                        AdminOrderDetailScreen(navController = navController, orderId = orderId)
                    }

                    composable("admin_order") { AdminOrderScreen(navController = navController) }

                    composable(
                        route = Screen.AdminOrderDetail.route,
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                        AdminOrderDetailScreen(navController = navController, orderId = orderId)
                    }

                    composable("admin_user") { AdminUserScreen(navController = navController) }
                    composable("admin_stats") { AdminStatsScreen(navController = navController) }
                    composable("admin_chat_list") {
                        AdminChatListScreen(navController = navController)
                    }
                    composable("user_chat_list") {
                        UserChatListScreen(navController = navController)
                    }
                    composable(
                        route = "chat_detail/{channelId}",
                        arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""

                        ChatDetailScreen(
                            navController = navController,
                            channelId = channelId,
                            isAdminView = false
                        )
                    }
                    composable(
                        route = "admin_chat_detail/{channelId}",
                        arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""

                        ChatDetailScreen(
                            navController = navController,
                            channelId = channelId,
                            isAdminView = true
                        )
                    }
                    composable(
                        route = "profile_detail/{userId}",
                        arguments = listOf(
                            navArgument("userId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        ProfileDetailScreen(
                            navController = navController,
                            targetUserId = userId
                        )
                    }

                    composable("edit_profile") {
                        EditProfileScreen(navController = navController)
                    }
                    composable(
                        route = "model_3d/{url}",
                        arguments = listOf(navArgument("url") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                        val decodedUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                        Model3DScreen(
                            glbUrl = decodedUrl,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "checkout_screen?productId={productId}&quantity={quantity}",
                        arguments = listOf(
                            navArgument("productId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("quantity") {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString("productId")
                        val quantity = backStackEntry.arguments?.getInt("quantity") ?: -1

                        CheckoutScreen(
                            navController = navController,
                            productId = productId,
                            quantity = if (quantity != -1) quantity else null
                        )
                    }
                    composable("cart_screen") {
                        CartScreen(navController = navController)
                    }
                    composable("recentlyviewed_screen") {
                        RecentlyViewedScreen(navController = navController)
                    }
                    composable("profile_tab") {
                        val currentUserId =
                            FirebaseAuth.getInstance().currentUser?.uid
                                ?: ""
                        if (currentUserId.isNotEmpty()) {
                            ProfileDetailScreen(
                                navController = navController,
                                targetUserId = currentUserId
                            )
                        } else {
                        }
                    }
                    composable(
                        route = "product_detail/{productId}",
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString("productId") ?: ""
                        DetailScreen(
                            navController = navController,
                        )
                    }
                    composable("change_password") {
                        ChangePasswordScreen(navController = navController)
                    }
                    composable("privacy_policy") {
                        TermsPolicyScreen(navController = navController)
                    }
                    composable("about_us") {
                        AboutScreen(navController = navController)
                    }
                    composable("wishlist") {
                        WishlistScreen(navController = navController)
                    }
                }
            }
        }
        saveFCMTokenToFirestore()
        initCloudinary()
        handleIntent(intent)
    }
    private fun initCloudinary() {
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = "djk7z1i0w"
            config["api_key"] = "173273377241456"
            config["api_secret"] = "MKSoEnz1YCdN7C2mXu963i_po5U"
            MediaManager.init(this, config)
        } catch (e: Exception) {
        }
    }
    private fun saveFCMTokenToFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection("users").document(currentUser.uid)
                    userRef.set(mapOf("fcmToken" to token), SetOptions.merge())
                }
            }
        }
    }
    private fun handleIntent(intent: Intent?) {
        intent?.extras?.let { bundle ->
            val type = bundle.getString("type")
            val orderId = bundle.getString("orderId")

            if (type != null && orderId != null) {
                deepLinkData = DeepLinkData(type, orderId)
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}