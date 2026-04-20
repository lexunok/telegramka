package ru.jarvis.telegramka.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Repositories are provided via @Inject constructor,
    // so no explicit @Provides functions are needed here.
}
