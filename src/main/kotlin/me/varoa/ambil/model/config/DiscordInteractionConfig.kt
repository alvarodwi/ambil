package me.varoa.ambil.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscordInteractionConfig(
    // Sniff these from DevTools (Network tab) when you manually click the reward button.
    // Look for a POST to /api/v9/interactions and copy the request body fields below.
    @SerialName("application_id")
    val applicationId: String, // "application_id" in the payload
    @SerialName("channel_id")
    val channelId: String, // "channel_id"
    @SerialName("guild_id")
    val guildId: String, // "guild_id" (empty string "" if DM)
    @SerialName("message_id")
    val messageId: String, // "message_id"
    @SerialName("session_id")
    val sessionId: String, // "session_id" — changes per browser session, update periodically
    @SerialName("custom_id")
    val customId: String, // data.custom_id of the button
    @SerialName("component_type")
    val componentType: Int = 2, // 2 = Button, 3 = Select menu
    val name: String? = null, // label in Telegram messages, e.g. "nikke daily"; not sent to Discord
)
