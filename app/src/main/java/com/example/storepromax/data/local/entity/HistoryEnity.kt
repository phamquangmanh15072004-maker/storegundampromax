package com.example.storepromax.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "view_history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val title: String,
    val content: String,
    val price: Long,
    val images: List<String>,
    val condition: String,
    val grade: String,

    val likeCount: Int,
    val commentCount: Int,
    val status: String,

    val createdAt: Long,
    val viewedAt: Long = System.currentTimeMillis()
)