package com.example.storepromax.presentation.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatRepo: ChatRepository
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = firestore.collection("posts").document(postId).get().await()
                if (doc.exists()) {
                    _post.value = doc.toObject(Post::class.java)?.copy(id = doc.id)
                } else {
                    _post.value = null
                }
            } catch (e: Exception) {
                Log.e("PostDetail", "Lỗi tải bài viết: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun contactSeller(post: Post, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            if (post.userId == currentUserId) return@launch // Không tự chat với chính mình

            val helloMessage =
                "Chào bạn, mình thấy bài đăng: \"${post.title}\" và muốn trao đổi thêm!"

            chatRepo.getOrCreateUserChat(
                targetUserId = post.userId,
                targetUserName = post.userName,
                initialContent = helloMessage
            ).onSuccess { channelId -> onSuccess(channelId) }
        }
    }
}