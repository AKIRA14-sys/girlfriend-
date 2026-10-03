package com.mika.app

import com.mika.app.data.SystemPromptBuilder
import com.mika.app.data.db.Memory
import com.mika.app.domain.ResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1UnitTest {

    @Test
    fun testResponseParserWithEmotionAndGestureAndAction() {
        val raw = "[emotion:happy] [gesture:wave] Hello there darling! [[ACTION: open_app | Spotify]]"
        val parsed = ResponseParser.parse(raw)

        assertEquals("happy", parsed.emotion)
        assertEquals("wave", parsed.gesture)
        assertEquals("Hello there darling!", parsed.cleanText)
        assertNotNull(parsed.actionTag)
        assertEquals("open_app", parsed.actionTag?.type)
        assertEquals("Spotify", parsed.actionTag?.rawArgs)
    }

    @Test
    fun testSystemPromptBuilderContainsCompanionDetails() {
        val prompt = SystemPromptBuilder.buildSystemPrompt(
            companionName = "Mika",
            personalityText = "Gamer girl personality",
            attitudeLevel = 0.8f,
            memories = listOf(Memory(fact = "User likes RPGs"))
        )

        assertTrue(prompt.contains("Mika"))
        assertTrue(prompt.contains("Gamer girl personality"))
        assertTrue(prompt.contains("User likes RPGs"))
        assertTrue(prompt.contains("sassy", ignoreCase = true))
    }
}
