package com.example.storepromax.presentation.profile.edit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

import com.example.storepromax.domain.model.Province
import com.example.storepromax.domain.model.District
import com.example.storepromax.domain.model.Ward
import com.example.storepromax.presentation.checkout.SearchableDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // --- 1. COLLECT STATE TỪ VIEWMODEL ---
    // Vì ViewModel đã khai báo _currentUser là User?, nên biến 'user' ở đây sẽ tự hiểu là kiểu User
    val user by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // Data Dropdown (Tỉnh/Huyện/Xã)
    val provinces by viewModel.provinces.collectAsState()
    val districts by viewModel.districts.collectAsState()
    val wards by viewModel.wards.collectAsState()

    // Các mục đang được chọn (Tự động cập nhật nhờ logic parse trong ViewModel)
    val selectedProvince by viewModel.selectedProvince.collectAsState()
    val selectedDistrict by viewModel.selectedDistrict.collectAsState()
    val selectedWard by viewModel.selectedWard.collectAsState()
    val specificAddress by viewModel.specificAddress.collectAsState()

    // --- 2. LOCAL STATE (CHO CÁC TRƯỜNG NHẬP LIỆU) ---
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 🔥 LOGIC TỰ ĐỘNG ĐIỀN: Khi user load xong, điền Tên và SĐT vào ô nhập
    LaunchedEffect(user) {
        user?.let {
            name = it.name   // Lấy từ User model
            phone = it.phone // Lấy từ User model
            // Lưu ý: Địa chỉ không cần set ở đây vì ViewModel đã tự parse và đẩy vào các biến StateFlow ở trên
        }
    }

    // Xử lý thông báo kết quả (Toast)
    LaunchedEffect(updateState) {
        if (updateState == "SUCCESS") {
            Toast.makeText(context, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            navController.popBackStack()
        } else if (updateState != null) {
            Toast.makeText(context, updateState, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        }
    }

    // Bộ chọn ảnh
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // --- 3. GIAO DIỆN (UI) ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // === PHẦN 1: AVATAR ===
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    val imageToShow = selectedImageUri ?: user?.avatarUrl

                    if (imageToShow != null && imageToShow.toString().isNotBlank()) {
                        AsyncImage(
                            model = imageToShow,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Surface(shape = CircleShape, color = Color.LightGray, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(20.dp))
                        }
                    }

                    Box(modifier = Modifier.align(Alignment.BottomEnd).background(Color.White, CircleShape).padding(6.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Chạm để đổi ảnh", color = Color.Gray, fontSize = 12.sp)


                // === PHẦN 2: THÔNG TIN CÁ NHÂN ===
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hiển thị") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )

                // === PHẦN 3: ĐỊA CHỈ GIAO HÀNG (Dropdown) ===
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Địa chỉ nhận hàng",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )

                // 3.1 Tỉnh/Thành phố
                SearchableDropdown<Province>(
                    label = "Tỉnh / Thành phố",
                    items = provinces,
                    selectedItem = selectedProvince, // Tự động hiển thị Tỉnh cũ nhờ ViewModel
                    onItemSelected = { viewModel.onProvinceSelected(it) },
                    itemToString = { it.name }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 3.2 Quận/Huyện
                SearchableDropdown<District>(
                    label = "Quận / Huyện",
                    items = districts,
                    selectedItem = selectedDistrict, // Tự động hiển thị Huyện cũ
                    onItemSelected = { viewModel.onDistrictSelected(it) },
                    itemToString = { it.name }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 3.3 Phường/Xã
                SearchableDropdown<Ward>(
                    label = "Phường / Xã",
                    items = wards,
                    selectedItem = selectedWard, // Tự động hiển thị Xã cũ
                    onItemSelected = { viewModel.onWardSelected(it) },
                    itemToString = { it.name }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 3.4 Số nhà cụ thể
                OutlinedTextField(
                    value = specificAddress,
                    onValueChange = { viewModel.onSpecificAddressChange(it) },
                    label = { Text("Số nhà, tên đường") },
                    placeholder = { Text("Ví dụ: Số 12, Ngõ 5...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // === PHẦN 4: NÚT LƯU ===
                Button(
                    onClick = {
                        viewModel.saveProfile(name, phone, selectedImageUri)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}