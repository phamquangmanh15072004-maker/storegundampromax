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
                // === 1. THÔNG TIN CƠ BẢN ===
                AdminSectionCard("Thông tin cơ bản") {
                    OutlinedTextField(
                        value = viewModel.name.value,
                        onValueChange = { viewModel.name.value = it },
                        label = { Text("Tên sản phẩm") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Giá bán và Giá gốc (Cạnh nhau)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = viewModel.price.value,
                            onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.price.value = it },
                            label = { Text("Giá bán (VNĐ)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedLabelColor = Color(0xFFD32F2F),
                                focusedBorderColor = Color(0xFFD32F2F),
                                focusedTextColor = Color(0xFFD32F2F)
                            )
                        )
                        OutlinedTextField(
                            value = viewModel.originalPrice.value,
                            onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.originalPrice.value = it },
                            label = { Text("Giá gốc (VNĐ)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viewModel.stock.value,
                        onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.stock.value = it },
                        label = { Text("Số lượng kho") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // === 2. PHÂN LOẠI (DROPDOWN MENU) ===
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

                // === 3. HÌNH ẢNH (GIAO DIỆN ĐẸP) ===
                AdminSectionCard("Hình ảnh (${viewModel.selectedImages.value.size})") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // Nút thêm ảnh (Nét đứt)
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

                        // Danh sách ảnh đã chọn
                        items(viewModel.selectedImages.value) { uri ->
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
                                // Nút xóa nhỏ gọn
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

                // === 4. TRẠNG THÁI ===
                AdminSectionCard("Trạng thái") {
                    // Switch: Is New
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sản phẩm mới (New)", fontWeight = FontWeight.SemiBold)
                            Text("Hiển thị nhãn NEW", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = viewModel.isNew.value,
                            onCheckedChange = { viewModel.isNew.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4CAF50))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))

                    // Switch: Is Active
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

                // === 5. CHI TIẾT KHÁC ===
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

            // Nút Lưu ở dưới cùng
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