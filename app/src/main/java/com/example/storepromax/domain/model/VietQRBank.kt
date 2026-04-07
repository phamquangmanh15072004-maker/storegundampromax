package com.example.storepromax.domain.model

import com.google.gson.annotations.SerializedName

data class VietQRResponse(
    val code: String,
    val desc: String,
    val data: List<VietQRBank>
)

data class VietQRBank(
    val bin: String,
    val shortName: String,
    val logo: String,
    @SerializedName("short_name")
    val altShortName: String? = null
)