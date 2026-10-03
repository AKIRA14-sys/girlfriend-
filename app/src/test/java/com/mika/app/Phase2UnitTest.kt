package com.mika.app

import com.mika.app.domain.ResponseParser
import org.junit.Assert.assertEquals
import org.junit.Test

class Phase2UnitTest {

    @Test
    fun testEmotionAndGestureExtraction() {
        val input = "[emotion:jealous] [gesture:armsCrossed] Who is she?"
        val parsed = ResponseParser.parse(input)

        assertEquals("jealous", parsed.emotion)
        assertEquals("armsCrossed", parsed.gesture)
        assertEquals("Who is she?", parsed.cleanText)
    }
}
