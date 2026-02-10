package com.example.storepromax.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _changePasswordState = MutableStateFlow<Result<String>?>(null)
    val changePasswordState = _changePasswordState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun changePassword(currentPass: String, newPass: String, confirmPass: String) {


        if (currentPass.isBlank() || newPass.isBlank() || confirmPass.isBlank()) {
            _changePasswordState.value = Result.failure(Exception("Vui lòng nhập đầy đủ thông tin."))
            return
        }

        if (newPass != confirmPass) {
            _changePasswordState.value = Result.failure(Exception("Mật khẩu xác nhận không trùng khớp."))
            return
        }

        if (newPass.length < 6) {
            _changePasswordState.value = Result.failure(Exception("Mật khẩu mới phải có ít nhất 6 ký tự."))
            return
        }

        if (currentPass == newPass) {
            _changePasswordState.value = Result.failure(Exception("Mật khẩu mới phải khác mật khẩu cũ."))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val user = auth.currentUser

            if (user != null && user.email != null) {
                try {
                    val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

                    user.reauthenticate(credential).await()

                    user.updatePassword(newPass).await()
                    auth.signOut()
                    _changePasswordState.value = Result.success("Đổi mật khẩu thành công! Vui lòng đăng nhập lại.")

                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    _changePasswordState.value = Result.failure(Exception("Mật khẩu hiện tại không chính xác."))
                } catch (e: Exception) {
                    e.printStackTrace()
                    _changePasswordState.value = Result.failure(e)
                }
            } else {
                _changePasswordState.value = Result.failure(Exception("Lỗi xác thực người dùng."))
            }
            _isLoading.value = false
        }
    }

    fun resetState() {
        _changePasswordState.value = null
    }
}