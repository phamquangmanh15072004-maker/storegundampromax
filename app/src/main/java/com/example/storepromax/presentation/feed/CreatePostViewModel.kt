package com.example.storepromax.presentation.feed

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    var title = mutableStateOf("")
    var content = mutableStateOf("")
    var price = mutableStateOf("")

    var grade = mutableStateOf("HG")
    var condition = mutableStateOf("USED")

    var selectedImages = mutableStateOf<List<Uri>>(emptyList())

    var isLoading = mutableStateOf(false)

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun addImages(newUris: List<Uri>) {
        val currentList = selectedImages.value.toMutableList()
        currentList.addAll(newUris)
        selectedImages.value = currentList.distinct()
    }

    fun removeImage(uri: Uri) {
        val currentList = selectedImages.value.toMutableList()
        currentList.remove(uri)
        selectedImages.value = currentList
    }

    private suspend fun uploadImageToCloudinary(uri: Uri): String? = suspendCancellableCoroutine { continuation ->
        try {
            MediaManager.get().upload(uri)
                .option("folder", "store_promax/user_posts") // (Tùy chọn) Lưu vào thư mục riêng
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        continuation.resume(secureUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        android.util.Log.e("CLOUDINARY", "Lỗi upload: ${error.description}")
                        continuation.resume(null) // Trả về null nếu lỗi
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(null)
        }
    }

    fun createPost() {
        if (title.value.isBlank() || price.value.isBlank() || content.value.isBlank()) {
            viewModelScope.launch { _uiEvent.send("Vui lòng nhập đủ tiêu đề, giá và nội dung!") }
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                _uiEvent.send("Lỗi: Bạn chưa đăng nhập!")
                isLoading.value = false
                return@launch
            }

            val finalUserName = if (!currentUser.displayName.isNullOrBlank()) {
                currentUser.displayName
            } else {
                currentUser.email?.substringBefore("@") ?: "Ẩn danh"
            }
            val uploadedImageUrls = if (selectedImages.value.isNotEmpty()) {
                val uploadJobs = selectedImages.value.map { uri ->
                    async { uploadImageToCloudinary(uri) }
                }
                uploadJobs.awaitAll().filterNotNull()
            } else {
                emptyList()
            }

            if (selectedImages.value.isNotEmpty() && uploadedImageUrls.isEmpty()) {
                _uiEvent.send("Lỗi: Không thể upload ảnh. Vui lòng thử lại!")
                isLoading.value = false
                return@launch
            }

            val keywords = title.value.lowercase().split(" ").filter { it.isNotEmpty() }

            val newPost = Post(
                id = "",
                userId = currentUser.uid,
                userName = finalUserName ?: "NoName",
                userAvatar = currentUser.photoUrl?.toString() ?: "",

                title = title.value,
                content = content.value,
                price = price.value.toLongOrNull() ?: 0,

                images = uploadedImageUrls,

                grade = grade.value,
                condition = condition.value,

                status = "PENDING",
                searchKeywords = keywords,

                createdAt = System.currentTimeMillis()
            )
            val result = postRepository.createPost(newPost)

            if (result.isSuccess) {
                _uiEvent.send("Success")
            } else {
                _uiEvent.send("Lỗi: ${result.exceptionOrNull()?.message}")
            }
            isLoading.value = false
        }
    }
}