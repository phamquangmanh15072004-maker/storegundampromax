package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.User
import kotlinx.coroutines.flow.Flow


interface AuthRepository {

    suspend fun login(email: String, pass: String): Result<Unit>

    suspend fun register(email: String, pass: String, name: String): Result<Unit>
    suspend fun getUserProfile(userId: String): User?
    suspend fun updateUserShippingInfo(userId: String, name: String, phone: String, address: String)
    suspend fun getUserRole(userId: String): String?
    fun getAllUsers(): Flow<List<User>>
    suspend fun updateUserLockStatus(userId: String, isLocked: Boolean): Result<Boolean>
    fun logout()
    suspend fun updateUserRole(userId: String, newRole: String): Result<Boolean>
    suspend fun getUserDetails(userId: String): Result<User>
}