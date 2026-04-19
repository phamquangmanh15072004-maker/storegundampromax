package com.example.storepromax.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.NotificationRepository
import com.example.storepromax.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepo: ChatRepository,
    private val notificationRepository: NotificationRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    val posts = combine(
        postRepository.getApprovedPosts(),
        _searchQuery.debounce(300L).distinctUntilChanged()
    ) { firebasePosts, query ->
        _isLoading.value = false

        if (query.isBlank()) {
            firebasePosts.take(100)
        } else {
            val keyword = query.lowercase().trim()
            firebasePosts.filter { post ->
                post.title.lowercase().contains(keyword) ||
                        post.userName.lowercase().contains(keyword) ||
                        post.grade.lowercase().contains(keyword) ||
                        post.content.lowercase().contains(keyword)
            }.take(100)
        }
    }
        .onEach { _isSearching.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
        }
    }
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val userId = currentUserId
            if (userId.isEmpty()) return@launch

            val currentPosts = posts.value
            val targetPost = currentPosts.find { it.id == postId } ?: return@launch

            val isCurrentlyLiked = targetPost.likedByUsers.contains(userId)
            val postOwnerId = targetPost.userId
            val postTitle = targetPost.title

            postRepository.toggleLike(postId, userId)

            if (!isCurrentlyLiked && postOwnerId != userId) {
                try {
                    val ownerDoc = firestore.collection("users").document(postOwnerId).get().await()
                    val ownerToken = ownerDoc.getString("fcmToken") ?: ""

                    val user = auth.currentUser
                    val senderName = user?.displayName.takeIf { !it.isNullOrBlank() } ?: "Ai đó"

                    notificationRepository.sendLikeNotification(
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

    fun onSearchQueryChange(newQuery: String) {
        if (_searchQuery.value == newQuery) return
        _searchQuery.value = newQuery
        _isSearching.value = true
    }
}