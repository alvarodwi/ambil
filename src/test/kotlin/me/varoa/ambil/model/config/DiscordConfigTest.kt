package me.varoa.ambil.model.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DiscordConfigTest {
    private val interaction =
        DiscordInteractionConfig(
            guildId = "1",
            channelId = "2",
            messageId = "3",
            applicationId = "4",
            sessionId = "5",
            customId = "btn",
        )

    @Test
    fun `empty interactions rejected`() {
        val e = assertFailsWith<IllegalArgumentException> { DiscordConfig("t", emptyList()).requireValid("discord") }
        assertEquals("Job 'discord': interactions must not be empty", e.message)
    }

    @Test
    fun `one interaction passes`() {
        DiscordConfig("t", listOf(interaction)).requireValid("discord")
    }
}
