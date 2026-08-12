package ru.quasaris.characternexus.backend

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import ru.quasaris.characternexus.model.Character

object LssAvatarService {
    private val client = HttpClient()

    suspend fun downloadAvatar(character: Character): ByteArray? {
        val url = character.avatarUrl ?: return null
        return try {
            val response = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                response.readRawBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
