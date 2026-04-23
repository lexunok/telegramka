package ru.jarvis.telegramka.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.jarvis.telegramka.data.storage.ITokenManager
import ru.jarvis.telegramka.data.storage.TokenManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TokenManagerModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): ITokenManager {
        TokenManager.initialize(context)
        return TokenManager
    }
}
