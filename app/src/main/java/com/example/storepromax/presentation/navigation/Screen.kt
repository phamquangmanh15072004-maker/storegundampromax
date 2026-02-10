package com.example.storepromax.presentation.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome_screen")
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen")
    object Detail : Screen("detail_screen/{productId}") {
        fun createRoute(productId: String) = "detail_screen/$productId"
    }
    object OrderHistory : Screen("order_history_screen?tabIndex={tabIndex}") {
        fun createRoute(tabIndex: Int) = "order_history_screen?tabIndex=$tabIndex"
    }
    object AdminOrderDetail : Screen("admin_order_detail_screen/{orderId}") {
        fun createRoute(orderId: String) = "admin_order_detail_screen/$orderId"
    }
}