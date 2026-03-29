package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.Comment
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.model.User
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getPendingPosts(): Flow<List<Post>>

    fun getApprovedPosts(): Flow<List<Post>>

    suspend fun createPost(post: Post): Result<Boolean>
    suspend fun updatePostStatus(postId: String, status: String, reason: String? = null): Result<Boolean>

    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun toggleLike(postId: String, userId: String): Result<Boolean>
    fun getPostsByUser(userId: String): Flow<List<Post>>
    suspend fun getUserInfo(userId: String): Result<User>
    suspend fun addToViewHistory(post: Post)

    fun getViewHistory(): Flow<List<Post>>

    suspend fun clearViewHistory()
    fun searchPosts(query: String): Flow<List<Post>>
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    suspend fun addComment(postId: String, comment: Comment): Result<Unit>
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
    suspend fun updateComment(commentId: String, newContent: String): Result<Unit>
}