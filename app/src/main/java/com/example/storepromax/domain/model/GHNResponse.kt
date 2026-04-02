package com.example.storepromax.domain.model

import com.google.gson.annotations.SerializedName

data class GHNResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)
data class ProvinceGHN(
    @SerializedName("ProvinceID") val provinceID: Int,
    @SerializedName("ProvinceName") val provinceName: String
)

data class DistrictGHN(
    @SerializedName("DistrictID") val districtID: Int,
    @SerializedName("ProvinceID") val provinceID: Int,
    @SerializedName("DistrictName") val districtName: String
)

data class WardGHN(
    @SerializedName("WardCode") val wardCode: String,
    @SerializedName("DistrictID") val districtID: Int,
    @SerializedName("WardName") val wardName: String
)
data class GHNFeeRequest(
    val service_type_id: Int = 2,
    val to_district_id: Int,
    val to_ward_code: String,
    val weight: Int,
    val insurance_value: Long
)
data class GHNFeeData(
    val total: Long
)