package com.example.storepromax.presentation.writereview

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline // 🌟 Nhớ import icon này
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Product
import com.example.storepromax.ui.components.StarRatingBar

val GunplaBlue = Color(0xFF0D47A1)
val BgColor = Color(0xFFF2F4F7)

data class ReviewFormState(
    val product: Product,
    var rating: Int = 0,
    var reviewText: String = "",
    var selectedImages: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewScreen(
    navController: NavController,
    productsToReview: List<Product>,
    isLoadingFromVM: Boolean,
    onSubmitReview: (String, Int, String, List<String>, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current

    val pendingReviews = remember(productsToReview) {
        mutableStateListOf(*productsToReview.map { ReviewFormState(it) }.toTypedArray())
    }

    var hasSubmittedAtLeastOne by remember { mutableStateOf(false) }

    var isUploading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đánh giá đơn hàng", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GunplaBlue)
            )
        },
        containerColor = BgColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoadingFromVM || isUploading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GunplaBlue)
                }
            } else if (productsToReview.isEmpty() && !hasSubmittedAtLeastOne) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = "Lỗi", tint = Color.Gray, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Không tìm thấy sản phẩm để đánh giá", color = Color.Gray)
                }
            } else if (pendingReviews.isEmpty() && hasSubmittedAtLeastOne) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Thành công", tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Cảm ơn bạn đã đánh giá!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Những đánh giá của bạn sẽ giúp cộng đồng mua sắm tốt hơn.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Về trang chủ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pendingReviews, key = { it.product.id }) { formState ->
                        ReviewItemForm(
                            formState = formState,
                            onFormUpdated = { updatedForm ->
                                val index = pendingReviews.indexOfFirst { it.product.id == formState.product.id }
                                if (index != -1) {
                                    pendingReviews[index] = updatedForm
                                }
                            },
                            onSubmit = {
                                isUploading = true
                                onSubmitReview(
                                    formState.product.id,
                                    formState.rating,
                                    formState.reviewText,
                                    formState.selectedImages
                                ) { isSuccess, message ->
                                    isUploading = false // Tắt vòng xoay
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                                    if (isSuccess) {
                                        hasSubmittedAtLeastOne = true
                                        pendingReviews.removeAll { it.product.id == formState.product.id }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItemForm(
    formState: ReviewFormState,
    onFormUpdated: (ReviewFormState) -> Unit,
    onSubmit: () -> Unit
) {
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newImages = formState.selectedImages + uris.map { it.toString() }
            onFormUpdated(formState.copy(selectedImages = newImages.take(5)))
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = formState.product.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = formState.product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Phân loại: Mặc định",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (formState.rating) {
                        1 -> "Tệ quá 😞"
                        2 -> "Không hài lòng 撇"
                        3 -> "Bình thường 😐"
                        4 -> "Hài lòng 🙂"
                        5 -> "Tuyệt vời 😍"
                        else -> "Chất lượng sản phẩm thế nào?"
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (formState.rating > 0) GunplaBlue else Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                StarRatingBar(
                    rating = formState.rating,
                    onRatingChanged = { newRating -> onFormUpdated(formState.copy(rating = newRating)) },
                    isEditable = true,
                    maxStars = 5
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = formState.reviewText,
                onValueChange = { onFormUpdated(formState.copy(reviewText = it)) },
                placeholder = {
                    Text(
                        "Hãy chia sẻ những điều bạn thích về sản phẩm này nhé...",
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = GunplaBlue
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (formState.selectedImages.size < 5) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                            .clickable {
                                multiplePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Thêm ảnh",
                                tint = GunplaBlue
                            )
                            Text("Thêm ảnh", fontSize = 10.sp, color = GunplaBlue)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(formState.selectedImages) { imageUrl ->
                        Box(modifier = Modifier.size(64.dp)) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Selected Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = {
                                    val newList = formState.selectedImages.filter { it != imageUrl }
                                    onFormUpdated(formState.copy(selectedImages = newList))
                                },
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xóa",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            val isReadyToSubmit = formState.rating > 0
            Button(
                onClick = onSubmit,
                enabled = isReadyToSubmit,
                modifier = Modifier
                    .align(Alignment.End)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Gửi Đánh Giá", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}