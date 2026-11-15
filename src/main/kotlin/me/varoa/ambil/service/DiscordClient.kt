package me.varoa.ambil.service

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import me.varoa.ambil.model.config.DiscordConfig
import me.varoa.ambil.model.config.DiscordInteractionConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class DiscordClient(
    private val config: DiscordConfig,
) {
    private val log = LoggerFactory.getLogger(DiscordClient::class.java)
    private val json = "application/json".toMediaType()

    private val http =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
        val body = interactionPayload(interaction, generateNonce()).toString().toRequestBody(json)

        val request =
            Request
                .Builder()
                .url("https://discord.com/api/v9/interactions")
                .post(body)
                .header("Authorization", config.userToken)
                .header("Content-Type", "application/json")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                ).header("X-Super-Properties", buildSuperProperties())
                .header("X-Discord-Locale", "en-US")
                .header("Origin", "https://discord.com")
                .header("Referer", "https://discord.com/channels/${interaction.guildId}/${interaction.channelId}")
                .build()

        return try {
            val response = http.newCall(request).execute()
            response.use {
                when {
                    it.code == 204 -> {
                        log.info("[Discord] Button click successful (204)")
                        Result.success(Unit)
                    }

                    it.code == 200 -> {
                        log.info("[Discord] Button click successful (200)")
                        Result.success(Unit)
                    }

                    it.code == 429 -> {
                        val rb = it.body.string()
                        log.warn("[Discord] Rate limited: $rb")
                        Result.failure(RuntimeException("Rate limited: $rb"))
                    }

                    else -> {
                        val rb = it.body.string()
                        log.error("[Discord] Failed. Code: ${it.code}, Body: $rb")
                        Result.failure(RuntimeException("HTTP ${it.code}: $rb"))
                    }
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
    private fun buildSuperProperties(): String {
        val props =
            buildJsonObject {
                put("os", "Windows")
                put("browser", "Chrome")
                put("device", "")
                put("system_locale", "en-US")
                put(
                    "browser_user_agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                )
                put("browser_version", "124.0.0.0")
                put("os_version", "10")
                put("referrer", "")
                put("referring_domain", "")
                put("release_channel", "stable")
                put("client_build_number", 290510)
                put("client_event_source", JsonNull)
            }
        return java.util.Base64
            .getEncoder()
            .encodeToString(props.toString().toByteArray())
    }
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
