package com.example.storepromax.presentation.admin.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminFeedApprovalViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _pendingPosts = MutableStateFlow<List<Post>>(emptyList())
    val pendingPosts = _pendingPosts.asStateFlow()
    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadPendingPosts()
    }

    private fun loadPendingPosts() {
        viewModelScope.launch {
            postRepository.getPendingPosts().collect { posts ->
                _pendingPosts.value = posts
            }
        }
    }

    fun approvePost(post: Post) {
        viewModelScope.launch {
            postRepository.updatePostStatus(post.id, status = "APPROVED")
                .onSuccess { _uiEvent.send("Đã duyệt bài: ${post.title}") }
                .onFailure { _uiEvent.send("Lỗi: ${it.message}") }
        }
    }

    fun rejectPost(post: Post, reason: String) {
        viewModelScope.launch {
            postRepository.updatePostStatus(post.id, status = "REJECTED", reason = reason)
                .onSuccess { _uiEvent.send("Đã từ chối bài: ${post.title}") }
                .onFailure { _uiEvent.send("Lỗi: ${it.message}") }
        }
    }
}