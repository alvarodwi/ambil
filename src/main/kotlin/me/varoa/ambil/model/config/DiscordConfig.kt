package me.varoa.ambil.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("discord")
data class DiscordConfig(
    // Your user token. DevTools → Application → Local Storage → https://discord.com → token
    // Or: Network tab, any request to discord.com/api, copy the "Authorization" header value.
    val userToken: String,
    val interactions: List<DiscordInteractionConfig>,
) : JobConfig
