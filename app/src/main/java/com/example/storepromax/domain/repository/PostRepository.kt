package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.model.User
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPendingPosts(): Flow<List<Post>>

    fun getApprovedPosts(): Flow<List<Post>>

    suspend fun createPost(post: Post): Result<Boolean>
    suspend fun updatePostStatus(postId: String, status: String, reason: String? = null): Result<Boolean>

    suspend fun deletePost(postId: String): Result<Boolean>
    suspend fun toggleLike(postId: String, userId: String): Result<Boolean>
    fun getPostsByUser(userId: String): Flow<List<Post>>
    suspend fun getUserInfo(userId: String): Result<User>
    suspend fun addToViewHistory(post: Post)

    fun getViewHistory(): Flow<List<Post>>

    suspend fun clearViewHistory()
}