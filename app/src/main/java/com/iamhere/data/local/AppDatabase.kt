package com.iamhere.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PacketEntity::class, MessageEntity::class, ContactEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packetDao(): PacketDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
}
