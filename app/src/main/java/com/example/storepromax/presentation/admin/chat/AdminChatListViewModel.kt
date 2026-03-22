package com.example.storepromax.presentation.admin.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.ChatChannel
import com.example.storepromax.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AdminChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    val currentAdminId = auth.currentUser?.uid ?: ""
    private val _allChannels = chatRepository.getSupportChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allChannels: StateFlow<List<ChatChannel>> = _allChannels

    val needsReplyChannels: StateFlow<List<ChatChannel>> = _allChannels.map { list ->
        list.filter { channel ->
            channel.lastSenderId != currentAdminId && channel.lastSenderId != "SYSTEM_BOT"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}