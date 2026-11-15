package me.varoa.ambil.model.config

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleConfig(
    // 24-hour format: "08:00", "23:30", etc.
    val dailyTime: String,
    // Timezone ID, e.g. "Asia/Jakarta", "UTC", "America/New_York"
    val timezone: String = "Asia/Jakarta",
)
