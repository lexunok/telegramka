package ru.jarvis.telegramka.data.remote

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    private const val BASE_URL = "http://10.0.2.2:3000/api"

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }

        // TODO: Add default request settings like headers (Authorization)
        // TODO: Add error handling plugin
    }

    fun getBaseUrl(): String = BASE_URL
}
