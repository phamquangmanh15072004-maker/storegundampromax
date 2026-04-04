package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.UserVoucher
import com.example.storepromax.domain.model.Voucher

interface VoucherRepository {
    suspend fun getAvailableVouchers(): Result<List<Voucher>>

    suspend fun getVoucherByCode(code: String): Result<Voucher>
    suspend fun getUserVouchers(userId: String): Result<List<UserVoucher>>
    suspend fun claimVoucher(userId: String, voucher: Voucher): Result<Boolean>
    suspend fun updateVoucherStatus(voucherId: String, isActive: Boolean): Result<Boolean>
    suspend fun saveVoucher(voucher: Voucher): Result<Boolean>
    suspend fun getAllVouchersForAdmin(): Result<List<Voucher>>
}