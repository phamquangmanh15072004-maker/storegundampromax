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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
            val provinceList = AddressUtils(context).getProvinces()
            _provinces.value = provinceList

            val uid = auth.currentUser?.uid
            if (uid != null) {
                userRepository.getUserDetails(uid).onSuccess { user ->
                    _currentUser.value = user

                    if (user.shippingAddress.isNotBlank()) {
                        parseAddressToDropdown(user.shippingAddress, provinceList)
                    }
                }
            }
        }
    }
    private fun parseAddressToDropdown(fullAddress: String, provinceList: List<Province>) {
        try { val parts = fullAddress.split(",").map { it.trim() }

            if (parts.size >= 3) {
                val pName = parts.last()
                val dName = parts[parts.size - 2]
                val wName = parts[parts.size - 3]

                val specific = parts.take(parts.size - 3).joinToString(", ")
                specificAddress.value = specific
                val foundProvince = provinceList.find { it.name.equals(pName, ignoreCase = true) }
                if (foundProvince != null) {
                    _selectedProvince.value = foundProvince
                    val districtList = foundProvince.getDistrictList()
                    _districts.value = districtList
                    val foundDistrict = districtList.find { it.name.equals(dName, ignoreCase = true) }
                    if (foundDistrict != null) {
                        _selectedDistrict.value = foundDistrict

                        val wardList = foundDistrict.getWardList()
                        _wards.value = wardList

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

                try {
                    val db = FirebaseFirestore.getInstance()
                    val batch = db.batch()
                    val uid = auth.currentUser?.uid ?: ""
                    val postsSnapshot = db.collection("posts")
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()

                    for (doc in postsSnapshot.documents) {
                        batch.update(doc.reference, mapOf(
                            "userName" to name,
                            "userAvatar" to avatarUrl
                        ))
                    }

                    val commentsSnapshot = db.collectionGroup("comments")
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()

                    for (doc in commentsSnapshot.documents) {
                        batch.update(doc.reference, mapOf(
                            "userName" to name,
                            "userAvatar" to avatarUrl
                        ))
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

    fun resetState() { _updateState.value = null }
}