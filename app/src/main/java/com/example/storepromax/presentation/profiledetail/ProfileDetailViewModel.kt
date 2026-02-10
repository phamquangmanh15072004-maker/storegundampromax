package com.example.storepromax.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.model.User
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentUserId = auth.currentUser?.uid ?: ""

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts = _userPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadProfileData(targetUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (targetUserId == currentUserId && auth.currentUser != null) {
                val cur = auth.currentUser!!
                _userProfile.value = User(
                    id = cur.uid,
                    name = cur.displayName ?: "Tôi",
                    email = cur.email ?: "",
                    avatarUrl = cur.photoUrl?.toString() ?: "",
                    role = "USER"
                )
            } else {
                try {
                    val result = postRepository.getUserInfo(targetUserId)
                    result.onSuccess { user ->
                        _userProfile.value = user
                    }.onFailure {
                        _userProfile.value = User(
                            id = targetUserId,
                            name = "Người dùng (Chưa cập nhật)",
                            email = "",
                            avatarUrl = "",
                            role = "USER"
                        )
                    }
                } catch (e: Exception) {
                    _userProfile.value = User(id = targetUserId, name = "Lỗi tải tin", email = "", avatarUrl = "", role = "USER")
                }
            }
            postRepository.getPostsByUser(targetUserId).collect { posts ->
                _userPosts.value = posts.sortedByDescending { it.createdAt }
                _isLoading.value = false
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
            postRepository.toggleLike(postId, currentUserId)
        }
    }

    fun contactUser(onSuccess: (String) -> Unit) {
        val targetUser = _userProfile.value ?: return
        if (targetUser.id == currentUserId) return

        viewModelScope.launch {
            chatRepository.getOrCreateUserChat(
                targetUserId = targetUser.id,
                targetUserName = targetUser.name,
                initialContent = "Chào bạn, mình quan tâm đến sản phẩm của bạn."
            ).onSuccess { channelId ->
                onSuccess(channelId)
            }
        }
    }
}