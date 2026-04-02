package com.example.storepromax.data.api

import retrofit2.http.Query
import com.example.storepromax.domain.model.DistrictGHN
import com.example.storepromax.domain.model.GHNFeeData
import com.example.storepromax.domain.model.GHNFeeRequest
import com.example.storepromax.domain.model.GHNResponse
import com.example.storepromax.domain.model.ProvinceGHN
import com.example.storepromax.domain.model.WardGHN
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface GHNApi {
    @GET("master-data/province")
    suspend fun getProvinces(): GHNResponse<List<ProvinceGHN>>

    @GET("master-data/district")
    suspend fun getDistricts(@Query("province_id") provinceId: Int): GHNResponse<List<DistrictGHN>>

    @GET("master-data/ward")
    suspend fun getWards(@Query("district_id") districtId: Int): GHNResponse<List<WardGHN>>

    @POST("v2/shipping-order/fee")
    suspend fun calculateFee(
        @Header("ShopId") shopId: Int,
        @Body request: GHNFeeRequest
    ): GHNResponse<GHNFeeData>
}