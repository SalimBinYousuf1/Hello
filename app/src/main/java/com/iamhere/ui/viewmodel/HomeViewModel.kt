package com.iamhere.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamhere.domain.model.NetworkStatus
import com.iamhere.domain.repository.MessageRepository
import com.iamhere.domain.repository.NetworkRepository
import com.iamhere.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    networkRepository: NetworkRepository,
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {
    val networkStatus = networkRepository.networkStatus.stateIn(viewModelScope, SharingStarted.Eagerly, NetworkStatus.Offline)
    val messages = messageRepository.getAllMessages().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun start() = Unit
    fun send(text: String, recipient: String) = viewModelScope.launch { sendMessageUseCase(text, recipient) }
    fun markRead(thread: String) = viewModelScope.launch { messageRepository.markAsRead(thread) }
}
