package com.example.storepromax.presentation.register

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerState = mutableStateOf(RegisterState())
    val registerState: State<RegisterState> = _registerState
    fun register(email: String, pass: String, name: String, confirmPass:String) {
        if(email.isBlank() || pass.isBlank() || name.isBlank()|| confirmPass.isBlank()) {
            _registerState.value = RegisterState(error = "Vui lòng điền đầy đủ thông tin")
            return
        }
        if(pass != confirmPass) {
            _registerState.value = RegisterState(error = "Mật khẩu xác nhận không khớp!")
            return
        }
        if(pass.length < 6){
            _registerState.value = RegisterState(error = "Mật khẩu phải có ít nhất 6 ký tự!")
            return
        }
        viewModelScope.launch {
            _registerState.value = RegisterState(isLoading = true)
            val result = authRepository.register(email, pass, name)

            if (result.isSuccess) {
                _registerState.value = RegisterState(isSuccess = true)
            } else {
                _registerState.value = RegisterState(
                    error = result.exceptionOrNull()?.message ?: "Đăng Ký Thất Bại"
                )
            }
        }
    }
}

