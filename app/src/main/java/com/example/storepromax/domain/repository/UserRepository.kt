package com.example.storepromax.domain.repository

import android.net.Uri
import com.example.storepromax.domain.model.User

interface UserRepository {
    suspend fun getUserDetails(userId: String): Result<User>
    suspend fun updateUser(user: User): Result<Boolean>
    suspend fun uploadAvatar(imageUri: Uri): Result<String>
}