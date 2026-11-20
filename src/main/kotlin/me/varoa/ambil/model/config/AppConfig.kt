package me.varoa.ambil.model.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val telegram: TelegramConfig,
    // Set to false to skip headless mode (show browser window)
    val headless: Boolean = true,
    // When set, the logged-in browser's game_* cookies are written here (e.g. for shiniri-cli)
    val cookieExportPath: String? = null,
    val jobs: List<ScheduledJobConfig>,
)
