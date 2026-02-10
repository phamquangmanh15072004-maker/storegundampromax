package com.example.storepromax.utils

import android.content.Context
import com.example.storepromax.domain.model.Province
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddressUtils(private val context: Context) {

    suspend fun getProvinces(): List<Province> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("tree.json").bufferedReader().use { it.readText() }

                val type = object : TypeToken<Map<String, Province>>() {}.type
                val data: Map<String, Province> = Gson().fromJson(jsonString, type)
                data.values.toList().sortedBy { it.name }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}