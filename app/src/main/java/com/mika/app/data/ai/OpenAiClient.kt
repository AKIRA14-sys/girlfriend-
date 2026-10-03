package com.mika.app.data.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiClient : AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    override suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        messages: List<Map<String, Any>>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API key is missing. Please set your API key in Settings.")
        }

        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.endsWith("/chat/completions")) cleanBaseUrl else "$cleanBaseUrl/chat/completions"

        val requestBodyMap = mapOf(
            "model" to modelName,
            "messages" to messages
        )

        val jsonBody = gson.toJson(requestBodyMap)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(bodyString, response.code)
                throw IOException(errorMsg)
            }

            try {
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                val choices = jsonObject.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    val firstChoice = choices.get(0).asJsonObject
                    val message = firstChoice.getAsJsonObject("message")
                    return@withContext message.get("content").asString.trim()
                } else {
                    throw IOException("Empty response choices received from AI service.")
                }
            } catch (e: Exception) {
                if (e is IOException) throw e
                throw IOException("Failed to parse AI response: ${e.localizedMessage}")
            }
        }
    }

    override suspend fun extractMemories(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        userMessage: String,
        assistantReply: String
    ): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()

        val extractionPrompt = """
            Extract any short lasting personal facts worth remembering about the user from this recent chat exchange (e.g. people introduced with short description, favorite games, goals, dates, preferences).
            Return ONLY a JSON array of strings, for example: ["Friend: Rahul (gamer friend)", "Favorite game: Valorant"].
            If there are no new lasting facts or it is trivial conversation, return [].
            User message: "$userMessage"
            Assistant reply: "$assistantReply"
        """.trimIndent()

        val messages = listOf(
            mapOf("role" to "user", "content" to extractionPrompt)
        )

        return@withContext try {
            val rawReply = sendMessage(baseUrl, apiKey, modelName, messages)
            parseMemoriesResponse(rawReply)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMemoriesResponse(raw: String): List<String> {
        var clean = raw.trim()
        if (clean.startsWith("```")) {
            val firstLineEnd = clean.indexOf('\n')
            if (firstLineEnd != -1) {
                clean = clean.substring(firstLineEnd + 1)
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length - 3)
            }
            clean = clean.trim()
        }

        return try {
            val jsonArray = gson.fromJson(clean, JsonArray::class.java)
            val list = mutableListOf<String>()
            for (element in jsonArray) {
                val fact = element.asString.trim()
                if (fact.isNotEmpty()) {
                    list.add(fact)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val obj = gson.fromJson(body, JsonObject::class.java)
            if (obj.has("error")) {
                val errorObj = obj.get("error")
                if (errorObj.isJsonObject) {
                    val msg = errorObj.asJsonObject.get("message")?.asString
                    if (!msg.isNullOrBlank()) return msg
                } else if (errorObj.isJsonPrimitive) {
                    return errorObj.asString
                }
            }
            "Server returned error code $code"
        } catch (e: Exception) {
            "HTTP error $code"
        }
    }
}
