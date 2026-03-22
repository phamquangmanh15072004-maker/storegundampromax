package com.example.storepromax.domain.utils

import java.text.NumberFormat
import java.util.Locale

fun formatVietnameseCurrency(price: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(price)
}