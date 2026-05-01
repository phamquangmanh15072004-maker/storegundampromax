package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.BannerModel

interface BannerRepository {
    suspend fun getActiveBanners(): Result<List<BannerModel>>
}