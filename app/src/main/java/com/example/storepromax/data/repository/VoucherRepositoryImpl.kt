package com.example.storepromax.data.repository

import com.example.storepromax.domain.model.UserVoucher
import com.example.storepromax.domain.model.Voucher
import com.example.storepromax.domain.repository.VoucherRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class VoucherRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : VoucherRepository {

    override suspend fun saveVoucher(voucher: Voucher): Result<Boolean> {
        return try {
            val vouchersCol = firestore.collection("vouchers")
            if (voucher.id.isEmpty()) {
                val duplicateCheck = vouchersCol.whereEqualTo("code", voucher.code.uppercase().trim()).get().await()
                if (!duplicateCheck.isEmpty) {
                    return Result.failure(Exception("Mã Voucher này đã tồn tại trên hệ thống!"))
                }
            }
            val docRef = if (voucher.id.isEmpty()) {
                vouchersCol.document()
            } else {
                vouchersCol.document(voucher.id)
            }
            val finalVoucher = voucher.copy(
                id = docRef.id,
                code = voucher.code.uppercase().trim()
            )

            docRef.set(finalVoucher).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVoucherByCode(code: String): Result<Voucher> {
        return try {
            val snapshot = firestore.collection("vouchers")
                .whereEqualTo("code", code)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(Exception("Mã giảm giá không tồn tại!"))
            }

            val voucher = snapshot.documents.first().toObject(Voucher::class.java)
            if (voucher != null) {
                Result.success(voucher)
            } else {
                Result.failure(Exception("Lỗi dữ liệu voucher!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getUserVouchers(userId: String): Result<List<UserVoucher>> {
        return try {
            val snapshot = firestore.collection("user_vouchers")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val userVouchers = snapshot.toObjects(UserVoucher::class.java)
            Result.success(userVouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun claimVoucher(userId: String, voucher: Voucher): Result<Boolean> {
        return try {
            val expirationTime = (voucher.expirationDate as? Number)?.toLong() ?: 0L
            val deleteAtTime = if (expirationTime > 0) {
                expirationTime + (90L * 24 * 60 * 60 * 1000) // Hết hạn + 90 ngày
            } else {
                System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000) // Nếu ko HSD thì xóa sau 1 năm
            }
            val checkSnapshot = firestore.collection("user_vouchers")
                .whereEqualTo("userId", userId)
                .whereEqualTo("voucherId", voucher.id)
                .get()
                .await()

            if (!checkSnapshot.isEmpty) {
                return Result.failure(Exception("Bạn đã lưu mã này trong ví rồi!"))
            }
            val newUserVoucher = UserVoucher(
                userId = userId,
                voucherId = voucher.id,
                voucher = voucher,
                status = "AVAILABLE",
                deleteAt = deleteAtTime
            )
            firestore.collection("user_vouchers").document().set(newUserVoucher).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun updateVoucherStatus(voucherId: String, isActive: Boolean): Result<Boolean> {
        return try {
            firestore.collection("vouchers").document(voucherId)
                .update("isActive", isActive)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getAvailableVouchers(): Result<List<Voucher>> {
        return try {
            val currentTime = System.currentTimeMillis()
            val snapshot = firestore.collection("vouchers")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val vouchers = snapshot.toObjects(Voucher::class.java)
                // Lọc thêm: Còn hạn và Còn lượt
                .filter { it.expirationDate > currentTime && it.usedCount < it.usageLimit }

            Result.success(vouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getAllVouchersForAdmin(): Result<List<Voucher>> {
        return try {
            val snapshot = firestore.collection("vouchers").get().await()
            val vouchers = snapshot.toObjects(Voucher::class.java)
            Result.success(vouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getVouchersByIds(voucherIds: List<String>): Result<List<Voucher>> {
        return try {
            if (voucherIds.isEmpty()) return Result.success(emptyList())
            val chunks = voucherIds.chunked(10)
            val allVouchers = mutableListOf<Voucher>()
            for (chunk in chunks) {
                val snapshot = firestore.collection("vouchers")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk) // Tìm theo ID của Document
                    .get()
                    .await()
                allVouchers.addAll(snapshot.toObjects(Voucher::class.java))
            }

            Result.success(allVouchers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}