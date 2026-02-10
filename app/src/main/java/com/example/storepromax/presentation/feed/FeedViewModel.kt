package com.example.storepromax.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepo: ChatRepository
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts = _posts.asStateFlow()
    val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        getApprovedPosts()
    }

    private fun getApprovedPosts() {
        viewModelScope.launch {
            postRepository.getApprovedPosts().collect { fetchedPosts ->
                _posts.value = fetchedPosts.sortedByDescending { it.createdAt }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            if (currentUserId.isNotEmpty()) {
                postRepository.toggleLike(postId, currentUserId)
            }
        }
    }

    fun contactSeller(post: Post, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            if (post.userId == currentUserId) return@launch

            val helloMessage = "Chào bạn, mình thấy bài đăng: \"${post.title}\" và muốn trao đổi thêm!"

            chatRepo.getOrCreateUserChat(
                targetUserId = post.userId,
                targetUserName = post.userName,
                initialContent = helloMessage
            ).onSuccess { channelId -> onSuccess(channelId) }
        }
    }
}