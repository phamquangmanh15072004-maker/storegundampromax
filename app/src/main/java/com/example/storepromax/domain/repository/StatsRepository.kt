package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.DashboardStats

interface StatsRepository {
    suspend fun getDashboardStats(startTime: Long?, endTime: Long?): Result<DashboardStats>
}