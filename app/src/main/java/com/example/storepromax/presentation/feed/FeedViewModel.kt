package com.example.storepromax.presentation.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepo: ChatRepository
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _recentPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    val posts = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .combine(_recentPosts) { query: String, recentPosts: List<Post> ->
            if (query.isBlank()) {
                recentPosts
            } else {
                val keyword = query.lowercase().trim()
                recentPosts.filter { post ->
                    post.title.lowercase().contains(keyword) ||
                            post.userName.lowercase().contains(keyword) ||
                            post.grade.lowercase().contains(keyword) ||
                            post.content.lowercase().contains(keyword)
                }
            }
        }
        .onEach {
            _isSearching.value = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()


    fun loadInitialFeed() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val fetchedPosts = postRepository.getApprovedPosts().first()
                _recentPosts.value = fetchedPosts.sortedByDescending { it.createdAt }.take(100)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePost(postId: String) {

        viewModelScope.launch {
            _recentPosts.value = _recentPosts.value.filter { it.id != postId }
            postRepository.deletePost(postId)
        }
    }

    fun toggleLike(postId: String, context: Context) {
        viewModelScope.launch {
            if (currentUserId.isNotEmpty()) {
                val targetPost = _recentPosts.value.find { it.id == postId } ?: return@launch
                val isCurrentlyLiked = targetPost.likedByUsers.contains(currentUserId)
                val postOwnerId = targetPost.userId
                val postTitle = targetPost.title
                _recentPosts.value = _recentPosts.value.map { post ->
                    if (post.id == postId) {
                        val newLikedByUsers = if (isCurrentlyLiked) {
                            post.likedByUsers - currentUserId
                        } else {
                            post.likedByUsers + currentUserId
                        }
                        val newLikeCount =
                            if (isCurrentlyLiked) post.likeCount - 1 else post.likeCount + 1
                        post.copy(likedByUsers = newLikedByUsers, likeCount = newLikeCount)
                    } else {
                        post
                    }
                }
                postRepository.toggleLike(postId, currentUserId)
                if (!isCurrentlyLiked && postOwnerId != currentUserId) {
                    try {
                        val db = FirebaseFirestore.getInstance()
                        val ownerDoc = db.collection("users").document(postOwnerId).get().await()
                        val ownerToken = ownerDoc.getString("fcmToken") ?: ""
                        val user = FirebaseAuth.getInstance().currentUser
                        val senderName = user?.displayName.takeIf { !it.isNullOrBlank() } ?: "Ai đó"

                        NotificationHelper.sendLikeNotification(
                            context = context,
                            receiverToken = ownerToken,
                            receiverUserId = postOwnerId,
                            senderName = senderName,
                            postTitle = postTitle,
                            postId = postId
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        if (_searchQuery.value == newQuery) return
        _searchQuery.value = newQuery
        _isSearching.value = true
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val fetchedPosts = postRepository.getApprovedPosts().first()
                _recentPosts.value = fetchedPosts.sortedByDescending { it.createdAt }.take(100)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}