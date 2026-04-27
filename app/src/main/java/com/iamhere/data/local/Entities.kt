package com.iamhere.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packets")
data class PacketEntity(
    @PrimaryKey val packetId: String,
    val rawBytes: ByteArray,
    val ttl: Int,
    val timestamp: Long
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isVerified: Boolean
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val pubKey: String,
    val alias: String,
    val qrCodeData: String,
    val isTrusted: Boolean
)
