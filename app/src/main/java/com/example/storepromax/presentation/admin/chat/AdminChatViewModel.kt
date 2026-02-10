package com.example.storepromax.presentation.admin.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.ChatMessage
import com.example.storepromax.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository
) : ViewModel() {
    private val _allChannels = chatRepo.getSupportChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingChats = _allChannels.map { list ->
        list.filter { it.status == "PENDING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val processingChats = _allChannels.map { list ->
        list.filter { it.status == "PROCESSING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    fun loadMessages(channelId: String) {
        viewModelScope.launch {
            chatRepo.getMessages(channelId).collect {
                _messages.value = it
            }
        }
    }

    fun joinChat(channelId: String) {
        viewModelScope.launch {
            chatRepo.updateChannelStatus(channelId, "PROCESSING")
        }
    }
    fun sendMessage(channelId: String, content: String) {
        viewModelScope.launch {
            chatRepo.sendMessage(channelId, content, isAdmin = true)
        }
    }
    fun closeTicket(channelId: String) {
        viewModelScope.launch {
            chatRepo.updateChannelStatus(channelId, "SOLVED")
        }
    }
}