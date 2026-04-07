package com.example.storepromax.presentation.feed

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage // Nhớ import cái này
import coil.compose.rememberAsyncImagePainter
import java.text.DecimalFormat

class CurrencyAmountInputVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text.trim()
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val formattedText = try {
            val number = originalText.toLong()
            DecimalFormat("#,###").format(number).replace(",", ".")
        } catch (e: Exception) {
            originalText
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return formattedText.length
            }
            override fun transformedToOriginal(offset: Int): Int {
                return text.length
            }
        }
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    navController: NavController,
    postId: String? = null,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    LaunchedEffect(postId) {
        if (!postId.isNullOrBlank()) {
            viewModel.loadPostDataForEdit(postId)
        }
    }
    val context = LocalContext.current

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            if (event == "Success") {
                Toast.makeText(context, "Đã gửi bài! Vui lòng chờ Admin duyệt.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            } else {
                Toast.makeText(context, event, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditMode.value) "SỬA BÀI VIẾT" else "ĐĂNG BÁN GUNDAM", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalImages = viewModel.existingImageUrls.value.size + viewModel.selectedImages.value.size
                    Text("Hình ảnh sản phẩm ($totalImages/10)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Thêm ảnh",
                        color = Color(0xFF00D4FF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            multiplePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (viewModel.existingImageUrls.value.isNotEmpty() || viewModel.selectedImages.value.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(viewModel.existingImageUrls.value) { url ->
                            Box(modifier = Modifier.size(100.dp)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                        .clickable { viewModel.removeExistingImage(url) }, // Hàm xóa ảnh cũ
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(14.dp), tint = Color.Red)
                                }
                            }
                        }
                        items(viewModel.selectedImages.value) { uri ->
                            Box(modifier = Modifier.size(100.dp)) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                        .clickable { viewModel.removeImage(uri) }, // Hàm xóa ảnh mới
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(14.dp), tint = Color.Red)
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                    .clickable { multiplePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                            .clickable { multiplePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF00D4FF), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bấm để tải ảnh lên (Max 10)", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            Divider(color = Color(0xFFEEEEEE))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = viewModel.title.value,
                    onValueChange = { viewModel.title.value = it },
                    label = { Text("Tiêu đề bài viết", color = Color.Gray) },
                    placeholder = { Text("Ví dụ: Pass lại Sazabi Ver Ka ráp nét...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D4FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                OutlinedTextField(
                    value = viewModel.price.value,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 12) {
                            viewModel.price.value = it
                        }
                    },
                    label = { Text("Giá mong muốn", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D4FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    visualTransformation = CurrencyAmountInputVisualTransformation(),
                    suffix = { Text("VNĐ", fontWeight = FontWeight.Bold, color = Color.Gray) }
                )
            }

            Divider(color = Color(0xFFEEEEEE))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Tình trạng sản phẩm", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("NEW", "LIKE NEW", "USED", "JUNK").forEach { condition ->
                            FilterChip(
                                selected = viewModel.condition.value == condition,
                                onClick = { viewModel.condition.value = condition },
                                label = { Text(condition, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF9800),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Column {
                    Text("Dòng (Grade)", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("HG", "RG", "MG", "PG", "SD", "MB", "OTHER").forEach { grade ->
                            FilterChip(
                                selected = viewModel.grade.value == grade,
                                onClick = { viewModel.grade.value = grade },
                                label = { Text(grade, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00D4FF),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFFEEEEEE))
            Column {
                Text("Mô tả chi tiết", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = viewModel.content.value,
                    onValueChange = { viewModel.content.value = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("Mô tả tình trạng box, khớp, decal, phụ kiện...", color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D4FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.submitPost() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                shape = RoundedCornerShape(12.dp),
                enabled = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.isEditMode.value) "CẬP NHẬT & XIN DUYỆT" else "GỬI BÀI DUYỆT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}