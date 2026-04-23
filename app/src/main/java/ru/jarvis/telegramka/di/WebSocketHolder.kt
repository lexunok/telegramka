package ru.jarvis.telegramka.di

import io.ktor.client.HttpClient

// A wrapper class to help Hilt with providing a specific HttpClient instance.
data class WebSocketHolder(val client: HttpClient)
