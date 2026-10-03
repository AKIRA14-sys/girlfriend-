package com.mika.app.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Folder
import javax.mail.Session
import javax.mail.Store

data class EmailHeader(
    val sender: String,
    val subject: String,
    val date: String,
    val snippet: String
)

object ImapEmailReader {

    suspend fun fetchRecentEmails(
        email: String,
        password: String,
        host: String = "imap.gmail.com",
        port: Int = 993,
        limit: Int = 10
    ): List<EmailHeader> = withContext(Dispatchers.IO) {
        if (email.isBlank() || password.isBlank()) return@withContext emptyList()

        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", host)
            put("mail.imaps.port", port.toString())
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.timeout", "10000")
            put("mail.imaps.connectiontimeout", "10000")
        }

        var store: Store? = null
        var inbox: Folder? = null

        return@withContext try {
            val session = Session.getInstance(props, null)
            store = session.getStore("imaps")
            store.connect(host, email, password)

            inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)

            val messageCount = inbox.messageCount
            val startIndex = Math.max(1, messageCount - limit + 1)
            val messages = inbox.getMessages(startIndex, messageCount)

            val results = mutableListOf<EmailHeader>()
            for (msg in messages.reversed()) {
                val senderStr = msg.from?.firstOrNull()?.toString() ?: "Unknown"
                val subjectStr = msg.subject ?: "(No Subject)"
                val dateStr = msg.sentDate?.toString() ?: ""
                val snippetStr = try {
                    msg.content?.toString()?.take(100) ?: ""
                } catch (_: Exception) {
                    ""
                }
                results.add(EmailHeader(sender = senderStr, subject = subjectStr, date = dateStr, snippet = snippetStr))
            }
            results
        } catch (e: Exception) {
            emptyList()
        } finally {
            try { inbox?.close(false) } catch (_: Exception) {}
            try { store?.close() } catch (_: Exception) {}
        }
    }
}
