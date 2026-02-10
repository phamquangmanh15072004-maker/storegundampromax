package com.example.storepromax.domain.model

data class DashboardStats(
    val totalRevenue: Long = 0,
    val totalOrders: Int = 0,
    val totalUsers: Int = 0,
    val totalProducts: Int = 0,
    val revenueChartData: List<ChartPoint> = emptyList(),
    val topProducts: List<TopProduct> = emptyList()
)

data class ChartPoint(
    val label: String,
    val value: Long
)

data class TopProduct(
    val name: String,
    val soldCount: Int,
    val imageUrl: String
)