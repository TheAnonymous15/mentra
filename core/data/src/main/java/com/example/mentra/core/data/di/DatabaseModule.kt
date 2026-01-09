package com.example.mentra.core.data.di

import android.content.Context
import androidx.room.Room
import com.example.mentra.core.data.local.MentraDatabase
import com.example.mentra.core.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database and DAO instances
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMentraDatabase(
        @ApplicationContext context: Context
    ): MentraDatabase {
        return Room.databaseBuilder(
            context,
            MentraDatabase::class.java,
            MentraDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // Health DAOs
    @Provides
    @Singleton
    fun provideActivityDao(database: MentraDatabase): ActivityDao {
        return database.activityDao()
    }

    @Provides
    @Singleton
    fun provideHealthStatsDao(database: MentraDatabase): HealthStatsDao {
        return database.healthStatsDao()
    }

    @Provides
    @Singleton
    fun provideSleepDao(database: MentraDatabase): SleepDao {
        return database.sleepDao()
    }

    // Navigation DAOs
    @Provides
    @Singleton
    fun provideSavedRouteDao(database: MentraDatabase): SavedRouteDao {
        return database.savedRouteDao()
    }

    @Provides
    @Singleton
    fun provideRoutePointDao(database: MentraDatabase): RoutePointDao {
        return database.routePointDao()
    }

    @Provides
    @Singleton
    fun providePOIDao(database: MentraDatabase): POIDao {
        return database.poiDao()
    }

    // Media DAOs
    @Provides
    @Singleton
    fun provideMediaItemDao(database: MentraDatabase): MediaItemDao {
        return database.mediaItemDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: MentraDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun providePlaylistItemDao(database: MentraDatabase): PlaylistItemDao {
        return database.playlistItemDao()
    }

    // Shell DAOs
    @Provides
    @Singleton
    fun provideShellHistoryDao(database: MentraDatabase): ShellHistoryDao {
        return database.shellHistoryDao()
    }

    @Provides
    @Singleton
    fun provideShellAliasDao(database: MentraDatabase): ShellAliasDao {
        return database.shellAliasDao()
    }

    @Provides
    @Singleton
    fun provideShellScriptDao(database: MentraDatabase): ShellScriptDao {
        return database.shellScriptDao()
    }

    @Provides
    @Singleton
    fun provideShellTriggerDao(database: MentraDatabase): ShellTriggerDao {
        return database.shellTriggerDao()
    }

    // User DAO
    @Provides
    @Singleton
    fun provideUserProfileDao(database: MentraDatabase): UserProfileDao {
        return database.userProfileDao()
    }
}

