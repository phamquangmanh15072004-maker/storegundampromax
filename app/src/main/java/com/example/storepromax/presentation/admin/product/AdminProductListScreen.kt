package com.example.storepromax.presentation.admin.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.Product
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductListScreen(
    navController: NavController,
    viewModel: AdminProductListViewModel = hiltViewModel()
) {
    val products by viewModel.filteredProducts.collectAsState()
    val currentSearch by viewModel.searchQuery.collectAsState()
    val currentCategory by viewModel.selectedCategory.collectAsState()

    var showDeleteSingleDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val categories = listOf("Tất cả", "HG", "RG", "MG", "PG", "SD", "ACCESSORY", "TOOL")
    val lifecycleOwner = LocalLifecycleOwner.current

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importProductsFromCsv(uri)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadProducts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("QUẢN LÝ KHO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${products.size} sản phẩm", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                    }
                },
                actions = {
                    IconButton(onClick = { csvLauncher.launch("text/*") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import CSV", tint = Color(0xFF2E7D32))
                    }

                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Xóa tất cả", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_product") },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm mới", tint = Color.White)
            }
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Khu vực Tìm kiếm & Filter (Giữ nguyên)
            Column(
                modifier = Modifier
                    .background(Color(0xFFF5F5F5))
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = currentSearch,
                    onValueChange = { viewModel.onSearchTextChange(it) },
                    placeholder = { Text("Tìm theo tên...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = currentCategory == cat,
                            onClick = { viewModel.onCategoryChange(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE0E0E0),
                                labelColor = Color.Black
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = currentCategory == cat, borderColor = Color.Transparent)
                        )
                    }
                }
            }

            // Danh sách sản phẩm (Giữ nguyên)
            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy sản phẩm nào!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                    items(products) { product ->
                        AdminProductItem(
                            product = product,
                            onEdit = { navController.navigate("add_product?productId=${product.id}") },
                            onDelete = {
                                productToDelete = product
                                showDeleteSingleDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showDeleteSingleDialog && productToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteSingleDialog = false },
                title = { Text("Xóa sản phẩm") },
                text = { Text("Bạn có chắc muốn xóa '${productToDelete?.name}' khỏi kho không?") },
                confirmButton = {
                    Button(
                        onClick = {
                            productToDelete?.let { viewModel.deleteProduct(it.id) }
                            showDeleteSingleDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Xóa ngay")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSingleDialog = false }) { Text("Hủy") }
                }
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                icon = { Icon(Icons.Default.Warning, null, tint = Color.Red) },
                title = { Text("CẢNH BÁO NGUY HIỂM") },
                text = { Text("Hành động này sẽ XÓA SẠCH toàn bộ sản phẩm trong kho hàng để chuẩn bị Import mới.\n\nDữ liệu sẽ KHÔNG THỂ khôi phục. Bạn chắc chứ?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllProducts()
                            showDeleteAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("XÓA SẠCH")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) { Text("Hủy") }
                }
            )
        }
    }
}

@Composable
fun AdminProductItem(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    val formatter = DecimalFormat("#,###")
    val isLowStock = product.stock < 5

    val isHidden = !product.isActive

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) Color(0xFFF9FAFB) else Color(0xFFFFFFFF)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (isHidden) 1.dp else 4.dp),
        modifier = Modifier.alpha(if (isHidden) 0.6f else 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(80.dp)) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                if (isHidden) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.isNewProduct() && !isHidden) {
                        Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                            Text("NEW", fontSize = 8.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (product.isFeatured && !isHidden) {
                        Surface(color = Color(0xFFFF9800), shape = RoundedCornerShape(4.dp)) {
                            Text("HOT", fontSize = 8.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = product.name,
                        color = if (isHidden) Color.Gray else Color.Black,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatter.format(product.price)} ₫",
                        color = if (isHidden) Color.Gray else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (isHidden) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color(0xFFEEEEEE), shape = RoundedCornerShape(4.dp)) {
                            Text("Đang ẩn", fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if(isLowStock && !isHidden) Icons.Default.Warning else Icons.Default.Inventory,
                        contentDescription = null,
                        tint = if (isHidden) Color.LightGray else if (isLowStock) Color.Red else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Kho: ${product.stock} • SKU: ${product.sku}", // Sửa lại hiện SKU thay vì Category
                        color = if (isHidden) Color.LightGray else if (isLowStock) Color.Red else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (isLowStock && !isHidden) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = Color(0xFFFFA000), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}