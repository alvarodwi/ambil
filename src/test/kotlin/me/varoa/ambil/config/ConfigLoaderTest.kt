package me.varoa.ambil.config

import me.varoa.ambil.model.config.AppConfig
import me.varoa.ambil.model.config.BlablalinkConfig
import me.varoa.ambil.model.config.DiscordConfig
import me.varoa.ambil.model.config.JobConfig
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigLoaderTest {
    @Test
    fun `decodes minimal config`() {
        val config =
            ConfigLoader.json.decodeFromString<AppConfig>(
                """{"telegram": {"token": "t", "chatId": "c"}, "jobs": []}""",
            )
        assertEquals(true, config.headless)
        assertEquals(emptyList(), config.jobs)
    }

    @Test
    fun `example config decodes`() {
        // Gradle runs tests from the project dir, where the example lives
        val config = ConfigLoader.json.decodeFromString<AppConfig>(File("config.json.example").readText())
        assertEquals(1, config.jobs.count { it.config is DiscordConfig })
        assertEquals(1, config.jobs.count { it.config is BlablalinkConfig })
        assertTrue(config.jobs.all { it.name.isNotBlank() && it.retry.maxAttempts >= 1 })
        val names = config.jobs.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `blablalink config needs no credentials`() {
        assertEquals(BlablalinkConfig, ConfigLoader.json.decodeFromString<JobConfig>("""{"type": "blablalink"}"""))
    }

    @Test
    fun `blablalink config ignores legacy credentials`() {
        val config =
            ConfigLoader.json.decodeFromString<JobConfig>(
                """{"type": "blablalink", "username": "u", "password": "p"}""",
            )
        assertEquals(BlablalinkConfig, config)
    }

    @Test
    fun `discord interaction name is optional`() {
        val ids = """"guild_id": "g", "channel_id": "c", "message_id": "m", "application_id": "a", "session_id": "s", "custom_id": "x""""
        val config =
            ConfigLoader.json.decodeFromString<JobConfig>(
                """{"type": "discord", "userToken": "t", "interactions": [{"name": "nikke daily", $ids}, {$ids}]}""",
            ) as DiscordConfig
        assertEquals(listOf("nikke daily", null), config.interactions.map { it.name })
    }
}
