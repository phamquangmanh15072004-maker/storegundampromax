package com.example.storepromax.presentation.admin.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.ChatChannel
import com.example.storepromax.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AdminChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _allChannels = chatRepository.getSupportChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingChannels: StateFlow<List<ChatChannel>> = _allChannels.map { list ->
        list.filter { it.status == "PENDING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val processingChannels: StateFlow<List<ChatChannel>> = _allChannels.map { list ->
        list.filter { it.status == "PROCESSING" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val solvedChannels: StateFlow<List<ChatChannel>> = _allChannels.map { list ->
        list.filter { it.status == "SOLVED" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}