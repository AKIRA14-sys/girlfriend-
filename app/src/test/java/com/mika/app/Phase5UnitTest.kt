package com.mika.app

import com.mika.app.domain.ResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class Phase5UnitTest {

    @Test
    fun testActionTagParsing() {
        val raw = "I'll open Spotify for you! [[ACTION: open_app | Spotify]]"
        val parsed = ResponseParser.parse(raw)

        assertNotNull(parsed.actionTag)
        assertEquals("open_app", parsed.actionTag?.type)
        assertEquals("Spotify", parsed.actionTag?.rawArgs)
        assertEquals("I'll open Spotify for you!", parsed.cleanText)
    }

    @Test
    fun testCallActionTagParsing() {
        val raw = "Calling Rahul now... [[ACTION: call | Rahul]]"
        val parsed = ResponseParser.parse(raw)

        assertNotNull(parsed.actionTag)
        assertEquals("call", parsed.actionTag?.type)
        assertEquals("Rahul", parsed.actionTag?.rawArgs)
    }
}
