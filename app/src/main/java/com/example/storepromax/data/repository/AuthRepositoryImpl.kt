package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.User
import com.example.storepromax.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    override suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Email không tồn tại hoặc bị khóa!"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Sai email hoặc mật khẩu!"))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Lỗi kết nối. Vui lòng kiểm tra mạng!"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun register(email: String, pass: String, name: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            val userId = user?.uid ?: throw Exception("Tạo User thất bại")
            if (user != null) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name).build()

                user.updateProfile(profileUpdates).await()
                user.reload().await()
            }
            val userMap = hashMapOf(
                "id" to userId,
                "email" to email,
                "name" to name,
                "role" to "user",
                "phone" to "",
                "shippingAddress" to "",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(userId).set(userMap).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email này đã được đăng ký rồi!"))
        }
        catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Mật khẩu quá yếu! Vui lòng nhập ít nhất 6 ký tự."))
        }catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getUserProfile(userId: String): User? {
        return try {
            firestore.collection("users").document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) { null }
    }

    override suspend fun updateUserShippingInfo(userId: String, name: String, phone: String, address: String) {
        val updates = mapOf(
            "name" to name,
            "phone" to phone,
            "shippingAddress" to address
        )
        firestore.collection("users").document(userId).update(updates).await()
    }
    override suspend fun getUserRole(userId: String): String? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            document.getString("role") ?: "user"
        } catch (e: Exception) {
            null
        }
    }
    override fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection("users")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun updateUserLockStatus(userId: String, isLocked: Boolean): Result<Boolean> {
        return try {
            firestore.collection("users").document(userId)
                .update("isLocked", isLocked)
                .await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateUserRole(userId: String, newRole: String): Result<Boolean> {
        return try {
            firestore.collection("users").document(userId)
                .update("role", newRole)
                .await()
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }
    override fun logout() {
        auth.signOut()
    }
    override suspend fun getUserDetails(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()

            val user = snapshot.toObject(User::class.java)?.copy(id = snapshot.id)

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Không tìm thấy thông tin người dùng"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}