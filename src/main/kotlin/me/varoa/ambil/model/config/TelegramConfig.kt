package me.varoa.ambil.model.config

import kotlinx.serialization.Serializable

@Serializable
data class TelegramConfig(
    val token: String,
    val chatId: String,
)
