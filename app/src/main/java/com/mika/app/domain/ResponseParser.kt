package com.mika.app.domain

data class ParsedResponse(
    val emotion: String = "neutral",
    val gesture: String? = null,
    val cleanText: String = "",
    val actionTag: ActionTag? = null
)

data class ActionTag(
    val type: String,
    val rawArgs: String
)

object ResponseParser {

    fun parse(raw: String): ParsedResponse {
        var text = raw.trim()
        var emotion = "neutral"
        var gesture: String? = null
        var actionTag: ActionTag? = null

        val actionRegex = Regex("""\[\[ACTION:\s*([a-zA-Z0-9_]+)(?:\s*\|\s*(.*?))?\]\]""", RegexOption.DOT_MATCHES_ALL)
        val actionMatch = actionRegex.find(text)
        if (actionMatch != null) {
            val type = actionMatch.groupValues[1].trim().lowercase()
            val args = actionMatch.groupValues[2].trim()
            actionTag = ActionTag(type, args)
            text = text.replace(actionMatch.value, "").trim()
        }

        val emotionRegex = Regex("""\[emotion:\s*([a-zA-Z]+)\]""", RegexOption.IGNORE_CASE)
        val emotionMatch = emotionRegex.find(text)
        if (emotionMatch != null) {
            val em = emotionMatch.groupValues[1].lowercase()
            if (em in listOf("neutral", "happy", "teasing", "jealous", "sleepy", "caring")) {
                emotion = em
            }
            text = text.replace(emotionMatch.value, "").trim()
        }

        val gestureRegex = Regex("""\[gesture:\s*([a-zA-Z]+)\]""", RegexOption.IGNORE_CASE)
        val gestureMatch = gestureRegex.find(text)
        if (gestureMatch != null) {
            val g = gestureMatch.groupValues[1]
            val validGestures = listOf("wave", "nod", "headTilt", "shrug", "armsCrossed", "handsOnHips")
            val matchedKey = validGestures.firstOrNull { it.equals(g, ignoreCase = true) }
            if (matchedKey != null) {
                gesture = matchedKey
            }
            text = text.replace(gestureMatch.value, "").trim()
        }

        return ParsedResponse(
            emotion = emotion,
            gesture = gesture,
            cleanText = text,
            actionTag = actionTag
        )
    }
}
