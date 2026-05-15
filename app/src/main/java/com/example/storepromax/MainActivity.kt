package com.example.storepromax

import AIChatScreen
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cloudinary.android.MediaManager
import com.example.storepromax.presentation.admin.AdminDashboardScreen
import com.example.storepromax.presentation.admin.AdminStatsScreen
import com.example.storepromax.presentation.admin.chat.AdminChatListScreen
import com.example.storepromax.presentation.admin.feed.AdminFeedApprovalScreen
import com.example.storepromax.presentation.admin.order.AdminOrderDetailScreen
import com.example.storepromax.presentation.admin.order.AdminOrderScreen
import com.example.storepromax.presentation.admin.product.AddProductScreen
import com.example.storepromax.presentation.admin.product.AdminProductListScreen
import com.example.storepromax.presentation.admin.user.AdminUserScreen
import com.example.storepromax.presentation.admin.voucher.AdminVoucherFormScreen
import com.example.storepromax.presentation.admin.voucher.AdminVoucherScreen
import com.example.storepromax.presentation.cart.CartScreen
import com.example.storepromax.presentation.chat.ChatDetailScreen
import com.example.storepromax.presentation.chat.UserChatListScreen
import com.example.storepromax.presentation.checkout.CheckoutScreen
import com.example.storepromax.presentation.detail.DetailScreen
import com.example.storepromax.presentation.detail.Model3DScreen
import com.example.storepromax.presentation.feed.CreatePostScreen
import com.example.storepromax.presentation.feed.FeedScreen
import com.example.storepromax.presentation.feed.PostDetailScreen
import com.example.storepromax.presentation.login.LoginScreen
import com.example.storepromax.presentation.main.MainScreen
import com.example.storepromax.presentation.main.MainViewModel
import com.example.storepromax.presentation.myreview.MyReviewScreen
import com.example.storepromax.presentation.myvoucher.MyVoucherScreen
import com.example.storepromax.presentation.navigation.Screen
import com.example.storepromax.presentation.notification.NotificationScreen
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
import com.example.storepromax.presentation.writereview.WriteReviewScreen
import com.example.storepromax.presentation.writereview.WriteReviewViewModel
import com.example.storepromax.ui.theme.StorePromaxTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class DeepLinkData(
    val type: String?,
    val orderId: String? = null,
    val channelId: String? = null,
    val postId: String? = null,
    val targetId: String? = null,
    val action: String? = null
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Bạn cần bật quyền thông báo để nhận cập nhật đơn hàng và tin nhắn.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private var deepLinkData by mutableStateOf<DeepLinkData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.create(this)
        requestNotificationPermissionIfNeeded()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        setContent {
            StorePromaxTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val mainViewModel: MainViewModel = hiltViewModel()
                val userStatus by mainViewModel.userStatus.collectAsState()

                val firebaseAuth = FirebaseAuth.getInstance()

                LaunchedEffect(deepLinkData) {
                    deepLinkData?.let { data ->
                        try {
                            if (data.action == "NAVIGATE_TO_REVIEW" && !data.orderId.isNullOrEmpty()) {
                                navController.navigate("write_review_screen/${data.orderId}")
                            } else {
                                when (data.type) {
                                    null, "" -> {
                                        when {
                                            !data.channelId.isNullOrEmpty() -> {
                                                navController.navigate("chat_detail/${data.channelId}") { launchSingleTop = true }
                                            }
                                            !data.orderId.isNullOrEmpty() -> {
                                                navController.navigate("order_history_screen/0") { launchSingleTop = true }
                                            }
                                            !data.postId.isNullOrEmpty() || !data.targetId.isNullOrEmpty() -> {
                                                val postId = data.postId ?: data.targetId
                                                navController.navigate("post_detail/$postId") { launchSingleTop = true }
                                            }
                                        }
                                    }
                                    "ORDER_UPDATE" -> {
                                        if (!data.orderId.isNullOrEmpty()) {
                                            navController.navigate("order_history_screen/0") { launchSingleTop = true }
                                        }
                                    }
                                    "NEW_ORDER" -> {
                                        if (!data.orderId.isNullOrEmpty()) {
                                            navController.navigate("admin_order_detail/${data.orderId}") { launchSingleTop = true }
                                        }
                                    }
                                    "CHAT_MESSAGE" -> {
                                        if (!data.channelId.isNullOrEmpty()) {
                                            navController.navigate("chat_detail/${data.channelId}")
                                        }
                                    }
                                    "CHAT" -> {
                                        if (!data.channelId.isNullOrEmpty()) {
                                            navController.navigate("chat_detail/${data.channelId}")
                                        }
                                    }
                                    "CHAT_ADMIN" -> {
                                        if (!data.channelId.isNullOrEmpty()) {
                                            navController.navigate("admin_chat_detail/${data.channelId}")
                                        }
                                    }
                                    "COMMENT", "LIKE" -> {
                                        val postId = data.postId ?: data.targetId
                                        if (!postId.isNullOrEmpty()) {
                                            navController.navigate("post_detail/$postId") { launchSingleTop = true }
                                        }
                                    }
                                }
                            }
                            deepLinkData = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (firebaseAuth.currentUser != null) {
                        saveFCMTokenToFirestore()
                    }
                }
                LaunchedEffect(userStatus.isLocked) {
                    if (userStatus.isLocked && firebaseAuth.currentUser != null) {
                        mainViewModel.logout {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                            Toast.makeText(
                                context,
                                "Tài khoản bị khóa: ${userStatus.reason}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                val startDestination = if (firebaseAuth.currentUser != null) {
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
                        OrderHistoryScreen(
                            navController = navController,
                            initialTabIndex = tabIndex
                        )
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
                        route = "admin_order_detail/{orderId}",
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                        AdminOrderDetailScreen(navController = navController, orderId = orderId)
                    }
                    composable("admin_user") { AdminUserScreen(navController = navController) }
                    composable("admin_stats") { AdminStatsScreen(navController = navController) }
                    composable("admin_chat_list") { AdminChatListScreen(navController = navController) }
                    composable("user_chat_list") { UserChatListScreen(navController = navController) }
                    composable(
                        route = "chat_detail/{channelId}",
                        arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChatDetailScreen(navController = navController, channelId = channelId)
                    }
                    composable(
                        route = "admin_chat_detail/{channelId}",
                        arguments = listOf(navArgument("channelId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        ChatDetailScreen(navController = navController, channelId = channelId)
                    }
                    composable(
                        route = "profile_detail/{userId}",
                        arguments = listOf(
                            navArgument("userId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        ProfileDetailScreen(navController = navController, targetUserId = userId)
                    }
                    composable("edit_profile") { EditProfileScreen(navController = navController) }
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
                        route = "checkout_screen?discountCode={discountCode}&freeshipCode={freeshipCode}&productId={productId}&quantity={quantity}",
                        arguments = listOf(
                            navArgument("discountCode") { type = NavType.StringType; defaultValue = "" },
                            navArgument("freeshipCode") { type = NavType.StringType; defaultValue = "" },
                            navArgument("productId") { type = NavType.StringType; nullable = true; defaultValue = null },
                            navArgument("quantity") { type = NavType.IntType; defaultValue = 1 }
                        )
                    ) { backStackEntry ->
                        val dCode = backStackEntry.arguments?.getString("discountCode")
                        val fCode = backStackEntry.arguments?.getString("freeshipCode")
                        val productId = backStackEntry.arguments?.getString("productId")
                        val quantity = backStackEntry.arguments?.getInt("quantity") ?: 1
                        CheckoutScreen(
                            navController = navController,
                            discountCode = dCode,
                            freeshipCode = fCode,
                            productId = productId,
                            quantity = quantity
                        )
                    }
                    composable(
                        route = "cart_screen?showBack={showBack}",
                        arguments = listOf(
                            navArgument("showBack") { type = NavType.BoolType; defaultValue = false }
                        )
                    ) { backStackEntry ->
                        val showBack = backStackEntry.arguments?.getBoolean("showBack") ?: false
                        CartScreen(navController = navController, showBackBtn = showBack)
                    }
                    composable("recentlyviewed_screen") { RecentlyViewedScreen(navController = navController) }
                    composable("profile_tab") {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        if (currentUserId.isNotEmpty()) {
                            ProfileDetailScreen(navController = navController, targetUserId = currentUserId)
                        }
                    }
                    composable(
                        route = "product_detail/{productId}",
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { DetailScreen(navController = navController) }
                    composable("change_password") { ChangePasswordScreen(navController = navController) }
                    composable("privacy_policy") { TermsPolicyScreen(navController = navController) }
                    composable("about_us") { AboutScreen(navController = navController) }
                    composable("wishlist") { WishlistScreen(navController = navController) }
                    composable(
                        route = "write_review_screen/{orderId}",
                        arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                    ) {
                        val viewModel: WriteReviewViewModel = hiltViewModel()
                        val productsToReview by viewModel.productsToReview.collectAsState()
                        val isLoading by viewModel.isLoading.collectAsState()

                        WriteReviewScreen(
                            navController = navController,
                            productsToReview = productsToReview,
                            isLoadingFromVM = isLoading,
                            onSubmitReview = { productId, rating, text, images, onResult ->
                                viewModel.submitReview(productId, rating, text, images, onResult)
                            }
                        )
                    }
                    composable("notification_screen") { NotificationScreen(navController = navController) }
                    composable("ai_chat_screen") { AIChatScreen(navController = navController) }
                    composable(
                        route = "post_detail/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId") ?: ""
                        PostDetailScreen(navController = navController, postId = postId)
                    }
                    composable(route ="my_voucher_screen"){ MyVoucherScreen(navController = navController) }
                    composable(route ="admin_voucher"){ AdminVoucherScreen(navController = navController) }
                    composable("admin_voucher_form") { AdminVoucherFormScreen(navController = navController, voucherId = null) }
                    composable(
                        route = "admin_voucher_form/{voucherId}",
                        arguments = listOf(navArgument("voucherId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("voucherId")
                        AdminVoucherFormScreen(navController = navController, voucherId = id)
                    }
                    composable("edit_post/{postId}") { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId")
                        CreatePostScreen(navController = navController, postId = postId)
                    }
                    composable(
                        route = "order_history_screen/{tabIndex}",
                        arguments = listOf(navArgument("tabIndex") { type = NavType.IntType; defaultValue = 0 })
                    ) { backStackEntry ->
                        val tabIndex = backStackEntry.arguments?.getInt("tabIndex") ?: 0
                        OrderHistoryScreen(navController = navController, initialTabIndex = tabIndex)
                    }
                    composable("my_reviews") { MyReviewScreen(navController = navController) }
                }
            }
        }
        saveFCMTokenToFirestore()
        initCloudinary()
        handleIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permissionState == PackageManager.PERMISSION_GRANTED) return

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
            val channelId = bundle.getString("channelId")
            val postId = bundle.getString("postId")
            val targetId = bundle.getString("targetId")
            val action = bundle.getString("action")
            if (type != null || action != null || !orderId.isNullOrEmpty() || !channelId.isNullOrEmpty() || !postId.isNullOrEmpty() || !targetId.isNullOrEmpty()) {
                deepLinkData = DeepLinkData(type, orderId, channelId, postId, targetId, action)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
    }
}
