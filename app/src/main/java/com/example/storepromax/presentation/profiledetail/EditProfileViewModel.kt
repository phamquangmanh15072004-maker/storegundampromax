package com.example.storepromax.presentation.profile.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.District
import com.example.storepromax.domain.model.Province
import com.example.storepromax.domain.model.Ward
import com.example.storepromax.domain.model.User // ⚠️ Quan trọng: Import đúng Model User của bạn
import com.example.storepromax.domain.repository.UserRepository
import com.example.storepromax.utils.AddressUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _updateState = MutableStateFlow<String?>(null)
    val updateState = _updateState.asStateFlow()

    // --- DATA ĐỊA CHỈ ---
    private val _provinces = MutableStateFlow<List<Province>>(emptyList())
    val provinces = _provinces.asStateFlow()

    private val _districts = MutableStateFlow<List<District>>(emptyList())
    val districts = _districts.asStateFlow()

    private val _wards = MutableStateFlow<List<Ward>>(emptyList())
    val wards = _wards.asStateFlow()

    private val _selectedProvince = MutableStateFlow<Province?>(null)
    val selectedProvince = _selectedProvince.asStateFlow()

    private val _selectedDistrict = MutableStateFlow<District?>(null)
    val selectedDistrict = _selectedDistrict.asStateFlow()

    private val _selectedWard = MutableStateFlow<Ward?>(null)
    val selectedWard = _selectedWard.asStateFlow()

    val specificAddress = MutableStateFlow("")

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 1. Load danh sách Tỉnh
            val provinceList = AddressUtils(context).getProvinces()
            _provinces.value = provinceList

            val uid = auth.currentUser?.uid
            if (uid != null) {
                userRepository.getUserDetails(uid).onSuccess { user ->
                    // ✅ Hết lỗi Type Mismatch ở đây
                    _currentUser.value = user

                    // Parse địa chỉ cũ vào Dropdown
                    if (user.shippingAddress.isNotBlank()) {
                        parseAddressToDropdown(user.shippingAddress, provinceList)
                    }
                }
            }
        }
    }

    // --- LOGIC PARSE ĐỊA CHỈ ---
    private fun parseAddressToDropdown(fullAddress: String, provinceList: List<Province>) {
        try {
            // Giả sử: "Số 12, Phường X, Quận Y, Tỉnh Z"
            val parts = fullAddress.split(",").map { it.trim() }

            if (parts.size >= 3) {
                val pName = parts.last() // Tỉnh
                val dName = parts[parts.size - 2] // Huyện
                val wName = parts[parts.size - 3] // Xã

                val specific = parts.take(parts.size - 3).joinToString(", ")
                specificAddress.value = specific

                // 1. Tìm Tỉnh
                val foundProvince = provinceList.find { it.name.equals(pName, ignoreCase = true) }
                if (foundProvince != null) {
                    // ✅ Hết lỗi Unresolved reference ở đây
                    _selectedProvince.value = foundProvince

                    // 2. Load Huyện
                    val districtList = foundProvince.getDistrictList()
                    _districts.value = districtList

                    // 3. Tìm Huyện
                    val foundDistrict = districtList.find { it.name.equals(dName, ignoreCase = true) }
                    if (foundDistrict != null) {
                        _selectedDistrict.value = foundDistrict

                        // 4. Load Xã
                        val wardList = foundDistrict.getWardList()
                        _wards.value = wardList

                        // 5. Tìm Xã
                        val foundWard = wardList.find { it.name.equals(wName, ignoreCase = true) }
                        if (foundWard != null) {
                            _selectedWard.value = foundWard
                        }
                    }
                }
            } else {
                specificAddress.value = fullAddress
            }
        } catch (e: Exception) {
            specificAddress.value = fullAddress
        }
    }

    // --- CÁC HÀM SỰ KIỆN CHỌN ---
    fun onProvinceSelected(province: Province) {
        _selectedProvince.value = province
        _selectedDistrict.value = null
        _selectedWard.value = null
        _districts.value = province.getDistrictList()
        _wards.value = emptyList()
    }

    fun onDistrictSelected(district: District) {
        _selectedDistrict.value = district
        _selectedWard.value = null
        _wards.value = district.getWardList()
    }

    fun onWardSelected(ward: Ward) {
        _selectedWard.value = ward
    }

    fun onSpecificAddressChange(value: String) {
        specificAddress.value = value
    }

    // --- LƯU THÔNG TIN ---
    fun saveProfile(name: String, phone: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = _currentUser.value ?: return@launch // Lấy từ biến User Model

            var avatarUrl = currentUser.avatarUrl

            // 1. Upload ảnh
            if (newImageUri != null) {
                val uploadResult = userRepository.uploadAvatar(newImageUri)
                if (uploadResult.isSuccess) {
                    avatarUrl = uploadResult.getOrNull() ?: avatarUrl
                }
            }

            val p = _selectedProvince.value?.name ?: ""
            val d = _selectedDistrict.value?.name ?: ""
            val w = _selectedWard.value?.name ?: ""
            val s = specificAddress.value

            val finalAddress = if (p.isNotBlank() && d.isNotBlank() && w.isNotBlank()) {
                "$s, $w, $d, $p"
            } else {
                s
            }
            val updatedUser = currentUser.copy(
                name = name,
                phone = phone,
                shippingAddress = finalAddress,
                avatarUrl = avatarUrl
            )

            userRepository.updateUser(updatedUser).onSuccess {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(if (avatarUrl.isNotBlank()) Uri.parse(avatarUrl) else null)
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)

                _currentUser.value = updatedUser
                _updateState.value = "SUCCESS"
            }.onFailure {
                _updateState.value = "Lỗi: ${it.message}"
            }

            _isLoading.value = false
        }
    }

    fun resetState() { _updateState.value = null }
}