package me.varoa.ambil.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.varoa.ambil.model.config.TelegramConfig
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class TelegramNotifier(
    private val config: TelegramConfig,
) {
    private val log = LoggerFactory.getLogger(TelegramNotifier::class.java)
    private val http = HttpClient.newHttpClient()
    private val json =
        Json {
            encodeDefaults = true
        }

    private val apiUrl =
        "https://api.telegram.org/bot${config.token}/sendMessage"

    private val markdownRegex =
        Regex("""([_*\[\]()~`>#+\-=|{}.!])""")

    fun send(message: String) {
        val payload =
            TelegramPayload(
                chat_id = config.chatId,
                text = escapeMarkdown(message),
                parse_mode = "MarkdownV2",
            )

        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        json.encodeToString(payload),
                    ),
                ).build()

        try {
            val response =
                http.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                log.warn("[Telegram] Non-200 response: ${response.statusCode()} — ${response.body()}")
            } else {
                log.info("[Telegram] Notification sent")
            }
        } catch (e: Exception) {
            log.error("[Telegram] Failed to send notification", e)
        }
    }

    private fun escapeMarkdown(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace(markdownRegex, "\\\\$1")

    @Serializable
    data class TelegramPayload(
        val chat_id: String,
        val text: String,
        val parse_mode: String,
    )
}
