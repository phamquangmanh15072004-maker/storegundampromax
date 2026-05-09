package com.example.storepromax.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer

fun removeVietnameseTones(str: String): String {
    val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .replace('đ', 'd').replace('Đ', 'D')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableDropdown(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    onClearSelection: (() -> Unit)? = null,
    itemToString: (T) -> String
) {
    val selectedText = selectedItem?.let { itemToString(it) } ?: ""
    var searchText by remember { mutableStateOf(selectedText) }
    var expanded by remember { mutableStateOf(false) }
    var isUserTyping by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(selectedItem) {
        if (!isUserTyping) {
            searchText = selectedText
        }
    }
    LaunchedEffect(items) {
        if (selectedItem == null) {
            isUserTyping = false
            searchText = ""
        }
    }

    val filteredItems = remember(items, searchText) {
        val searchNormalized = removeVietnameseTones(searchText.trim().lowercase())
        if (searchNormalized.isEmpty()) items
        else items.filter {
            val itemNormalized = removeVietnameseTones(itemToString(it).lowercase())
            itemNormalized.contains(searchNormalized)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (it) expanded = true
        }
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                isUserTyping = true
                searchText = it
                expanded = true
            },
            label = { Text(label, color = Color.Gray, fontSize = 13.sp) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = {
                        isUserTyping = false
                        searchText = ""
                        expanded = false
                        focusManager.clearFocus()
                        onClearSelection?.invoke()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0D47A1),
                unfocusedBorderColor = Color.LightGray
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        if (expanded && items.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    isUserTyping = false
                    searchText = selectedText
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .background(Color.White)
                    .heightIn(max = 250.dp)
            ) {
                if (filteredItems.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Không tìm thấy kết quả", color = Color.Gray) },
                        onClick = {}
                    )
                } else {
                    filteredItems.take(50).forEach { item ->
                        val itemText = itemToString(item)
                        DropdownMenuItem(
                            text = { Text(itemText, color = Color.Black) },
                            onClick = {
                                isUserTyping = false
                                searchText = itemText
                                expanded = false
                                focusManager.clearFocus()
                                onItemSelected(item)
                            }
                        )
                    }
                }
            }
        }
    }
}