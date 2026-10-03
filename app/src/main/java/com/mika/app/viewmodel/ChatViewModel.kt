package com.mika.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mika.app.data.AppPreferences
import com.mika.app.data.SystemPromptBuilder
import com.mika.app.data.ai.AiClient
import com.mika.app.data.ai.OpenAiClient
import com.mika.app.data.db.AppDatabase
import com.mika.app.data.db.ChatMessage
import com.mika.app.data.db.Memory
import com.mika.app.domain.ActionTag
import com.mika.app.domain.ResponseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Error(val message: String, val lastFailedMessage: String? = null) : ChatUiState()
}

class ChatViewModel(
    private val database: AppDatabase,
    private val preferences: AppPreferences,
    private val aiClient: AiClient = OpenAiClient()
) : ViewModel() {

    private val chatDao = database.chatMessageDao()
    private val memoryDao = database.memoryDao()

    val searchQuery = MutableStateFlow("")

    val messages: StateFlow<List<ChatMessage>> = combine(
        chatDao.getAllMessages(),
        searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.content.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _pendingAction = MutableStateFlow<ActionTag?>(null)
    val pendingAction: StateFlow<ActionTag?> = _pendingAction.asStateFlow()

    var onSpeakReplyRequested: ((String) -> Unit)? = null
    var onEmotionChanged: ((String) -> Unit)? = null
    var onGestureTriggered: ((String) -> Unit)? = null

    fun sendMessage(userText: String, imageBase64: String? = null) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            chatDao.insertMessage(ChatMessage(sender = "user", content = userText))

            executeAiCall(userText, imageBase64)
        }
    }

    fun retryLastMessage(userText: String) {
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            executeAiCall(userText, null)
        }
    }

    private suspend fun executeAiCall(userText: String, imageBase64: String?) {
        val apiKey = preferences.apiKey
        if (apiKey.isBlank()) {
            _uiState.value = ChatUiState.Error("API key is missing. Please enter your API key in Settings.", userText)
            return
        }

        try {
            val memories = memoryDao.getAllMemories()
            val systemPrompt = SystemPromptBuilder.buildSystemPrompt(
                companionName = preferences.companionName,
                personalityText = preferences.personalityText,
                attitudeLevel = preferences.attitudeLevel,
                memories = memories,
                cameraActive = preferences.cameraEnabled,
                cameraHasFrame = imageBase64 != null
            )

            val recentMessages = chatDao.getLastMessages(20).reversed()
            val apiMessages = mutableListOf<Map<String, Any>>()
            apiMessages.add(mapOf("role" to "system", "content" to systemPrompt))

            for (msg in recentMessages) {
                if (msg.sender == "user" && msg == recentMessages.last() && imageBase64 != null) {
                    val contentParts = listOf(
                        mapOf("type" to "text", "text" to msg.content),
                        mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$imageBase64"))
                    )
                    apiMessages.add(mapOf("role" to "user", "content" to contentParts))
                } else {
                    val role = if (msg.sender == "user") "user" else "assistant"
                    apiMessages.add(mapOf("role" to role, "content" to msg.content))
                }
            }

            val modelToUse = if (imageBase64 != null && preferences.visionModelName.isNotBlank()) {
                preferences.visionModelName
            } else {
                preferences.modelName
            }

            val rawReply = aiClient.sendMessage(
                baseUrl = preferences.baseUrl,
                apiKey = apiKey,
                modelName = modelToUse,
                messages = apiMessages
            )

            val parsed = ResponseParser.parse(rawReply)
            chatDao.insertMessage(ChatMessage(sender = "assistant", content = parsed.cleanText))

            _uiState.value = ChatUiState.Idle

            onEmotionChanged?.invoke(parsed.emotion)
            parsed.gesture?.let { onGestureTriggered?.invoke(it) }

            if (preferences.speakReplies && parsed.cleanText.isNotBlank()) {
                onSpeakReplyRequested?.invoke(parsed.cleanText)
            }

            if (parsed.actionTag != null) {
                _pendingAction.value = parsed.actionTag
            }

            triggerMemoryExtraction(userText, parsed.cleanText)

        } catch (e: Exception) {
            _uiState.value = ChatUiState.Error(e.localizedMessage ?: "Failed to get reply from AI.", userText)
        }
    }

    fun dismissAction() {
        _pendingAction.value = null
    }

    private fun triggerMemoryExtraction(userMsg: String, assistantReply: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newFacts = aiClient.extractMemories(
                    baseUrl = preferences.baseUrl,
                    apiKey = preferences.apiKey,
                    modelName = preferences.modelName,
                    userMessage = userMsg,
                    assistantReply = assistantReply
                )
                if (newFacts.isNotEmpty()) {
                    val existingMemories = memoryDao.getAllMemories().map { it.fact.lowercase() }.toSet()
                    for (fact in newFacts) {
                        if (fact.lowercase() !in existingMemories) {
                            memoryDao.insertMemory(Memory(fact = fact))
                        }
                    }
                    val count = memoryDao.count()
                    if (count > 200) {
                        memoryDao.deleteOldest(count - 200)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
