package com.example.mentra.shell.messaging.di

import android.content.Context
import com.example.mentra.shell.messaging.ContactAliasManager
import com.example.mentra.shell.messaging.ShellMessagingCommandHandler
import com.example.mentra.shell.messaging.ShellMessagingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for shell messaging dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object ShellMessagingModule {

    @Provides
    @Singleton
    fun provideContactAliasManager(
        @ApplicationContext context: Context
    ): ContactAliasManager {
        return ContactAliasManager(context)
    }

    @Provides
    @Singleton
    fun provideShellMessagingService(
        @ApplicationContext context: Context,
        aliasManager: ContactAliasManager
    ): ShellMessagingService {
        return ShellMessagingService(context, aliasManager)
    }

    @Provides
    @Singleton
    fun provideShellMessagingCommandHandler(
        messagingService: ShellMessagingService,
        aliasManager: ContactAliasManager
    ): ShellMessagingCommandHandler {
        return ShellMessagingCommandHandler(messagingService, aliasManager)
    }
}

