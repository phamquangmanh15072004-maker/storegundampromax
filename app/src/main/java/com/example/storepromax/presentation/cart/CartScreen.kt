package com.example.storepromax.presentation.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.storepromax.domain.model.CartItem
import com.example.storepromax.domain.model.Voucher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

val GunplaBlue = Color(0xFF0D47A1)
val BgLight = Color(0xFFF5F5F5)
val AlertRed = Color(0xFFFF3B30)
val BorderGray = Color(0xFFE0E0E0)
val SuccessGreen = Color(0xFF00C853)
val TealFreeship = Color(0xFF00BFA5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    viewModel: CartViewModel = hiltViewModel(),
    showBackBtn: Boolean = false
) {
    val context = LocalContext.current
    val cartItems by viewModel.cartItems.collectAsState()

    val subTotal by viewModel.subTotal.collectAsState()
    val shippingFee by viewModel.shippingFee.collectAsState()
    val productDiscount by viewModel.productDiscountAmount.collectAsState()
    val freeshipAmount by viewModel.freeshipAmount.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()

    val selectedDiscount by viewModel.selectedDiscountVoucher.collectAsState()
    val selectedFreeship by viewModel.selectedFreeshipVoucher.collectAsState()
    val availableVouchers by viewModel.availableVouchers.collectAsState()

    var showVoucherSheet by remember { mutableStateOf(false) }

    val isAllSelected = cartItems.isNotEmpty() && cartItems.all { it.isSelected }
    val totalSelectedItems = cartItems.filter { it.isSelected }.size

    Scaffold(
        containerColor = BgLight,
        topBar = {
            TopAppBar(
                title = { Text("Giỏ hàng (${cartItems.size})", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GunplaBlue) },
                navigationIcon = {
                    if (showBackBtn) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = GunplaBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (cartItems.isEmpty()) {
                EmptyCartView(modifier = Modifier.weight(1f))
            } else {
                CheckoutTopBar(
                    totalPrice = totalPrice,
                    productDiscount = productDiscount,
                    freeshipAmount = freeshipAmount,
                    totalSelectedCount = totalSelectedItems,
                    isAllSelected = isAllSelected,
                    onToggleAll = {
                        cartItems.forEach { item ->
                            if (item.isSelected != !isAllSelected) viewModel.toggleSelection(item)
                        }
                    },
                    onCheckout = {
                        val dCode = selectedDiscount?.code ?: ""
                        val fCode = selectedFreeship?.code ?: ""
                        navController.navigate("checkout_screen?discountCode=$dCode&freeshipCode=$fCode")
                    },
                    modifier = Modifier.zIndex(2f)
                )

                VoucherSelectionRow(
                    productDiscount = productDiscount,
                    freeshipAmount = freeshipAmount,
                    onClick = { showVoucherSheet = true },
                    modifier = Modifier.zIndex(1f)
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        AnimatedCartItem(
                            item = item,
                            modifier = Modifier.animateItem(),
                            onToggle = { viewModel.toggleSelection(item) },
                            onIncrease = { viewModel.increaseQuantity(item) },
                            onDecrease = { viewModel.decreaseQuantity(item) },
                            onDelete = { viewModel.removeItem(item.product.id) },
                            onQuantityChange = { newQty -> viewModel.updateQuantity(item, newQty) }
                        )
                    }
                }
            }
        }
        if (showVoucherSheet) {
            VoucherBottomSheet(
                availableVouchers = availableVouchers,
                selectedDiscount = selectedDiscount,
                selectedFreeship = selectedFreeship,
                currentSubTotal = subTotal,
                onDismiss = { showVoucherSheet = false },
                onApplyCode = { code ->
                    viewModel.applyVoucherByCode(code) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onSelectVoucher = { voucher ->
                    val isCurrentlySelected = (voucher.id == selectedDiscount?.id) || (voucher.id == selectedFreeship?.id)
                    if (isCurrentlySelected) {
                        viewModel.removeVoucher(voucher.type)
                    } else {
                        viewModel.applyVoucher(voucher) { success, msg ->
                            if (!success) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun VoucherSelectionRow(
    productDiscount: Long,
    freeshipAmount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DecimalFormat("#,###")
    Surface(
        color = Color.White,
        modifier = modifier.fillMaxWidth().clickable { onClick() }.padding(top = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = GunplaBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Voucher", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                if (productDiscount == 0L && freeshipAmount == 0L) {
                    Text("Chọn hoặc nhập mã", color = Color.Gray, fontSize = 14.sp)
                } else {
                    if (productDiscount > 0) {
                        Text("-₫${formatter.format(productDiscount)}", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (freeshipAmount > 0) {
                        Text("Miễn Phí Vận Chuyển", color = TealFreeship, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherBottomSheet(
    availableVouchers: List<Voucher>,
    selectedDiscount: Voucher?,
    selectedFreeship: Voucher?,
    currentSubTotal: Long,
    onDismiss: () -> Unit,
    onApplyCode: (String) -> Unit,
    onSelectVoucher: (Voucher) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputCode by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val freeshipVouchers = availableVouchers.filter { it.type == "FREESHIP" }
    val discountVouchers = availableVouchers.filter { it.type == "DISCOUNT" }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = BgLight) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            // Header & Nhập mã
            Text("Chọn Voucher", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputCode, onValueChange = { inputCode = it.uppercase() },
                    placeholder = { Text("Nhập mã voucher...") }, singleLine = true,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { focusManager.clearFocus(); if (inputCode.isNotBlank()) onApplyCode(inputCode) },
                    enabled = inputCode.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                    shape = RoundedCornerShape(8.dp), modifier = Modifier.height(56.dp)
                ) { Text("ÁP DỤNG", fontWeight = FontWeight.Bold) }
            }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (freeshipVouchers.isNotEmpty()) {
                    item { Text("Miễn Phí Vận Chuyển", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp) }
                    items(freeshipVouchers) { voucher ->
                        val isEligible = currentSubTotal >= voucher.minOrderValue && voucher.usedCount < voucher.usageLimit
                        VoucherTicket(
                            voucher = voucher,
                            isSelected = selectedFreeship?.id == voucher.id,
                            isEligible = isEligible,
                            currentSubTotal = currentSubTotal,
                            iconBgColor = TealFreeship,
                            icon = Icons.Default.LocalShipping,
                            onSelect = { onSelectVoucher(voucher) }
                        )
                    }
                }
                if (discountVouchers.isNotEmpty()) {
                    item { Text("Giảm Giá Đơn Hàng", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 14.sp) }
                    items(discountVouchers) { voucher ->
                        val isEligible = currentSubTotal >= voucher.minOrderValue && voucher.usedCount < voucher.usageLimit
                        VoucherTicket(
                            voucher = voucher,
                            isSelected = selectedDiscount?.id == voucher.id,
                            isEligible = isEligible,
                            currentSubTotal = currentSubTotal,
                            iconBgColor = GunplaBlue,
                            icon = Icons.Default.ConfirmationNumber,
                            onSelect = { onSelectVoucher(voucher) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
@Composable
fun VoucherTicket(
    voucher: Voucher, isSelected: Boolean, isEligible: Boolean, currentSubTotal: Long,
    iconBgColor: Color, icon: ImageVector, onSelect: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val progress = if (voucher.usageLimit > 0) {
        (voucher.usedCount.toFloat() / voucher.usageLimit.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percentString = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isEligible) 1f else 0.5f)
            .clickable(enabled = isEligible) { onSelect() }
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .background(if (isEligible) iconBgColor else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (voucher.type == "FREESHIP") "FREESHIP" else "DISCOUNT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(voucher.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (isEligible) {
                    Text("Đơn tối thiểu ₫${formatter.format(voucher.minOrderValue)}", color = Color.Gray, fontSize = 11.sp)
                } else {
                    if (voucher.usedCount >= voucher.usageLimit) {
                        Text("Đã hết lượt sử dụng", color = AlertRed, fontSize = 11.sp)
                    } else {
                        Text("Mua thêm ₫${formatter.format(voucher.minOrderValue - currentSubTotal)} để áp dụng", color = AlertRed, fontSize = 11.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (progress >= 0.9f) AlertRed else iconBgColor, // Đỏ lên nếu sắp hết mã
                        trackColor = Color(0xFFEEEEEE)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đã dùng $percentString%", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = iconBgColor),
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
            )
        }
    }
}
@Composable
fun CheckoutTopBar(
    totalPrice: Long, productDiscount: Long, freeshipAmount: Long,
    totalSelectedCount: Int, isAllSelected: Boolean, modifier: Modifier = Modifier,
    onToggleAll: () -> Unit, onCheckout: () -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val isEnabled = totalPrice > 0 || totalSelectedCount > 0

    Surface(color = Color.White, shadowElevation = 4.dp, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onToggleAll() }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAllSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = GunplaBlue))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tất cả", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
                Text("Tổng thanh toán", color = Color.Gray, fontSize = 12.sp)
                Text("₫${if (totalPrice == 0L) "0" else formatter.format(totalPrice)}", color = if (isEnabled) AlertRed else Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (productDiscount > 0 || freeshipAmount > 0) {
                    val saved = productDiscount + freeshipAmount
                    Text("Tiết kiệm ₫${formatter.format(saved)}", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Button(
                onClick = onCheckout,
                enabled = isEnabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GunplaBlue),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 110.dp)
            ) {
                Text(
                    text = "MUA HÀNG ($totalSelectedCount)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedCartItem(
    item: CartItem,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDeleted by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { state ->
            if (state == SwipeToDismissBoxValue.EndToStart) {
                isDeleted = true
                coroutineScope.launch {
                    delay(300)
                    onDelete()
                }
                true
            } else false
        }
    )

    androidx.compose.animation.AnimatedVisibility(
        visible = !isDeleted,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
        ),
        // 🌟 1. GIẢM PADDING NGOÀI TỪ 16dp -> 8dp ĐỂ MỞ RỘNG DIỆN TÍCH
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                val color = if (isSwiping) AlertRed else Color.Transparent

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 2.dp) // Cách đều mép trên dưới
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isSwiping) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Xóa",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            content = {
                CartItemRow(item, onToggle, onIncrease, onDecrease, onQuantityChange)
            }
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onToggle: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val isAvailable = item.product.isActive
    Surface(
        color = if (isAvailable) Color.White else Color(0xFFF9FAFB),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 8.dp).fillMaxWidth()
                .alpha(if (isAvailable) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Checkbox(
                    checked = item.isSelected && isAvailable,
                    onCheckedChange = { if (isAvailable) onToggle() },
                    enabled = isAvailable,
                    colors = CheckboxDefaults.colors(checkedColor = GunplaBlue, uncheckedColor = Color.LightGray),
                    modifier = Modifier.scale(0.85f)
                )
            }
            Box(modifier = Modifier.size(80.dp)) {
                AsyncImage(
                    model = item.product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().border(0.5.dp, BorderGray, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                if (!isAvailable) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        Text("NGỪNG BÁN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f).height(80.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.product.name,
                    color = Color(0xFF222222),
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AutoResizePriceText(
                        price = item.product.price,
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )
                    if (isAvailable) {
                        Row(
                            modifier = Modifier
                                .height(26.dp)
                                .border(0.5.dp, BorderGray, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuantityBtn(Icons.Default.Remove, item.quantity > 1, onDecrease)
                            Box(modifier = Modifier.width(0.5.dp).fillMaxHeight().background(BorderGray))
                            InlineQuantityInput(item.quantity, item.product.stock, onQuantityChange)
                            Box(modifier = Modifier.width(0.5.dp).fillMaxHeight().background(BorderGray))
                            QuantityBtn(Icons.Default.Add, item.quantity < item.product.stock, onIncrease)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InlineQuantityInput(quantity: Int, maxStock: Int, onQuantityChange: (Int) -> Unit) {
    var textValue by remember(quantity) { mutableStateOf(quantity.toString()) }
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = textValue,
        onValueChange = { input ->
            if (input.isEmpty() || (input.all { it.isDigit() } && input.length <= 4)) {
                textValue = input
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                val newQty = textValue.toIntOrNull() ?: 1
                val finalQty = newQty.coerceIn(1, maxStock)
                textValue = finalQty.toString()
                onQuantityChange(finalQty)
                focusManager.clearFocus()
            }
        ),
        textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(GunplaBlue),
        modifier = Modifier.width(36.dp).wrapContentHeight(), // Rộng vừa đủ
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (textValue.isEmpty()) Text("1", color = Color.Transparent)
                innerTextField()
            }
        }
    )
}

@Composable
fun QuantityBtn(icon: ImageVector, isEnabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(26.dp) // Nút bấm vừa tay
            .fillMaxHeight()
            .background(if (isEnabled) Color.White else Color(0xFFFAFAFA))
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isEnabled) Color.DarkGray else Color(0xFFD0D0D0),
            modifier = Modifier.size(14.dp) // Icon bé gọn lại
        )
    }
}

@Composable
fun EmptyCartView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Giỏ hàng của bạn đang trống", color = Color.Gray, fontSize = 15.sp)
        }
    }
}
@Composable
fun AutoResizePriceText(
    price: Long,
    modifier: Modifier = Modifier
) {
    val formatter = DecimalFormat("#,###")
    val textString = "₫${formatter.format(price)}"

    var textSize by remember { mutableStateOf(15.sp) }

    Text(
        text = textString,
        color = AlertRed,
        fontWeight = FontWeight.Bold,
        fontSize = textSize,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                textSize *= 0.9f
            }
        }
    )
}