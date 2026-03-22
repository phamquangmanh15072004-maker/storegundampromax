import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.ChatMessageAI
import com.example.storepromax.domain.utils.formatVietnameseCurrency
import com.example.storepromax.presentation.chat_ai.AIChatViewModel
import com.example.storepromax.presentation.navigation.Screen
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val GunplaBlue = Color(0xFF0D47A1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    navController: NavController,
    viewModel: AIChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GunplaBlue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "AI",
                            tint = GunplaBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Trợ lý ảo Gunpla (AI)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Luôn sẵn sàng hỗ trợ", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        bottomBar = {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
            var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
            var isUploadingImage by remember { mutableStateOf(false) }
            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri ->
                    if (uri != null) {
                        selectedImageUri = uri
                        isUploadingImage = true
                        coroutineScope.launch {
                            uploadedImageUrl = viewModel.uploadImageToCloudinary(uri)
                            isUploadingImage = false
                        }
                    }
                }
            )

            Surface(shadowElevation = 8.dp, color = Color.White) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    if (selectedImageUri != null) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .size(80.dp)
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Ảnh đính kèm",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (isUploadingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = GunplaBlue
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        selectedImageUri = null
                                        uploadedImageUrl = null
                                        isUploadingImage = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xóa",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Ảnh", tint = GunplaBlue)
                        }

                        TextField(
                            value = textState,
                            onValueChange = { textState = it },
                            placeholder = {
                                Text(
                                    "Hỏi AI về Gundam, công cụ...",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color(0xFFF0F2F5),
                                unfocusedContainerColor = Color(0xFFF0F2F5),
                                cursorColor = GunplaBlue
                            ),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if ((textState.isNotBlank() || selectedImageUri != null) && !isLoading && !isUploadingImage) {
                                    viewModel.sendMessage(
                                        textState,
                                        selectedImageUri,
                                        uploadedImageUrl,
                                        context
                                    )
                                    textState = ""
                                    selectedImageUri = null
                                    uploadedImageUrl = null
                                }
                            })
                        )

                        if (isLoading) {
                            IconButton(onClick = { viewModel.stopGenerating() }) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Dừng",
                                    tint = Color.Red
                                )
                            }
                        } else {
                            val canSend =
                                (textState.isNotBlank() || selectedImageUri != null) && !isUploadingImage
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        viewModel.sendMessage(
                                            textState,
                                            selectedImageUri,
                                            uploadedImageUrl,
                                            context
                                        )
                                        textState = ""
                                        selectedImageUri = null
                                        uploadedImageUrl = null
                                    }
                                },
                                enabled = canSend
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    "Gửi",
                                    tint = if (canSend) GunplaBlue else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.forEachIndexed { index, msg ->
                val shouldShowTime = if (index == 0) {
                    true
                } else {
                    val prevMsg = messages[index - 1]
                    msg.timestamp - prevMsg.timestamp > 10 * 60 * 1000L
                }

                if (shouldShowTime) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatMessageTime(msg.timestamp),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                item {
                    AIChatBubble(
                        message = msg,
                        onProductClick = { productId ->
                            navController.navigate(Screen.Detail.createRoute(productId))
                        },
                        onGoToCartClick = {
                            navController.navigate("cart_screen?showBack=true")
                        },
                        onPostClick = { postId ->
                            navController.navigate("post_detail/$postId")
                        }
                    )
                }
            }

            if (isLoading) {
                item { AILoadingBubble() }
            }
        }
    }
}

@Composable
fun AIChatBubble(
    message: ChatMessageAI,
    onProductClick: (String) -> Unit = {},
    onGoToCartClick: () -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
    if (message.content == "_[Đã dừng tạo câu trả lời]_") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Đã dừng tạo câu trả lời",
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
        return
    }
    val isMe = message.isFromUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        val bubbleShape =
            if (isMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(
                18.dp,
                18.dp,
                18.dp,
                4.dp
            )
        val bubbleColor = if (isMe) GunplaBlue else Color(0xFFF0F2F5)
        val textColor = if (isMe) Color.White else Color.Black

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            val imageModel = message.localBitmap ?: message.userImageUrl
            if (imageModel != null) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "User Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
            if (message.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    MarkdownText(
                        markdown = message.content,
                        style = TextStyle(color = textColor, fontSize = 15.sp)
                    )
                }
            }
        }
        if (message.hasGoToCartButton) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onGoToCartClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)), // Màu cam chốt đơn
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.ShoppingCartCheckout,
                    contentDescription = "Cart",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đi đến Giỏ hàng", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        if (message.attachedProducts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                items(message.attachedProducts) { product ->
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { onProductClick(product.id) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            AsyncImage(
                                model = product.images.firstOrNull() ?: "",
                                contentDescription = product.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentScale = ContentScale.Crop
                            )

                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    product.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatVietnameseCurrency(product.price),
                                    color = GunplaBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

    }
    if (message.attachedPosts.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "🛒 Góc sang nhượng:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            items(message.attachedPosts) { post ->
                if (post.status == "DELETED") {
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Deleted",
                                tint = Color.LightGray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Bài viết này đã bị xóa hoặc bị ẩn bởi người bán.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onPostClick(post.id) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Box {
                                AsyncImage(
                                    model = post.images.firstOrNull() ?: "",
                                    contentDescription = post.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentScale = ContentScale.Crop
                                )
                                val tagColor =
                                    if (post.condition == "USED") Color.Red else Color(0xFF4CAF50)
                                val tagText = if (post.condition == "USED") "Đã ráp" else "Mới"
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp),
                                    color = tagColor,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        tagText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    post.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatVietnameseCurrency(post.price),
                                    color = Color(0xFFFF5722),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "👤 ${post.userName}",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}


@Composable
fun AILoadingBubble() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .background(Color(0xFFF0F2F5))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            CircularProgressIndicator(
                color = GunplaBlue,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

fun formatMessageTime(timestamp: Long): String {
    val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dayOfWeekFormat = SimpleDateFormat("EEE HH:mm", Locale("vi", "VN"))

    return when {
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) -> {
            timeFormat.format(messageTime.time)
        }

        now.timeInMillis - timestamp < 7 * 24 * 60 * 60 * 1000L -> {
            dayOfWeekFormat.format(messageTime.time)
        }

        else -> {
            "${dateFormat.format(messageTime.time)} ${timeFormat.format(messageTime.time)}"
        }
    }
}