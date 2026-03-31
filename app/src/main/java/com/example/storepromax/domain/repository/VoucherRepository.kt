package com.example.storepromax.domain.repository

import com.example.storepromax.domain.model.Voucher

interface VoucherRepository {
    suspend fun getAvailableVouchers(): Result<List<Voucher>>

    suspend fun getVoucherByCode(code: String): Result<Voucher>
}