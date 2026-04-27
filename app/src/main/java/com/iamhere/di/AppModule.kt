package com.iamhere.di

import android.content.Context
import androidx.room.Room
import com.iamhere.data.local.AppDatabase
import com.iamhere.data.local.ContactDao
import com.iamhere.data.local.MessageDao
import com.iamhere.data.local.PacketDao
import com.iamhere.data.repository.ContactRepositoryImpl
import com.iamhere.data.repository.MessageRepositoryImpl
import com.iamhere.data.repository.NetworkRepositoryImpl
import com.iamhere.domain.repository.ContactRepository
import com.iamhere.domain.repository.MessageRepository
import com.iamhere.domain.repository.NetworkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "iamhere.db").fallbackToDestructiveMigration().build()

    @Provides fun packetDao(db: AppDatabase): PacketDao = db.packetDao()
    @Provides fun messageDao(db: AppDatabase): MessageDao = db.messageDao()
    @Provides fun contactDao(db: AppDatabase): ContactDao = db.contactDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds abstract fun bindMsg(impl: MessageRepositoryImpl): MessageRepository
    @Binds abstract fun bindContact(impl: ContactRepositoryImpl): ContactRepository
    @Binds abstract fun bindNetwork(impl: NetworkRepositoryImpl): NetworkRepository
}
