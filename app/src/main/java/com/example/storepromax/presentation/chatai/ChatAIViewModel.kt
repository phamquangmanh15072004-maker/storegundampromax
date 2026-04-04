package com.example.storepromax.presentation.chat_ai

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.example.storepromax.domain.model.ChatMessageAI
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.CartRepository
import com.example.storepromax.domain.repository.OrderRepository
import com.example.storepromax.domain.repository.ProductRepository
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import com.cloudinary.android.callback.ErrorInfo
import com.example.storepromax.BuildConfig
import com.example.storepromax.domain.model.Post
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume

@HiltViewModel
class AIChatViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private var chatSession: Chat? = null
    private val _messages = MutableStateFlow<List<ChatMessageAI>>(emptyList())
    val messages = _messages.asStateFlow()
    private var cachedPostsInMarket: List<Post> = emptyList()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private var cachedProductsInKho: List<Product> = emptyList()
    private val currentUserId = auth.currentUser?.uid ?: "UNKNOWN_USER"
    private var aiJob: Job? = null
    fun stopGenerating() {
        aiJob?.cancel()
        _isLoading.value = false
    }
    init {
        loadChatHistoryAndInitAI()
    }
    suspend fun uploadImageToCloudinary(uri: Uri): String? =
        suspendCancellableCoroutine { continuation ->
            MediaManager.get().upload(uri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (continuation.isActive) continuation.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        }

    private fun loadChatHistoryAndInitAI() {
        if (currentUserId == "UNKNOWN") return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (productsInfo, allProductsInKho) = fetchRelevantProductsData()
                val (postsInfo, allPosts) = fetchMarketplacePostsData()
                cachedProductsInKho = allProductsInKho
                cachedPostsInMarket = allPosts
                val snapshot = firestore.collection("users").document(currentUserId)
                    .collection("ai_chats")
                    .orderBy("timestamp")
                    .get()
                    .await()

                val loadedMessages = snapshot.documents.mapNotNull { doc ->
                    val content = doc.getString("content") ?: ""
                    val isFromUser = doc.getBoolean("isFromUser") ?: true
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val hasGoToCartButton = doc.getBoolean("hasGoToCartButton") ?: false
                    val userImageUrl = doc.getString("userImageUrl")
                    val attachedProductIds =
                        doc.get("attachedProductIds") as? List<String> ?: emptyList()
                    val attachedProducts = allProductsInKho.filter { it.id in attachedProductIds }
                    val attachedPostIds = doc.get("attachedPostIds") as? List<String> ?: emptyList()
                    val attachedPosts = attachedPostIds.map { postId ->
                        val existingPost = allPosts.find { it.id == postId }
                        if (existingPost != null) {
                            existingPost
                        } else {
                            Post(
                                id = postId,
                                title = "Bài viết đã bị xóa hoặc ẩn",
                                price = 0,
                                userName = "Hệ thống",
                                status = "DELETED",
                                images = emptyList()
                            )
                        }
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
                        content = "Chào bạn! Mình là GunplaAI, trợ lý ảo thông minh của cửa hàng Gunpla đây. Kho đang về rất nhiều mẫu Gundam cực cháy. Mình nắm rõ từng mẫu Gundam trong kho cũng như các dụng cụ lắp ráp. Bạn đang muốn tìm dòng HG, RG, MG hay cần tư vấn gì thì cứ nhắn mình nha! 🤖",
                        isFromUser = false
                    )
                    _messages.value = listOf(welcomeMsg)
                    saveMessageToFirebase(welcomeMsg)
                } else {
                    _messages.value = loadedMessages
                }
                val pastOrders = orderRepository.getOrders().first()
                val purchasedItemsInfo = if (pastOrders.isNotEmpty()) {
                    val activeOrders = pastOrders.filter {
                        it.status != "DELIVERED" && it.status != "CANCELLED" && it.status != "COMPLETED"
                    }

                    if (activeOrders.isNotEmpty()) {
                        val sb = StringBuilder("CÁC ĐƠN HÀNG ĐANG CHỜ GIAO CỦA KHÁCH:\n")
                        activeOrders.forEach { order ->
                            val statusText = when(order.status) {
                                "PENDING" -> "Đang chờ xác nhận (Dự kiến 3-4 ngày nữa giao tới)"
                                "PROCESSING" -> "Đang đóng gói chuẩn bị hàng (Dự kiến 2-3 ngày nữa giao tới)"
                                "SHIPPED" -> "Đã giao cho đơn vị vận chuyển (Dự kiến trong hôm nay hoặc ngày mai sẽ tới)"
                                else -> "Đang xử lý (Sắp giao)"
                            }
                            val items = order.items.joinToString(", ") { "${it.product.name} (Số lượng: ${it.quantity})" }
                            sb.append("- Đơn #${order.id.takeLast(6)}: Gồm [$items] -> Tình trạng: $statusText\n")
                        }
                        sb.toString()
                    } else {
                        "Khách hiện không có đơn hàng nào đang giao."
                    }
                } else {
                    "Khách chưa từng mua hoặc đặt sản phẩm nào."
                }
                Log.d("AIChat_Debug", "Products Info nạp cho AI: \n$productsInfo")

                val sysPrompt = """
                    Bạn là GunplaAI, trợ lý ảo cực kỳ thông minh và tận tâm của cửa hàng Gunpla.
                    
                    📦 KHO HÀNG CỦA SHOP (ƯU TIÊN 1):
                    $productsInfo
                    
                    ♻️ BÀI ĐĂNG TỪ CỘNG ĐỒNG / MARKETPLACE (ƯU TIÊN 2):
                    $postsInfo
                    
                    🚚 THÔNG TIN ĐƠN HÀNG CỦA KHÁCH:
                    $purchasedItemsInfo
                    
                    NHIỆM VỤ VÀ QUY TẮC TỐI THƯỢNG (VI PHẠM SẼ CRASH HỆ THỐNG):
                    
                    1. VÀO THẲNG VẤN ĐỀ: Không dài dòng chào hỏi nếu đang ở giữa cuộc trò chuyện. Xưng hô là "Gunpla" hoặc "Em", gọi khách là "Anh/Chị" hoặc "Bạn".
                    
                    2. KHI TƯ VẤN SẢN PHẨM & MARKETPLACE:
                       - ƯU TIÊN 1 (Hàng của Shop): Khi gợi ý sản phẩm của shop, BẮT BUỘC chèn mã [ID: id_san_pham] vào cuối câu. Có thể gộp ID: [ID: id1, id2].
                       - ƯU TIÊN 2 (Hàng Pass/Cũ): CHỈ gợi ý bài đăng từ cộng đồng khi: Khách chê giá đắt, khách chủ động hỏi mua hàng cũ/pass, hoặc kho shop đã hết hàng. 
                       - KHI GỢI Ý BÀI ĐĂNG, BẮT BUỘC chèn mã [POST_ID: id_bai_viet] vào cuối câu.
                       - TUYỆT ĐỐI KHÔNG nhầm lẫn việc dùng [ID] và [POST_ID].
                    
                    3. KHI CHỐT ĐƠN HÀNG SHOP:
                       - Nếu khách đồng ý mua nhưng chưa rõ số lượng: Phải hỏi lại số lượng.
                       - Nếu đã rõ số lượng: Xác nhận với khách, SAU ĐÓ BẮT BUỘC chèn mã [AUTO_CART: id_san_pham, so_luong] vào cuối câu.
                       
                    4. KHI KHÁCH GỬI HÌNH ẢNH:
                       - Hãy quan sát kỹ hình ảnh. Tìm sản phẩm giống hoặc tương tự nhất trong kho hàng để tư vấn. Nếu shop không có, hãy thử tìm trong kho Bài đăng cộng đồng.
                       
                    5. KHI KHÁCH HỎI VỀ ĐƠN HÀNG (VD: "Hàng của tôi đâu", "Bao giờ giao"):
                       - Dựa vào "THÔNG TIN ĐƠN HÀNG CỦA KHÁCH" bên trên. Trả lời mã đơn hàng (đọc 6 số cuối), tình trạng hiện tại và thời gian dự kiến nhận hàng một cách tự nhiên, xoa dịu nếu khách giục.
                    
                    VÍ DỤ CÁCH BẠN PHẢI TRẢ LỜI:
                    Khách: Tư vấn cho mình mẫu MG.
                    Bạn: Dạ cửa hàng đang có sẵn mẫu MG Zeta Ver Ka và MG Barbatos cực kỳ xịn xò ạ! [ID: p4RrEFESLqJq3nUkqsLs, Y8w02J6P3bIgCwnIaAYd]
                    
                    Khách: Con Zeta đắt quá, Bạn xem có ai pass lại không?
                    Bạn: Dạ Trợ Lý vừa kiểm tra thấy trong hội có bạn GundamMaster đang pass lại một con MG Zeta Ver Ka ráp rồi, giá rẻ hơn hẳn đó ạ. Anh xem thử bài này nhé! [POST_ID: post_123456]
                    
                    Khách: Ok chốt lấy anh con Barbatos của shop nha, 1 con thôi.
                    Bạn: Dạ Trợ Lý đã tự động bỏ 1 hộp MG Barbatos vào giỏ hàng cho anh rồi nhé. Anh ghé giỏ hàng để chốt đơn nha! [AUTO_CART: Y8w02J6P3bIgCwnIaAYd, 1]
                """.trimIndent()

                val generativeModel = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    systemInstruction = content {
                        text(
                            sysPrompt
                        )
                    },
                    apiKey = BuildConfig.GEMINI_API_KEY
                )

                val historyForGemini = _messages.value.map { msg ->
                    content(role = if (msg.isFromUser) "user" else "model") {
                        text(msg.content)
                    }
                }

                chatSession = generativeModel.startChat(historyForGemini)

            } catch (e: Exception) {
                Log.e("AIChat", "Lỗi khởi tạo AI: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchRelevantProductsData(): Pair<String, List<Product>> {
        var productsDataString = "Hiện tại kho đang trống."
        var productList = emptyList<Product>()

        try {
            val result = productRepository.getProductsPaginated(20, null, "All")

            result.onSuccess { (list, _) ->
                if (list.isNotEmpty()) {
                    productList = list

                    val productsInfoBuilder = StringBuilder()

                    for (product in list) {
                        productsInfoBuilder.append("- Tên: ${product.name} | Giá: ${product.price} | ID: ${product.id}\n")
                        val reviewsResult = productRepository.getProductReviews(product.id)
                        reviewsResult.onSuccess { reviews ->
                            if (reviews.isNotEmpty()) {
                                productsInfoBuilder.append("  * Đánh giá của người mua (3 đánh giá mới nhất):\n")
                                reviews.take(3).forEach { review ->
                                    productsInfoBuilder.append("    + ${review.rating} sao: \"${review.comment}\"\n")
                                }
                            } else {
                                productsInfoBuilder.append("  * Đánh giá: Chưa có đánh giá nào.\n")
                            }
                        }
                        productsInfoBuilder.append("\n")
                    }
                    productsDataString = productsInfoBuilder.toString()
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseError", "Lỗi: ${e.message}")
        }
        return Pair(productsDataString, productList)
    }

    fun sendMessage(
        userContent: String,
        imageUri: Uri? = null,
        imageUrl: String? = null,
        context: Context? = null
    ) {
        if ((userContent.isBlank() && imageUri == null) || _isLoading.value || chatSession == null) return

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
                    Log.e("AIChat", "Lỗi đọc ảnh: ${e.message}")
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
                val inputContent = content("user") {
                    if (bitmap != null) {
                        image(bitmap)
                    }
                    if (userContent.isNotBlank()) {
                        text(userContent)
                    }
                }

                val response = chatSession?.sendMessage(inputContent)
                val rawResponse = response?.text ?: "Lỗi phản hồi"
                var showCartButton = false
                var cleanAiMessage = rawResponse
                val attachedProducts = mutableListOf<Product>()

                val idRegex = "\\[ID:(.*?)\\]".toRegex()
                val idMatches = idRegex.findAll(cleanAiMessage)
                val extractedIds = mutableListOf<String>()

                for (match in idMatches) {
                    val ids = match.groupValues[1].split(",").map { it.trim() }
                    extractedIds.addAll(ids)
                }

                cleanAiMessage = cleanAiMessage.replace(idRegex, "").trim()

                if (extractedIds.isNotEmpty()) {
                    attachedProducts.addAll(cachedProductsInKho.filter { product ->
                        extractedIds.contains(product.id)
                    })
                }

                val autoCartRegex = "\\[AUTO_CART:(.*?),\\s*(\\d+)\\]".toRegex()
                val cartMatch = autoCartRegex.find(cleanAiMessage)

                if (cartMatch != null) {
                    showCartButton = true
                    val productId = cartMatch.groupValues[1].trim()
                    val quantity = cartMatch.groupValues[2].trim().toIntOrNull() ?: 1
                    cleanAiMessage = cleanAiMessage.replace(autoCartRegex, "").trim()

                    if (cleanAiMessage.isEmpty()) {
                        cleanAiMessage =
                            "Trợ Lý đã tự động thêm $quantity sản phẩm vào giỏ hàng cho bạn rồi nhé! Bạn kiểm tra giỏ hàng để tiến hành thanh toán nha."
                    }

                    val productToAdd = cachedProductsInKho.find { it.id == productId }
                    if (productToAdd != null) {
                        addToCartSilently(productToAdd, quantity)
                    }
                }
                val postIdRegex = "\\[POST_ID:(.*?)\\]".toRegex()
                val postIdMatches = postIdRegex.findAll(cleanAiMessage)
                val extractedPostIds = mutableListOf<String>()

                for (match in postIdMatches) {
                    val ids = match.groupValues[1].split(",").map { it.trim() }
                    extractedPostIds.addAll(ids)
                }

                cleanAiMessage = cleanAiMessage.replace(postIdRegex, "").trim()

                val attachedPostsList = mutableListOf<Post>()
                if (extractedPostIds.isNotEmpty()) {
                    attachedPostsList.addAll(cachedPostsInMarket.filter { post ->
                        extractedPostIds.contains(post.id)
                    })
                }
                val aiMsg = ChatMessageAI(
                    content = cleanAiMessage,
                    isFromUser = false,
                    attachedProducts = attachedProducts,
                    attachedPosts = attachedPostsList,
                    hasGoToCartButton = showCartButton
                )
                _messages.value = _messages.value + aiMsg
                saveMessageToFirebase(aiMsg)
            } catch (e: Exception) {
                if (e is CancellationException || aiJob?.isCancelled == true || e.message?.contains("unexpected") == true) {
                    Log.d("AIChat_Debug", "AI generation cancelled by user.")
                    val stopMsg = ChatMessageAI(
                        content = "_[Đã dừng tạo câu trả lời]_",
                        isFromUser = false
                    )
                    _messages.value = _messages.value + stopMsg

                } else {
                    Log.e("AIChat_Error", "Lỗi AI: ${e.message}", e)
                    val errorMsg = ChatMessageAI(
                        content = "GunplaAI đang bận chút xíu, lỗi: ${e.message}",
                        isFromUser = false
                    )
                    _messages.value = _messages.value + errorMsg
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addToCartSilently(product: Product, quantity: Int) {
        viewModelScope.launch {
            try {
                cartRepository.addToCart(product, quantity)
                Log.d("AIChat", "Đã tự động thêm ${product.name} vào giỏ hàng thành công!")
            } catch (e: Exception) {
                Log.e("AIChat", "Lỗi thêm giỏ hàng ngầm: ${e.message}")
            }
        }
    }

    private fun saveMessageToFirebase(message: ChatMessageAI) {
        if (currentUserId == "UNKNOWN") return

        val chatRef = firestore.collection("users").document(currentUserId)
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
        var postsDataString = "Hiện tại chưa có bài đăng nào từ cộng đồng."
        var postList = emptyList<Post>()

        try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("status", "APPROVED")
                .limit(20)
                .get()
                .await()
            postList = snapshot.toObjects(Post::class.java)

            if (postList.isNotEmpty()) {
                val sb = StringBuilder()
                for (post in postList) {
                    val tinhTrang = if (post.condition == "USED") "Đã ráp (Cũ)" else "Chưa ráp (Mới)"
                    sb.append("- Bài: ${post.title} | Giá pass: ${post.price}đ | Người bán: ${post.userName} | Tình trạng: $tinhTrang | POST_ID: ${post.id}\n")
                }
                postsDataString = sb.toString()
            }
        } catch (e: Exception) {
            Log.e("AIChat", "Lỗi đọc Posts: ${e.message}")
        }
        return Pair(postsDataString, postList)
    }
}