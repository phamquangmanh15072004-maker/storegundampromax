package com.example.storepromax.presentation.feed

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.admin.utils.NotificationHelper
import com.example.storepromax.domain.model.Comment
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.repository.ChatRepository
import com.example.storepromax.domain.repository.PostRepository
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
    private val chatRepo: ChatRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()
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
            if (post.userId == currentUserId) return@launch

            val helloMessage =
                "Chào bạn, mình thấy bài đăng: \"${post.title}\" và muốn trao đổi thêm!"

            chatRepo.getOrCreateUserChat(
                targetUserId = post.userId,
                targetUserName = post.userName,
                initialContent = helloMessage
            ).onSuccess { channelId -> onSuccess(channelId) }
        }
    }
    fun loadPostAndComments(postId: String) {
        loadPost(postId)

        viewModelScope.launch {
            postRepository.getCommentsForPost(postId).collect { fetchedComments ->
                _comments.value = fetchedComments
            }
        }
    }

    fun sendComment(
        context: Context,
        postId: String,
        content: String,
        parentId: String = "",
        replyingToUserId: String = "",
        replyingToName: String = "",
        onSuccess: () -> Unit
    ) {
        if (content.isBlank() || currentUserId.isEmpty()) return

        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val userName = user?.displayName.takeIf { !it.isNullOrBlank() } ?: "Người dùng"
            val userAvatar = user?.photoUrl?.toString() ?: ""

            val newComment = Comment(
                postId = postId,
                userId = currentUserId,
                userName = userName,
                userAvatar = userAvatar,
                content = content.trim(),
                parentId = parentId,
                replyingToName = replyingToName
            )

            postRepository.addComment(postId, newComment).onSuccess {
                onSuccess()
                _post.value = _post.value?.let { it.copy(commentCount = it.commentCount + 1) }
                viewModelScope.launch {
                    try {
                        val db = FirebaseFirestore.getInstance()
                        val postOwnerId = post.value?.userId ?: ""
                        if (replyingToUserId.isNotEmpty() && replyingToUserId != currentUserId) {
                            val originalComment = comments.value.find { it.id == parentId }?.content ?: ""
                            val shortOriginal = if (originalComment.length > 20) originalComment.take(20) + "..." else originalComment

                            val targetUserDoc = db.collection("users").document(replyingToUserId).get().await()
                            val token = targetUserDoc.getString("fcmToken") ?: ""

                            if (token.isNotEmpty()) {
                                NotificationHelper.sendCommentNotification(
                                    context = context,
                                    receiverToken = token,
                                    title = "$userName đã trả lời bình luận của bạn",
                                    body = "Bạn: $shortOriginal\n$userName: $content",
                                    postId = postId,
                                    receiverUserId = replyingToUserId
                                )
                            }
                        }
                        if (postOwnerId.isNotEmpty() && postOwnerId != currentUserId && postOwnerId != replyingToUserId) {

                            val ownerDoc = db.collection("users").document(postOwnerId).get().await()
                            val ownerToken = ownerDoc.getString("fcmToken") ?: ""

                            if (ownerToken.isNotEmpty()) {
                                NotificationHelper.sendCommentNotification(
                                    context = context,
                                    receiverToken = ownerToken,
                                    title = "$userName đã bình luận về bài viết của bạn",
                                    body = content,
                                    postId = postId,
                                    receiverUserId = postOwnerId
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    fun updateComment(commentId: String, newContent: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (newContent.isBlank()) return
        if (commentId.isEmpty()) {
            onError("ID bình luận bị trống!")
            return
        }
        _comments.value = _comments.value.map {
            if (it.id == commentId) it.copy(content = newContent.trim()) else it
        }

        viewModelScope.launch {
            postRepository.updateComment(commentId, newContent.trim())
                .onSuccess { onSuccess() }
                .onFailure { e -> onError(e.message ?: "Lỗi không xác định") }
        }
    }

    fun deleteComment(postId: String, commentId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (commentId.isEmpty()) {
            onError("ID bình luận bị trống!")
            return
        }
        val replies = _comments.value.filter { it.parentId == commentId }
        val totalDeleted = 1 + replies.size
        val idsToDelete = listOf(commentId) + replies.map { it.id }
        _comments.value = _comments.value.filter { it.id !in idsToDelete }
        _post.value = _post.value?.let {
            it.copy(commentCount = maxOf(0, it.commentCount - totalDeleted))
        }
        viewModelScope.launch {
            postRepository.deleteComment(postId, commentId)
                .onSuccess { onSuccess() }
                .onFailure { e -> onError(e.message ?: "Lỗi không xác định") }
        }
    }
}