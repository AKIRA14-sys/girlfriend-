package com.mika.app.data.ai

interface AiClient {
    suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        messages: List<Map<String, Any>>
    ): String

    suspend fun extractMemories(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        userMessage: String,
        assistantReply: String
    ): List<String>
}
