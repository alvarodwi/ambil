package me.varoa.ambil.model.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val telegram: TelegramConfig,
    // Set to false to skip headless mode (show browser window)
    val headless: Boolean = true,
    val jobs: List<ScheduledJobConfig>,
)
