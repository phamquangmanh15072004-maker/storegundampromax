package com.example.storepromax.presentation.profile.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.data.api.GHNRetrofit
import com.example.storepromax.domain.model.DistrictGHN
import com.example.storepromax.domain.model.ProvinceGHN
import com.example.storepromax.domain.model.WardGHN
import com.example.storepromax.domain.model.User
import com.example.storepromax.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _updateState = MutableStateFlow<String?>(null)
    val updateState = _updateState.asStateFlow()

    private val _provinces = MutableStateFlow<List<ProvinceGHN>>(emptyList())
    val provinces = _provinces.asStateFlow()

    private val _districts = MutableStateFlow<List<DistrictGHN>>(emptyList())
    val districts = _districts.asStateFlow()

    private val _wards = MutableStateFlow<List<WardGHN>>(emptyList())
    val wards = _wards.asStateFlow()

    val selectedProvince = MutableStateFlow<ProvinceGHN?>(null)
    val selectedDistrict = MutableStateFlow<DistrictGHN?>(null)
    val selectedWard = MutableStateFlow<WardGHN?>(null)

    val specificAddress = MutableStateFlow("")

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            fetchProvincesFromGHN()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                userRepository.getUserDetails(uid).onSuccess { user ->
                    _currentUser.value = user
                    if (user.specificAddress.isNotBlank()) {
                        specificAddress.value = user.specificAddress
                    } else if (user.shippingAddress.isNotBlank()) {
                        specificAddress.value = user.shippingAddress
                    }
                    if (user.provinceId != 0 && _provinces.value.isNotEmpty()) {
                        selectedProvince.value =
                            _provinces.value.find { it.provinceID == user.provinceId }
                        try {
                            val dRes = GHNRetrofit.api.getDistricts(user.provinceId)
                            if (dRes.code == 200 && dRes.data != null) {
                                _districts.value = dRes.data
                                selectedDistrict.value =
                                    dRes.data.find { it.districtID == user.districtId }
                                val wRes = GHNRetrofit.api.getWards(user.districtId)
                                if (wRes.code == 200 && wRes.data != null) {
                                    _wards.value = wRes.data
                                    selectedWard.value =
                                        wRes.data.find { it.wardCode == user.wardCode }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            _isLoading.value = false
        }
    }

    private suspend fun fetchProvincesFromGHN() {
        try {
            val response = GHNRetrofit.api.getProvinces()
            if (response.code == 200 && response.data != null) {
                _provinces.value = response.data
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onProvinceSelected(province: ProvinceGHN) {
        selectedProvince.value = province
        selectedDistrict.value = null
        selectedWard.value = null
        _districts.value = emptyList()
        _wards.value = emptyList()

        viewModelScope.launch {
            try {
                val response = GHNRetrofit.api.getDistricts(province.provinceID)
                if (response.code == 200 && response.data != null) {
                    _districts.value = response.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onDistrictSelected(district: DistrictGHN) {
        selectedDistrict.value = district
        selectedWard.value = null
        _wards.value = emptyList()

        viewModelScope.launch {
            try {
                val response = GHNRetrofit.api.getWards(district.districtID)
                if (response.code == 200 && response.data != null) {
                    _wards.value = response.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onWardSelected(ward: WardGHN) {
        selectedWard.value = ward
    }

    fun onSpecificAddressChange(value: String) {
        specificAddress.value = value
    }

    fun saveProfile(name: String, phone: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = _currentUser.value ?: return@launch

            var avatarUrl = currentUser.avatarUrl

            if (newImageUri != null) {
                val uploadResult = userRepository.uploadAvatar(newImageUri)
                if (uploadResult.isSuccess) {
                    avatarUrl = uploadResult.getOrNull() ?: avatarUrl
                }
            }
            val p = selectedProvince.value
            val d = selectedDistrict.value
            val w = selectedWard.value
            val s = specificAddress.value.trim()

            val pName = p?.provinceName ?: ""
            val dName = d?.districtName ?: ""
            val wName = w?.wardName ?: ""

            val finalAddress = if (pName.isNotBlank() && dName.isNotBlank() && wName.isNotBlank()) {
                "$s, $wName, $dName, $pName"
            } else {
                s
            }

            val updatedUser = currentUser.copy(
                name = name,
                phone = phone,
                avatarUrl = avatarUrl,
                shippingAddress = finalAddress,
                specificAddress = s,
                provinceId = p?.provinceID ?: 0,
                districtId = d?.districtID ?: 0,
                wardCode = w?.wardCode ?: "",
                provinceName = pName,
                districtName = dName,
                wardName = wName
            )

            userRepository.updateUser(updatedUser).onSuccess {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(if (avatarUrl.isNotBlank()) Uri.parse(avatarUrl) else null)
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)
                try {
                    val db = FirebaseFirestore.getInstance()
                    val batch = db.batch()
                    val uid = auth.currentUser?.uid ?: ""

                    val postsSnapshot = db.collection("posts").whereEqualTo("userId", uid).get().await()
                    for (doc in postsSnapshot.documents) {
                        batch.update(doc.reference, mapOf("userName" to name, "userAvatar" to avatarUrl))
                    }

                    val commentsSnapshot = db.collectionGroup("comments").whereEqualTo("userId", uid).get().await()
                    for (doc in commentsSnapshot.documents) {
                        batch.update(doc.reference, mapOf("userName" to name, "userAvatar" to avatarUrl))
                    }
                    batch.commit().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                _currentUser.value = updatedUser
                _updateState.value = "SUCCESS"
            }.onFailure {
                _updateState.value = "Lỗi: ${it.message}"
            }

            _isLoading.value = false
        }
    }

    fun resetState() {
        _updateState.value = null
    }
}