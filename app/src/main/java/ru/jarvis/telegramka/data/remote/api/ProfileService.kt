package ru.jarvis.telegramka.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.*
import io.ktor.http.ContentDisposition.Companion.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import ru.jarvis.telegramka.BuildConfig
import ru.jarvis.telegramka.data.remote.model.ErrorResponse
import ru.jarvis.telegramka.data.remote.model.UpdateAvatarResponse
import ru.jarvis.telegramka.data.remote.model.UserDto
import ru.jarvis.telegramka.di.AuthClient
import java.io.File
import javax.inject.Inject

class ProfileService @Inject constructor(
    @AuthClient private val client: HttpClient
) {
    private val baseUrl = BuildConfig.API_BASE_URL

    suspend fun getProfile(): UserDto {
        val response = client.get("$baseUrl/profile")
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val error = response.body<ErrorResponse>()
            throw Exception(error.error)
        }
    }

    suspend fun updateAvatar(avatar: ByteArray): UpdateAvatarResponse {
        val response = client.post("$baseUrl/profile/avatar") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "avatar",
                            avatar,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"avatar.jpg\"")
                            }
                        )
                    }
                )
            )
        }

        if (response.status == HttpStatusCode.OK) {
            return response.body()
        } else {
            val error = response.body<ErrorResponse>()
            throw Exception(error.error)
        }
    }
}
