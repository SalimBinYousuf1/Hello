package com.iamhere.domain.repository

import com.iamhere.data.local.ContactEntity
import com.iamhere.data.local.MessageEntity
import com.iamhere.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getAllMessages(): Flow<List<MessageEntity>>
    fun getUnreadCount(): Flow<Int>
    suspend fun sendMessage(text: String, recipient: String)
    suspend fun markAsRead(thread: String)
}

interface ContactRepository {
    fun contacts(): Flow<List<ContactEntity>>
    suspend fun trust(pubKey: String, alias: String)
    suspend fun isTrusted(pubKey: String): Boolean
}

interface NetworkRepository {
    val networkStatus: Flow<NetworkStatus>
}
