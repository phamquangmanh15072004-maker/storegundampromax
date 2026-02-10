package com.example.storepromax.domain.model

import com.google.gson.annotations.SerializedName


data class Province(
    @SerializedName("name") val name: String = "",
    @SerializedName("code") val code: String = "",
    @SerializedName("quan-huyen") val districts: Map<String, District> = emptyMap()
) {
    fun getDistrictList(): List<District> = districts.values.toList().sortedBy { it.name }
}

data class District(
    @SerializedName("name") val name: String = "",
    @SerializedName("code") val code: String = "",
    @SerializedName("xa-phuong") val wards: Map<String, Ward> = emptyMap()
) {
    fun getWardList(): List<Ward> = wards.values.toList().sortedBy { it.name }
}

data class Ward(
    @SerializedName("name") val name: String = "",
    @SerializedName("code") val code: String = "",
    @SerializedName("path_with_type") val fullName: String = ""
)