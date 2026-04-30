package com.example.storepromax.presentation.myreview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.UserReview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReviewScreen(
    navController: NavController,
    viewModel: MyReviewViewModel = hiltViewModel()
) {
    // State dữ liệu
    val reviewUiModels = viewModel.myReviews.value
    val isLoadingReviews = viewModel.isLoadingReviews.value

    val unreviewedItems = viewModel.unreviewedItems.value
    val isLoadingUnreviewed = viewModel.isLoadingUnreviewed.value

    // State UI
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var reviewToEdit by remember { mutableStateOf<UserReview?>(null) }
    var editContentText by remember { mutableStateOf("") }

    // Bộ lọc
    val filterOptions = listOf("Tất cả", "5 Sao", "4 Sao", "3 Sao", "1-2 Sao")
    var selectedFilter by remember { mutableStateOf(filterOptions[0]) }

    val filteredReviews = remember(reviewUiModels, selectedFilter) {
        reviewUiModels.filter { uiModel ->
            when (selectedFilter) {
                "5 Sao" -> uiModel.review.rating == 5
                "4 Sao" -> uiModel.review.rating == 4
                "3 Sao" -> uiModel.review.rating == 3
                "1-2 Sao" -> uiModel.review.rating <= 2
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đánh giá của tôi", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D47A1))
            )
        },
        containerColor = Color(0xFFF8FAFC) // Màu nền sáng, sang trọng hơn
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF0D47A1),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF0D47A1)
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Chưa đánh giá (${unreviewedItems.size})", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Đã đánh giá", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp) }
                )
            }

            if (selectedTabIndex == 0) {
                // TAB CHƯA ĐÁNH GIÁ
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoadingUnreviewed) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF0D47A1))
                    } else if (unreviewedItems.isEmpty()) {
                        Text("Không có sản phẩm nào chờ đánh giá.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(unreviewedItems) { item ->
                                UnreviewedItemCard(
                                    item = item,
                                    onReviewClick = { navController.navigate("write_review_screen/${item.orderId}") }
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB ĐÃ ĐÁNH GIÁ
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!isLoadingReviews && reviewUiModels.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filterOptions) { option ->
                                val isSelected = selectedFilter == option
                                Surface(
                                    modifier = Modifier.clickable { selectedFilter = option },
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = option, color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF475569),
                                        fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }

                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        if (isLoadingReviews) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF0D47A1))
                        } else if (reviewUiModels.isEmpty() || filteredReviews.isEmpty()) {
                            Text("Chưa có đánh giá nào.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(filteredReviews) { uiModel ->
                                    MyReviewItem(
                                        uiModel = uiModel,
                                        onEditClick = {
                                            reviewToEdit = uiModel.review
                                            editContentText = uiModel.review.content
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // DIALOG SỬA
        if (reviewToEdit != null) {
            AlertDialog(
                onDismissRequest = { reviewToEdit = null },
                containerColor = Color.White,
                title = { Text("Sửa đánh giá", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editContentText,
                        onValueChange = { editContentText = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.editReview(reviewToEdit!!.productId, reviewToEdit!!.id, editContentText)
                            reviewToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                    ) { Text("Cập nhật") }
                },
                dismissButton = { TextButton(onClick = { reviewToEdit = null }) { Text("Hủy", color = Color.Gray) } }
            )
        }
    }
}

// 🌟 UI MỚI: CARD SẢN PHẨM CHƯA ĐÁNH GIÁ (Phẳng, Hiện đại)
@Composable
fun UnreviewedItemCard(item: UnreviewedItemUiModel, onReviewClick: () -> Unit) {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateString = formatter.format(Date(item.orderDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)) // Viền mỏng thay vì đổ bóng gắt
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = item.productImageUrl,
                    contentDescription = item.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).background(Color.White)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ngày mua: $dateString", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReviewClick,
                modifier = Modifier.align(Alignment.End).height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Text("Đánh giá ngay", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 🌟 UI MỚI: CARD ĐÃ ĐÁNH GIÁ (Xóa bỏ hình khối xám thừa thãi, Nút Sửa dạng Chip)
@Composable
fun MyReviewItem(uiModel: ReviewUiModel, onEditClick: () -> Unit) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(uiModel.review.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)) // Viền mỏng tinh tế
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Phân khu Sản phẩm (Gọn gàng, không nền xám)
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = uiModel.productImageUrl,
                    contentDescription = uiModel.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).background(Color.White)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = uiModel.productName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF334155),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

            // 2. Đánh giá (Sao gom sát lại, Nút Sửa dạng Pill xịn sò)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < uiModel.review.rating) Color(0xFFFF9800) else Color(0xFFE2E8F0),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateString, fontSize = 12.sp, color = Color(0xFF94A3B8))
                }

                // NÚT "SỬA" BỌC TRONG CHIP (Siêu đẹp)
                Surface(
                    onClick = onEditClick,
                    color = Color(0xFFEFF6FF), // Nền xanh nhạt
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sửa", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            // 3. Nội dung bình luận
            Text(text = uiModel.review.content, fontSize = 14.sp, color = Color(0xFF1E293B), lineHeight = 22.sp)
        }
    }
}