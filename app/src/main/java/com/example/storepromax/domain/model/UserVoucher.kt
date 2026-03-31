package com.example.storepromax.domain.model

import com.google.firebase.firestore.DocumentId

data class UserVoucher(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val voucherId: String = "",
    val voucher: Voucher = Voucher(),
    val status: String = "AVAILABLE",
    val claimedAt: Long = System.currentTimeMillis()
)