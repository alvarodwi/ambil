package me.varoa.ambil.service

import me.varoa.ambil.model.config.DiscordInteractionConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscordClientTest {
    @Test
    fun `payload has the shape the Discord client sends`() {
        val interaction =
            DiscordInteractionConfig(
                applicationId = "4",
                channelId = "2",
                guildId = "1",
                messageId = "3",
                sessionId = "5",
                customId = "dailyCheckInButton",
                name = "not sent",
            )

        assertEquals(
            """{"type":3,"nonce":"99","guild_id":"1","channel_id":"2","message_flags":0,"message_id":"3",""" +
                """"application_id":"4","session_id":"5","data":{"component_type":2,"custom_id":"dailyCheckInButton"}}""",
            interactionPayload(interaction, "99").toString(),
        )
    }
}
