package com.iamhere.domain.usecase

import com.iamhere.data.local.MessageEntity
import com.iamhere.domain.repository.ContactRepository
import com.iamhere.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(private val repo: MessageRepository) {
    suspend operator fun invoke(text: String, recipient: String) = repo.sendMessage(text, recipient)
}

class GetMessagesUseCase @Inject constructor(private val repo: MessageRepository) {
    operator fun invoke(): Flow<List<MessageEntity>> = repo.getAllMessages()
}

class VerifyContactUseCase @Inject constructor(private val repo: ContactRepository) {
    suspend operator fun invoke(pubKey: String): Boolean = repo.isTrusted(pubKey)
}
