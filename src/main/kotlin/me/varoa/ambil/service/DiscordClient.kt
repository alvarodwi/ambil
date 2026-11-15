package me.varoa.ambil.service

import com.google.gson.Gson
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
    private val gson = Gson()
    private val json = "application/json".toMediaType()

    private val http =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    /**
     * Iterates all configured interactions and clicks each button.
     * Returns a list of results in the same order as config.interactions.
     */
    fun clickAllButtons(): List<Result<Unit>> =
        config.interactions.mapIndexed { index, interaction ->
            log.info("[Discord] Clicking button ${index + 1}/${config.interactions.size} (customId=${interaction.customId})")
            val result = clickButton(interaction)
            // Small delay between interactions to avoid triggering rate limits
            if (index < config.interactions.lastIndex) Thread.sleep(1500)
            result
        }

    /**
     * Posts a single button-click interaction to Discord's API.
     * Mirrors exactly what the browser sends when you manually click the reward button.
     */
    private fun clickButton(interaction: DiscordInteractionConfig): Result<Unit> {
        val nonce = generateNonce()

        // This payload mirrors the exact structure Discord's client sends.
        // All field names must match — Discord validates the shape server-side.
        val payload =
            mapOf(
                "type" to 3,
                "nonce" to nonce,
                "guild_id" to interaction.guildId,
                "channel_id" to interaction.channelId,
                "message_flags" to 0,
                "message_id" to interaction.messageId,
                "application_id" to interaction.applicationId,
                "session_id" to interaction.sessionId,
                "data" to
                    mapOf(
                        "component_type" to interaction.componentType,
                        "custom_id" to interaction.customId,
                    ),
            )

        val body = gson.toJson(payload).toRequestBody(json)

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
            mapOf(
                "os" to "Windows",
                "browser" to "Chrome",
                "device" to "",
                "system_locale" to "en-US",
                "browser_user_agent" to
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "browser_version" to "124.0.0.0",
                "os_version" to "10",
                "referrer" to "",
                "referring_domain" to "",
                "release_channel" to "stable",
                "client_build_number" to 290510,
                "client_event_source" to null,
            )
        return java.util.Base64
            .getEncoder()
            .encodeToString(gson.toJson(props).toByteArray())
    }
}
