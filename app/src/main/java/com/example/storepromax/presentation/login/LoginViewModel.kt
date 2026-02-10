package com.example.storepromax.presentation.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.data.local.UserPreferences
import com.example.storepromax.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _loginState = mutableStateOf<LoginState>(LoginState.Idle)
    val loginState: State<LoginState> = _loginState

    val email = mutableStateOf("")
    val password = mutableStateOf("")

    val isRemember = mutableStateOf(false)

    init {
        email.value = userPreferences.getSavedEmail()
        isRemember.value = userPreferences.isRemembered()
    }

    fun login() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val result = authRepository.login(email.value, password.value)

            result.onSuccess {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid

                if (userId != null) {

                    val userResult = authRepository.getUserDetails(userId)

                    userResult.onSuccess { user ->
                        if (user.isLocked) {
                            authRepository.logout()

                            val reason = if (user.lockReason.isNotEmpty()) "\nLý do: ${user.lockReason}" else ""
                            _loginState.value = LoginState.Error("Tài khoản của bạn đã bị vô hiệu hóa.$reason")
                        } else {

                            userPreferences.saveRememberInfo(email.value, isRemember.value)

                            if (user.role == "ADMIN") {
                                _loginState.value = LoginState.Success("admin")
                            } else {
                                _loginState.value = LoginState.Success("user")
                            }
                        }
                    }.onFailure { e ->
                        _loginState.value = LoginState.Error("Không thể lấy thông tin người dùng: ${e.message}")
                        authRepository.logout()
                    }
                }
            }.onFailure {
                _loginState.value = LoginState.Error(it.message ?: "Đăng nhập thất bại")
            }
        }
    }
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class Success(val role: String) : LoginState
    data class Error(val message: String) : LoginState
}