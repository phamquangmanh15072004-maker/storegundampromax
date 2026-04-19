package com.example.storepromax.presentation.admin.product

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: String? = null,
    viewModel: AddProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isEditMode = productId != null

    // Launcher chọn ảnh
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris -> if (uris.isNotEmpty()) viewModel.addImages(uris) }

    // Load dữ liệu khi vào màn hình sửa
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProductById(productId)
        }
    }

    // Lắng nghe sự kiện Thành công/Thất bại
    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            if (event == "Success") {
                Toast.makeText(context, "Lưu thành công!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } else {
                Toast.makeText(context, event, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Cập Nhật Sản Phẩm" else "Thêm Sản Phẩm Mới", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdminSectionCard("Thông tin cơ bản") {
                    OutlinedTextField(
                        value = viewModel.name.value,
                        onValueChange = { viewModel.name.value = it },
                        label = { Text("Tên sản phẩm") },
                        isError = viewModel.nameError.value != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.sku.value,
                        onValueChange = { viewModel.sku.value = it.uppercase() },
                        label = { Text("Mã SKU (Bắt buộc)") },
                        isError = viewModel.skuError.value != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = viewModel.price.value,
                            onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.price.value = it },
                            label = { Text("Giá bán (VNĐ)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedLabelColor = Color.Red, focusedBorderColor = Color.Red)
                        )
                        OutlinedTextField(
                            value = viewModel.originalPrice.value,
                            onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.originalPrice.value = it },
                            label = { Text("Giá gốc (VNĐ)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = viewModel.stock.value,
                            onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.stock.value = it },
                            label = { Text("Kho") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = viewModel.costPrice.value,
                            onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.costPrice.value = it },
                            label = { Text("Giá vốn") },
                            modifier = Modifier.weight(1.2f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = viewModel.weight.value,
                            onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.weight.value = it },
                            label = { Text("Gram") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                AdminSectionCard("Phân loại") {
                    var expanded by remember { mutableStateOf(false) }
                    val categories = listOf("HG", "RG", "MG", "PG", "SD", "ACCESSORY", "TOOL")

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = viewModel.category.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Danh mục (Category)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        viewModel.category.value = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                AdminSectionCard("Hình ảnh (${viewModel.selectedImages.value.size})") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF9F9F9))
                                    .drawBehind {
                                        val stroke = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                                        drawRoundRect(color = Color.LightGray, style = stroke, cornerRadius = CornerRadius(8.dp.toPx()))
                                    }
                                    .clickable { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray)
                                    Text("Thêm", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                        itemsIndexed(viewModel.selectedImages.value) { index,uri ->
                            Box(modifier = Modifier.size(100.dp)) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (index == 0) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    ) {
                                        Text("Ảnh chính", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .zIndex(1f)
                                        .size(24.dp)
                                        .background(Color.White, CircleShape)
                                        .border(1.dp, Color.LightGray, CircleShape)
                                        .clickable { viewModel.removeImage(uri) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                AdminSectionCard("Trạng thái") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sản phẩm HOT", fontWeight = FontWeight.SemiBold)
                            Text("Gắn mác nổi bật thủ công", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = viewModel.isFeatured.value,
                            onCheckedChange = { viewModel.isFeatured.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF5252))
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Đang hoạt động (Active)", fontWeight = FontWeight.SemiBold)
                            Text("Hiển thị trên app", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = viewModel.isActive.value,
                            onCheckedChange = { viewModel.isActive.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2196F3))
                        )
                    }
                }
                AdminSectionCard("Mô tả & Cấu hình") {
                    OutlinedTextField(
                        value = viewModel.description.value,
                        onValueChange = { viewModel.description.value = it },
                        label = { Text("Mô tả chi tiết") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.model3DUrl.value,
                        onValueChange = { viewModel.model3DUrl.value = it },
                        label = { Text("Link 3D Model (.glb)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.ViewInAr, null) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
            Button(
                onClick = { viewModel.saveProduct() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                enabled = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ĐANG XỬ LÝ...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditMode) "CẬP NHẬT" else "LƯU SẢN PHẨM", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper Card để code gọn hơn
@Composable
fun AdminSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}