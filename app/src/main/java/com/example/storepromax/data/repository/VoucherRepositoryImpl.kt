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

    override suspend fun getAvailableVouchers(): Result<List<Voucher>> {
        return try {
            val snapshot = firestore.collection("vouchers")
                .get()
                .await()
            val vouchers = snapshot.toObjects(Voucher::class.java)
            Result.success(vouchers)
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
                status = "AVAILABLE"
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
                .update("isActive", isActive).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveVoucher(voucher: Voucher): Result<Boolean> {
        return try {
            val docRef = if (voucher.id.isEmpty()) {
                firestore.collection("vouchers").document()
            } else {
                firestore.collection("vouchers").document(voucher.id)
            }
            docRef.set(voucher).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}