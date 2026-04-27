package com.iamhere.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PacketDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(packet: PacketEntity)

    @Query("SELECT packetId FROM packets")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM packets WHERE packetId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<PacketEntity>

    @Query("SELECT * FROM packets WHERE packetId = :id")
    suspend fun getById(id: String): PacketEntity?

    @Query("DELETE FROM packets WHERE timestamp < :before")
    suspend fun deleteOld(before: Long)

    @Query("DELETE FROM packets")
    suspend fun clear()
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE senderId = :thread ORDER BY timestamp ASC")
    fun getAllByThread(thread: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE isRead = 0")
    fun unreadCount(): Flow<Int>

    @Query("UPDATE messages SET isRead = 1 WHERE senderId = :thread")
    suspend fun markThreadRead(thread: String)

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Query("SELECT * FROM contacts")
    fun all(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE pubKey = :pubKey")
    suspend fun byKey(pubKey: String): ContactEntity?

    @Query("DELETE FROM contacts")
    suspend fun clear()
}
