package com.example.storepromax.presentation.chat_ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.data.api.AiChatHistoryItem
import com.example.storepromax.data.api.AiChatRequest
import com.example.storepromax.data.api.GunplaBackendApi
import com.example.storepromax.domain.model.ChatMessageAI
import com.example.storepromax.domain.model.Post
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import kotlin.coroutines.resume

private class AiBackendException(
    val httpCode: Int,
    val errorCode: String,
    override val message: String
) : Exception(message)

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val backendApi: GunplaBackendApi
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessageAI>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var cachedProductsInKho: List<Product> = emptyList()
    private var cachedPostsInMarket: List<Post> = emptyList()
    private val currentUserId: String? = auth.currentUser?.uid
    private var isAiReady = false
    private var aiJob: Job? = null

    init {
        loadChatHistoryAndInitAI()
    }

    fun stopGenerating() {
        aiJob?.cancel()
        _isLoading.value = false
    }

    suspend fun uploadImageToCloudinary(uri: Uri): String? =
        suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(uri)
                .unsigned(CLOUDINARY_UNSIGNED_PRESET)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) = Unit
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) = Unit
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (continuation.isActive) continuation.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("AIChat", "Cloudinary upload failed: ${error.description}")
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) = Unit
                })
                .dispatch()
        }

    private fun loadChatHistoryAndInitAI() {
        val userId = currentUserId ?: run {
            _messages.value = listOf(
                ChatMessageAI(
                    content = "Bạn cần đăng nhập để GunplaAI lưu lịch sử chat và tư vấn theo đơn hàng của bạn.",
                    isFromUser = false
                )
            )
            isAiReady = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                ensureAiBackendReady()
                val (_, allProductsInKho) = fetchRelevantProductsData()
                val (_, allPosts) = fetchMarketplacePostsData()
                cachedProductsInKho = allProductsInKho
                cachedPostsInMarket = allPosts

                val snapshot = firestore.collection("users").document(userId)
                    .collection("ai_chats")
                    .orderBy("timestamp")
                    .limitToLast(MAX_STORED_HISTORY)
                    .get()
                    .await()

                val loadedMessages = snapshot.documents.mapNotNull { doc ->
                    val content = doc.getString("content") ?: ""
                    val isFromUser = doc.getBoolean("isFromUser") ?: true
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val hasGoToCartButton = doc.getBoolean("hasGoToCartButton") ?: false
                    val userImageUrl = doc.getString("userImageUrl")
                    val attachedProductIds = (doc.get("attachedProductIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    val attachedPostIds = (doc.get("attachedPostIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    val attachedProducts = allProductsInKho.filter { it.id in attachedProductIds }
                    val attachedPosts = attachedPostIds.map { postId ->
                        allPosts.find { it.id == postId } ?: Post(
                            id = postId,
                            title = "Bài viết đã bị xóa hoặc ẩn",
                            price = 0,
                            userName = "Hệ thống",
                            status = "DELETED",
                            images = emptyList()
                        )
                    }

                    ChatMessageAI(
                        id = doc.id,
                        content = content,
                        isFromUser = isFromUser,
                        timestamp = timestamp,
                        attachedProducts = attachedProducts,
                        attachedPosts = attachedPosts,
                        hasGoToCartButton = hasGoToCartButton,
                        userImageUrl = userImageUrl
                    )
                }

                if (loadedMessages.isEmpty()) {
                    val welcomeMsg = ChatMessageAI(
                        content = "Chào bạn! Mình là GunplaAI, trợ lý tư vấn Gunpla của cửa hàng. Bạn muốn tìm kit theo grade, ngân sách, độ khó lắp hay cần kiểm tra đơn hàng thì nhắn mình nhé.",
                        isFromUser = false
                    )
                    _messages.value = listOf(welcomeMsg)
                    saveMessageToFirebase(welcomeMsg)
                } else {
                    _messages.value = loadedMessages
                }

                isAiReady = true
            } catch (e: Exception) {
                Log.e("AIChat", "Không thể khởi tạo AI: ${e.message}", e)
                isAiReady = false
                _messages.value = listOf(
                    ChatMessageAI(
                        content = "GunplaAI đang gặp lỗi kết nối dữ liệu. Bạn thử mở lại màn chat sau ít phút nhé.",
                        isFromUser = false
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchRelevantProductsData(): Pair<String, List<Product>> {
        var productList = emptyList<Product>()

        try {
            productRepository.getProducts().onSuccess { list ->
                productList = list
                    .filter { it.isActive && it.stock > 0 }
                    .sortedWith(
                        compareByDescending<Product> { it.isFeatured }
                            .thenByDescending { it.sold }
                            .thenByDescending { it.rating }
                            .thenByDescending { it.createdAt }
                    )
            }
        } catch (e: Exception) {
            Log.e("AIChat", "Không thể đọc sản phẩm cho AI: ${e.message}", e)
        }
        return Pair("", productList)
    }

    fun sendMessage(
        userContent: String,
        imageUri: Uri? = null,
        imageUrl: String? = null,
        context: Context? = null
    ) {
        if (userContent.isBlank() && imageUri == null) return
        if (_isLoading.value) return
        if (!isAiReady) {
            appendAndSaveSystemMessage("GunplaAI chưa sẵn sàng. Bạn thử thoát màn chat rồi vào lại giúp mình nhé.")
            return
        }

        aiJob = viewModelScope.launch {
            _isLoading.value = true
            var bitmap: Bitmap? = null
            if (imageUri != null && context != null) {
                try {
                    bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, imageUri)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                    }
                } catch (e: Exception) {
                    Log.e("AIChat", "Không thể đọc ảnh người dùng gửi: ${e.message}", e)
                }
            }

            val userMsg = ChatMessageAI(
                content = userContent,
                isFromUser = true,
                userImageUrl = imageUrl,
                localBitmap = bitmap
            )
            _messages.value = _messages.value + userMsg
            saveMessageToFirebase(userMsg)

            try {
                val rawResponse = requestAiResponseFromBackend(userContent, imageUrl)
                val aiMessage = buildAiMessageFromRawResponse(rawResponse)
                _messages.value = _messages.value + aiMessage
                saveMessageToFirebase(aiMessage)
            } catch (e: Exception) {
                if (e is CancellationException || aiJob?.isCancelled == true) {
                    val stopMsg = ChatMessageAI(
                        content = "_[Đã dừng tạo câu trả lời]_",
                        isFromUser = false
                    )
                    _messages.value = _messages.value + stopMsg
                } else {
                    Log.e("AIChat_Error", "Lỗi AI backend: ${e.message}", e)
                    val errorMsg = ChatMessageAI(
                        content = friendlyAiError(e),
                        isFromUser = false
                    )
                    _messages.value = _messages.value + errorMsg
                    saveMessageToFirebase(errorMsg)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun requestAiResponseFromBackend(userContent: String, imageUrl: String?): String {
        val firebaseUser = auth.currentUser ?: throw IllegalStateException("Bạn cần đăng nhập để dùng GunplaAI.")
        val token = firebaseUser.getIdToken(false).await().token
            ?: throw IllegalStateException("Không lấy được phiên đăng nhập Firebase.")

        val history = _messages.value
            .dropLast(1)
            .takeLast(MAX_BACKEND_HISTORY)
            .filter { it.content.isNotBlank() && it.content != "_[Đã dừng tạo câu trả lời]_" }
            .map {
                AiChatHistoryItem(
                    role = if (it.isFromUser) "user" else "model",
                    content = it.content
                )
            }

        val response = backendApi.sendAiChatMessage(
            authorization = "Bearer $token",
            request = AiChatRequest(
                message = userContent,
                imageUrl = imageUrl,
                history = history
            )
        )

        if (!response.isSuccessful) {
            val code = response.code()
            val errorBody = response.errorBody()?.string().orEmpty()
            val (errorCode, message) = parseBackendError(code, errorBody)
            throw AiBackendException(code, errorCode, message)
        }

        val body = response.body()
        if (body?.success != true || body.text.isNullOrBlank()) {
            throw IllegalStateException(body?.message ?: "AI backend không trả về nội dung.")
        }

        return body.text
    }

    private suspend fun ensureAiBackendReady() {
        val response = backendApi.getAiHealth(check = true)
        if (!response.isSuccessful) {
            throw AiBackendException(response.code(), "AI_HEALTH_FAILED", "Không kiểm tra được cấu hình GunplaAI.")
        }

        val health = response.body()
            ?: throw AiBackendException(response.code(), "AI_HEALTH_EMPTY", "Backend AI không trả về trạng thái cấu hình.")

        if (!health.configured) {
            throw AiBackendException(503, "AI_NOT_CONFIGURED", "Backend AI chưa được cấu hình.")
        }

        if (!health.provider.equals("vertex", ignoreCase = true)) {
            throw AiBackendException(503, "AI_PROVIDER_INVALID", "Backend AI chưa chuyển sang Vertex AI.")
        }

        if (!health.model.equals("gemini-2.5-flash", ignoreCase = true)) {
            throw AiBackendException(503, "AI_MODEL_INVALID", "Backend AI chưa dùng model gemini-2.5-flash.")
        }

        if (health.vertexAuthReady == false) {
            throw AiBackendException(
                503,
                "AI_AUTH_INVALID",
                health.vertexAuthError ?: "Backend chưa lấy được token Vertex AI."
            )
        }
    }

    private fun parseBackendError(httpCode: Int, errorBody: String): Pair<String, String> {
        return try {
            val json = JSONObject(errorBody)
            val errorCode = json.optString("errorCode").ifBlank { "HTTP_$httpCode" }
            val message = json.optString("message").ifBlank { "AI backend error HTTP $httpCode" }
            errorCode to message
        } catch (_: Exception) {
            "HTTP_$httpCode" to "AI backend error HTTP $httpCode"
        }
    }

    private suspend fun buildAiMessageFromRawResponse(rawResponse: String): ChatMessageAI {
        var cleanAiMessage = rawResponse
        var showCartButton = false
        val attachedProducts = mutableListOf<Product>()
        val attachedPostsList = mutableListOf<Post>()

        val autoCartMatches = AUTO_CART_REGEX.findAll(cleanAiMessage).toList()
        cleanAiMessage = cleanAiMessage.replace(AUTO_CART_REGEX, "").trim()
        for (match in autoCartMatches) {
            val productId = match.groupValues[1].trim()
            val quantity = match.groupValues[2].toIntOrNull()?.coerceAtLeast(1) ?: 1
            val addResult = addToCartIfAvailable(productId, quantity)
            if (addResult != null) {
                showCartButton = true
                if (attachedProducts.none { it.id == addResult.id }) {
                    attachedProducts.add(addResult)
                }
                if (cleanAiMessage.isBlank()) {
                    cleanAiMessage = "Mình đã thêm $quantity sản phẩm ${addResult.name} vào giỏ hàng. Bạn kiểm tra giỏ hàng để chốt đơn nhé."
                }
            } else {
                cleanAiMessage = appendAiNotice(
                    cleanAiMessage,
                    "Mẫu AI vừa chọn hiện không còn đủ tồn kho, nên mình chưa thêm vào giỏ hàng. Bạn có thể chọn mẫu khác trong các gợi ý bên dưới."
                )
            }
        }

        val extractedIds = ID_REGEX.findAll(cleanAiMessage)
            .flatMap { it.groupValues[1].split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        cleanAiMessage = cleanAiMessage.replace(ID_REGEX, "").trim()
        if (extractedIds.isNotEmpty()) {
            attachedProducts.addAll(
                cachedProductsInKho.filter { product ->
                    product.id in extractedIds && attachedProducts.none { it.id == product.id }
                }
            )
        }

        val extractedPostIds = POST_ID_REGEX.findAll(cleanAiMessage)
            .flatMap { it.groupValues[1].split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        cleanAiMessage = cleanAiMessage.replace(POST_ID_REGEX, "").trim()
        if (extractedPostIds.isNotEmpty()) {
            attachedPostsList.addAll(cachedPostsInMarket.filter { post -> post.id in extractedPostIds })
        }

        return ChatMessageAI(
            content = cleanAiMessage.ifBlank { "Mình đã xử lý yêu cầu của bạn." },
            isFromUser = false,
            attachedProducts = attachedProducts,
            attachedPosts = attachedPostsList,
            hasGoToCartButton = showCartButton
        )
    }

    private suspend fun addToCartIfAvailable(productId: String, quantity: Int): Product? {
        val product = productRepository.getProductById(productId).getOrNull() ?: return null
        if (!product.isActive || product.stock < quantity) return null

        return cartRepository.addToCart(product, quantity).fold(
            onSuccess = {
                Log.d("AIChat", "Đã thêm ${product.name} vào giỏ hàng từ AI.")
                product
            },
            onFailure = {
                Log.e("AIChat", "Không thể thêm giỏ hàng từ AI: ${it.message}", it)
                null
            }
        )
    }

    private fun appendAndSaveSystemMessage(content: String) {
        val message = ChatMessageAI(content = content, isFromUser = false)
        _messages.value = _messages.value + message
        saveMessageToFirebase(message)
    }

    private fun saveMessageToFirebase(message: ChatMessageAI) {
        val userId = currentUserId ?: return

        val chatRef = firestore.collection("users").document(userId)
            .collection("ai_chats").document(message.id)
        val attachedProductIds = message.attachedProducts.map { it.id }
        val attachedPostIds = message.attachedPosts.map { it.id }
        val data = hashMapOf(
            "content" to message.content,
            "isFromUser" to message.isFromUser,
            "timestamp" to message.timestamp,
            "attachedProductIds" to attachedProductIds,
            "attachedPostIds" to attachedPostIds,
            "hasGoToCartButton" to message.hasGoToCartButton,
            "userImageUrl" to message.userImageUrl
        )
        chatRef.set(data)
    }

    private suspend fun fetchMarketplacePostsData(): Pair<String, List<Post>> {
        var postList = emptyList<Post>()

        try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("status", "APPROVED")
                .limit(MAX_POST_CONTEXT.toLong())
                .get()
                .await()
            postList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("AIChat", "Không thể đọc bài marketplace cho AI: ${e.message}", e)
        }
        return Pair("", postList)
    }

    private fun friendlyAiError(e: Exception): String {
        if (e is AiBackendException) {
            return when (e.errorCode) {
                "UNAUTHORIZED" -> "Phiên đăng nhập của bạn đã hết hạn. Bạn đăng nhập lại rồi dùng GunplaAI nhé."
                "IMAGE_TOO_LARGE" -> "Ảnh bạn gửi hơi lớn. Bạn chọn ảnh nhỏ hơn rồi thử lại nhé."
                "AI_TIMEOUT" -> "GunplaAI phản hồi hơi lâu. Bạn thử gửi lại sau vài giây nhé."
                "AI_NOT_CONFIGURED", "AI_PROVIDER_INVALID", "AI_MODEL_INVALID", "AI_AUTH_INVALID", "AI_UNAVAILABLE" ->
                    "GunplaAI backend chưa sẵn sàng hoặc cấu hình Vertex AI chưa đúng. Bạn thử lại sau ít phút nhé."
                "BAD_REQUEST", "EMPTY_MESSAGE" -> e.message ?: "Nội dung gửi lên GunplaAI chưa hợp lệ."
                else -> e.message ?: "Hệ thống AI đang gặp sự cố tạm thời. Bạn thử lại sau ít phút nhé."
            }
        }

        val errorMessage = e.message.orEmpty()
        return when {
            errorMessage.contains("401") || errorMessage.contains("đăng nhập", ignoreCase = true) -> {
                "Phiên đăng nhập của bạn đã hết hạn. Bạn đăng nhập lại rồi dùng GunplaAI nhé."
            }

            errorMessage.contains("413") || errorMessage.contains("IMAGE_TOO_LARGE", ignoreCase = true) -> {
                "Ảnh bạn gửi hơi lớn. Bạn chọn ảnh nhỏ hơn rồi thử lại nhé."
            }

            errorMessage.contains("504") || errorMessage.contains("timeout", ignoreCase = true) -> {
                "GunplaAI phản hồi hơi lâu. Bạn thử gửi lại sau vài giây nhé."
            }

            errorMessage.contains("503") || errorMessage.contains("AI_UNAVAILABLE", ignoreCase = true) -> {
                "Hiện tại GunplaAI đang quá tải hoặc cấu hình backend chưa sẵn sàng. Bạn thử lại sau ít phút nhé."
            }

            e is java.net.UnknownHostException || e is java.net.SocketTimeoutException -> {
                "Kết nối mạng đang không ổn định. Bạn kiểm tra Wi-Fi/4G rồi nhắn lại nhé."
            }

            else -> "Hệ thống AI đang gặp sự cố tạm thời. Bạn thử lại sau ít phút nhé."
        }
    }

    private fun appendAiNotice(message: String, notice: String): String {
        return if (message.isBlank()) notice else "$message\n\n$notice"
    }

    companion object {
        private const val CLOUDINARY_UNSIGNED_PRESET = "gundame-storepromax"
        private const val MAX_STORED_HISTORY = 80L
        private const val MAX_BACKEND_HISTORY = 30
        private const val MAX_POST_CONTEXT = 20

        private val AUTO_CART_REGEX = Regex(
            "\\[\\s*AUTO_CART\\s*:\\s*([^,\\]]+)\\s*,\\s*(\\d+)\\s*]",
            RegexOption.IGNORE_CASE
        )
        private val ID_REGEX = Regex(
            "\\[\\s*ID\\s*:\\s*([^\\]]+)]",
            RegexOption.IGNORE_CASE
        )
        private val POST_ID_REGEX = Regex(
            "\\[\\s*POST_ID\\s*:\\s*([^\\]]+)]",
            RegexOption.IGNORE_CASE
        )
    }
}
