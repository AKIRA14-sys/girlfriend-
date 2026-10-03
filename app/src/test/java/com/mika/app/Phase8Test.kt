package com.mika.app

import com.mika.app.data.SystemPromptBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase8Test {

    @Test
    fun testCameraVisionRulesInSystemPrompt() {
        val promptWithCamera = SystemPromptBuilder.buildSystemPrompt(
            companionName = "Mika",
            personalityText = "Friendly companion",
            attitudeLevel = 0.5f,
            memories = emptyList(),
            cameraActive = true,
            cameraHasFrame = true
        )
        assertTrue(promptWithCamera.contains("Camera is ON and a frame is provided."))

        val promptWithoutCamera = SystemPromptBuilder.buildSystemPrompt(
            companionName = "Mika",
            personalityText = "Friendly companion",
            attitudeLevel = 0.5f,
            memories = emptyList(),
            cameraActive = false,
            cameraHasFrame = false
        )
        assertTrue(promptWithoutCamera.contains("Camera is OFF or no frame provided."))
    }
}
