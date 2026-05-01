package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.BannerModel
import com.example.storepromax.domain.repository.BannerRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BannerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BannerRepository {
    override suspend fun getActiveBanners(): Result<List<BannerModel>> {
        return try {
            val snapshot = firestore.collection("banners")
                .whereEqualTo("isActive", true)
                .orderBy("priority", Query.Direction.ASCENDING)
                .limit(5)
                .get()
                .await()
            val banners = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BannerModel::class.java)?.copy(id = doc.id)
            }
            Result.success(banners)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}