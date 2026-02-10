package com.example.storepromax.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storepromax.domain.model.Product
import com.example.storepromax.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserChatViewModel @Inject constructor(
    private val chatRepo: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    val myChats = chatRepo.getUserChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentUserId = auth.currentUser?.uid ?: ""
    fun contactSeller(product: Product, sellerId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = chatRepo.createTradeChannel(sellerId, product)
            result.onSuccess { channelId ->
                onSuccess(channelId)
            }
        }
    }
}