package com.mika.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mika.app.data.AppPreferences
import com.mika.app.data.ai.AiClient
import com.mika.app.data.ai.OpenAiClient
import com.mika.app.data.db.AppDatabase
import com.mika.app.data.db.Memory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    val preferences: AppPreferences,
    private val aiClient: AiClient = OpenAiClient()
) : ViewModel() {

    private val _testConnectionState = MutableStateFlow<String?>(null)
    val testConnectionState: StateFlow<String?> = _testConnectionState.asStateFlow()

    fun testConnection() {
        val apiKey = preferences.apiKey
        if (apiKey.isBlank()) {
            _testConnectionState.value = "Error: API key is empty."
            return
        }

        viewModelScope.launch {
            _testConnectionState.value = "Testing..."
            try {
                val testMessages = listOf(
                    mapOf("role" to "user", "content" to "Hi, answer 'OK' if you see this.")
                )
                val reply = aiClient.sendMessage(
                    baseUrl = preferences.baseUrl,
                    apiKey = apiKey,
                    modelName = preferences.modelName,
                    messages = testMessages
                )
                _testConnectionState.value = "Success! Received: \"$reply\""
            } catch (e: Exception) {
                _testConnectionState.value = "Connection failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearTestConnectionState() {
        _testConnectionState.value = null
    }
}

class MemoriesViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val memoryDao = database.memoryDao()
    val memories = memoryDao.getAllMemoriesFlow()

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryDao.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryDao.clearAll()
        }
    }
}

class DataViewModel(
    private val database: AppDatabase
) : ViewModel() {

    fun clearChatHistory() {
        viewModelScope.launch {
            database.chatMessageDao().clearAll()
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            database.chatMessageDao().clearAll()
            database.memoryDao().clearAll()
        }
    }

    suspend fun getExportBackupJson(): String {
        val memories = database.memoryDao().getAllMemories()
        val messages = database.chatMessageDao().getLastMessages(1000)
        val map = mapOf(
            "memories" to memories.map { it.fact },
            "chatHistory" to messages.map { mapOf("sender" to it.sender, "content" to it.content, "timestamp" to it.timestamp) }
        )
        return com.google.gson.Gson().toJson(map)
    }

    suspend fun importBackupJson(json: String) {
        try {
            val obj = com.google.gson.Gson().fromJson(json, com.google.gson.JsonObject::class.java)
            if (obj.has("memories")) {
                val array = obj.getAsJsonArray("memories")
                for (elem in array) {
                    val fact = elem.asString
                    database.memoryDao().insertMemory(Memory(fact = fact))
                }
            }
        } catch (_: Exception) {
        }
    }
}
