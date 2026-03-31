package com.example.storepromax.data.repository

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
}