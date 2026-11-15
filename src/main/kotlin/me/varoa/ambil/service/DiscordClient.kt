package me.varoa.ambil.service

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import me.varoa.ambil.model.config.DiscordConfig
import me.varoa.ambil.model.config.DiscordInteractionConfig
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class DiscordClient(
    private val config: DiscordConfig,
) {
    private val log = LoggerFactory.getLogger(DiscordClient::class.java)
    private val http =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build()

    /**
     * Iterates all configured interactions and clicks each button.
     * Returns (label, result) pairs in the same order as config.interactions.
     * The label is the interaction's name, or its 1-based position when unnamed.
     */
    fun clickAllButtons(): List<Pair<String, Result<Unit>>> =
        config.interactions.mapIndexed { index, interaction ->
            log.info("[Discord] Clicking button ${index + 1}/${config.interactions.size} (customId=${interaction.customId})")
            val result = clickButton(interaction)
            // Small delay between interactions to avoid triggering rate limits
            if (index < config.interactions.lastIndex) Thread.sleep(1500)
            (interaction.name?.takeIf { it.isNotBlank() } ?: "interaction ${index + 1}") to result
        }

    /**
     * Posts a single button-click interaction to Discord's API.
     * Mirrors exactly what the browser sends when you manually click the reward button.
     */
    private fun clickButton(interaction: DiscordInteractionConfig): Result<Unit> {
        val body = interactionPayload(interaction, generateNonce()).toString()

        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("https://discord.com/api/v9/interactions"))
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", config.userToken)
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .header("X-Super-Properties", superProperties())
                .header("X-Discord-Locale", LOCALE)
                .header("Origin", "https://discord.com")
                .header("Referer", "https://discord.com/channels/${interaction.guildId}/${interaction.channelId}")
                .build()

        return try {
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            val code = response.statusCode()
            when (code) {
                200, 204 -> {
                    log.info("[Discord] Button click successful ($code)")
                    Result.success(Unit)
                }

                429 -> {
                    log.warn("[Discord] Rate limited: ${response.body()}")
                    Result.failure(RuntimeException("Rate limited: ${response.body()}"))
                }

                else -> {
                    log.error("[Discord] Failed. Code: $code, Body: ${response.body()}")
                    Result.failure(RuntimeException("HTTP $code: ${response.body()}"))
                }
            }
        } catch (e: Exception) {
            log.error("[Discord] HTTP call failed", e)
            Result.failure(e)
        }
    }

    private fun generateNonce(): String {
        val epoch = System.currentTimeMillis() - 1420070400000L
        return (epoch shl 22).toString()
    }

    /**
     * X-Super-Properties is a base64-encoded JSON blob Discord attaches to every request.
     * Sniff your own from DevTools for best accuracy (any request → Headers → X-Super-Properties).
     */
    private fun superProperties(): String =
        java.util.Base64
            .getEncoder()
            .encodeToString(superPropertiesJson().toString().toByteArray())
}

// One browser identity, used by every header, so they always agree.
// Taken from a Discord web client request captured in Firefox 149 on Windows.
// shortcut: hardcoded, goes stale as Discord ships new builds; re-capture if Discord starts rejecting clicks.
internal const val BROWSER_VERSION = "149.0"
internal const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:$BROWSER_VERSION) Gecko/20100101 Firefox/$BROWSER_VERSION"
internal const val LOCALE = "en-US"
internal const val CLIENT_BUILD_NUMBER = 520959

internal fun superPropertiesJson(): JsonObject =
    buildJsonObject {
        put("os", "Windows")
        put("browser", "Firefox")
        put("device", "")
        put("system_locale", LOCALE)
        put("has_client_mods", false)
        put("browser_user_agent", USER_AGENT)
        put("browser_version", BROWSER_VERSION)
        put("os_version", "10")
        put("referrer", "")
        put("referring_domain", "")
        put("release_channel", "stable")
        put("client_build_number", CLIENT_BUILD_NUMBER)
        put("client_event_source", JsonNull)
    }

/**
 * The interaction body, in the exact shape Discord's client sends.
 * All field names must match — Discord validates the shape server-side.
 */
internal fun interactionPayload(
    interaction: DiscordInteractionConfig,
    nonce: String,
): JsonObject =
    buildJsonObject {
        put("type", 3)
        put("nonce", nonce)
        put("guild_id", interaction.guildId)
        put("channel_id", interaction.channelId)
        put("message_flags", 0)
        put("message_id", interaction.messageId)
        put("application_id", interaction.applicationId)
        put("session_id", interaction.sessionId)
        putJsonObject("data") {
            put("component_type", interaction.componentType)
            put("custom_id", interaction.customId)
        }
    }
