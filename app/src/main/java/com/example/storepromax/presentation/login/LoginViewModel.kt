package com.example.storepromax.presentation.login

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.data.local.UserPreferences
import com.example.storepromax.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
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
        val currentEmail = email.value.trim()
        val currentPassword = password.value.trim()

        if (currentEmail.isEmpty() || currentPassword.isEmpty()) {
            _loginState.value = LoginState.Error("Vui lòng nhập đầy đủ Email và Mật khẩu!")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            _loginState.value = LoginState.Error("Định dạng Email không hợp lệ!")
            return
        }

        if (currentPassword.length < 6) {
            _loginState.value = LoginState.Error("Mật khẩu phải có ít nhất 6 ký tự!")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            try {
                val result = authRepository.login(currentEmail, currentPassword)

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
                                userPreferences.saveRememberInfo(currentEmail, isRemember.value)

                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val token = task.result
                                        FirebaseFirestore.getInstance().collection("users").document(userId)
                                            .set(mapOf("fcmToken" to token), SetOptions.merge())
                                    }
                                }

                                if (user.role == "ADMIN") {
                                    FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
                                    _loginState.value = LoginState.Success("admin")
                                } else {
                                    _loginState.value = LoginState.Success("user")
                                }
                            }
                        }.onFailure { e ->
                            _loginState.value = LoginState.Error("Không thể lấy thông tin người dùng: ${e.message}")
                            Log.e("LoginViewModel", "Error getting user details", e)
                            authRepository.logout()
                        }
                    } else {
                        _loginState.value = LoginState.Error("Lỗi: Không xác định được ID người dùng!")
                        authRepository.logout()
                    }
                }.onFailure {
                    _loginState.value = LoginState.Error("Thông tin đăng nhập không chính xác!")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Lỗi hệ thống hoặc mất mạng. Vui lòng thử lại!")
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
