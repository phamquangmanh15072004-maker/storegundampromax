package com.example.storepromax.presentation.admin.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.User
import com.example.storepromax.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminUserViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userList = MutableStateFlow<List<User>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val currentAdminId = auth.currentUser?.uid ?: ""

    init {
        viewModelScope.launch {
            authRepository.getAllUsers().collect { firebaseUsers ->
                _userList.value = firebaseUsers
            }
        }
    }

    val users = combine(_userList, _searchQuery) { users, query ->
        if (query.isBlank()) users
        else users.filter {
            it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleUserLock(user: User) {
        viewModelScope.launch {
            if (user.id == currentAdminId) {
                _uiEvent.send("Không thể tự khóa tài khoản của chính mình!")
                return@launch
            }

            val newStatus = !user.isLocked


            _userList.update { currentList ->
                currentList.map {
                    if (it.id == user.id) it.copy(isLocked = newStatus) else it
                }
            }

            val result = authRepository.updateUserLockStatus(user.id, newStatus)

            if (result.isSuccess) {
                val msg = if (newStatus) "Đã khóa: ${user.name}" else "Đã mở: ${user.name}"
                _uiEvent.send(msg)
            } else {
                _userList.update { currentList ->
                    currentList.map {
                        if (it.id == user.id) it.copy(isLocked = !newStatus) else it
                    }
                }
                _uiEvent.send("Lỗi kết nối: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleUserRole(user: User) {
        viewModelScope.launch {
            if (user.id == currentAdminId) {
                _uiEvent.send("Không thể tự thay đổi quyền của chính mình!")
                return@launch
            }

            val newRole = if (user.role == "ADMIN") "USER" else "ADMIN"

            _userList.update { currentList ->
                currentList.map {
                    if (it.id == user.id) it.copy(role = newRole) else it
                }
            }

            val result = authRepository.updateUserRole(user.id, newRole)

            if (result.isSuccess) {
                _uiEvent.send("Đã cập nhật quyền thành: $newRole")
            } else {
                _userList.update { currentList ->
                    currentList.map {
                        if (it.id == user.id) it.copy(role = user.role) else it
                    }
                }
                _uiEvent.send("Lỗi cập nhật quyền!")
            }
        }
    }
}