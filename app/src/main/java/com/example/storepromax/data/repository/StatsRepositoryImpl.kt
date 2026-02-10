package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.*
import com.example.storepromax.domain.repository.StatsRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : StatsRepository {

    override suspend fun getDashboardStats(startTime: Long?, endTime: Long?): Result<DashboardStats> {
        return try {
            val ordersSnapshot = firestore.collection("orders").get().await()
            var allOrders = ordersSnapshot.toObjects(Order::class.java)
            if (startTime != null && endTime != null) {
                allOrders = allOrders.filter {
                    it.createdAt in startTime..endTime
                }
            }
            val usersCount = firestore.collection("users").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
            val productsCount = firestore.collection("products").count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count

            val validOrders = allOrders.filter { it.status != "CANCELLED" }
            val totalRevenue = validOrders.sumOf { it.totalPrice }

            val chartData = calculateDynamicChart(validOrders, startTime, endTime)
            val topProducts = calculateTopProducts(validOrders)

            val stats = DashboardStats(
                totalRevenue = totalRevenue,
                totalOrders = allOrders.size,
                totalUsers = usersCount.toInt(),
                totalProducts = productsCount.toInt(),
                revenueChartData = chartData,
                topProducts = topProducts
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDynamicChart(orders: List<Order>, start: Long?, end: Long?): List<ChartPoint> {
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val result = mutableListOf<ChartPoint>()

        if (start == null || end == null) {
            val calendar = Calendar.getInstance()
            for (i in 6 downTo 0) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.DAY_OF_YEAR, -i)
                val dayStr = dateFormat.format(tempCal.time)
                val revenue = orders.filter { dateFormat.format(it.createdAt) == dayStr }.sumOf { it.totalPrice }
                result.add(ChartPoint(dayStr, revenue))
            }
            return result
        }

        val grouped = orders.groupBy { dateFormat.format(Date(it.createdAt)) }

        return grouped.map { (date, listOrders) ->
            ChartPoint(date, listOrders.sumOf { it.totalPrice })
        }.sortedBy { it.label }
    }

    private fun calculateTopProducts(orders: List<Order>): List<TopProduct> {
        val productMap = mutableMapOf<String, Int>()
        val imageMap = mutableMapOf<String, String>()

        orders.forEach { order ->
            order.items.forEach { item ->
                val currentCount = productMap.getOrDefault(item.product.name, 0)
                productMap[item.product.name] = currentCount + item.quantity

                if (!imageMap.containsKey(item.product.name)) {
                    imageMap[item.product.name] = item.product.imageUrl
                }
            }
        }

        return productMap.entries
            .sortedByDescending { it.value }
            .take(5) // Lấy top 5
            .map { TopProduct(it.key, it.value, imageMap[it.key] ?: "") }
    }
}