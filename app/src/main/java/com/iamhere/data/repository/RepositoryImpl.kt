package com.iamhere.data.repository

import com.iamhere.data.local.ContactDao
import com.iamhere.data.local.ContactEntity
import com.iamhere.data.local.MessageDao
import com.iamhere.data.local.MessageEntity
import com.iamhere.data.network.MeshEngine
import com.iamhere.domain.model.NetworkStatus
import com.iamhere.domain.repository.ContactRepository
import com.iamhere.domain.repository.MessageRepository
import com.iamhere.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val dao: MessageDao,
    private val engine: MeshEngine
) : MessageRepository {
    override fun getAllMessages(): Flow<List<MessageEntity>> = dao.getAll()
    override fun getUnreadCount(): Flow<Int> = dao.unreadCount()
    override suspend fun sendMessage(text: String, recipient: String) = engine.sendMessage(text, recipient)
    override suspend fun markAsRead(thread: String) = dao.markThreadRead(thread)
}

@Singleton
class ContactRepositoryImpl @Inject constructor(private val dao: ContactDao) : ContactRepository {
    override fun contacts(): Flow<List<ContactEntity>> = dao.all()
    override suspend fun trust(pubKey: String, alias: String) = dao.insert(ContactEntity(pubKey, alias, pubKey, true))
    override suspend fun isTrusted(pubKey: String): Boolean = dao.byKey(pubKey)?.isTrusted == true
}

@Singleton
class NetworkRepositoryImpl @Inject constructor(private val engine: MeshEngine) : NetworkRepository {
    override val networkStatus: Flow<NetworkStatus> = engine.networkStatus.map {
        when {
            it.startsWith("Connected") -> NetworkStatus.Connected
            it == "Searching" -> NetworkStatus.Searching
            else -> NetworkStatus.Offline
        }
    }
}
