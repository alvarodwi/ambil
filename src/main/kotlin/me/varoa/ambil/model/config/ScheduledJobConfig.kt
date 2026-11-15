package me.varoa.ambil.model.config

import kotlinx.serialization.Serializable

@Serializable
data class ScheduledJobConfig(
    val name: String,
    val enabled: Boolean = true,
    val runOnStartup: Boolean = false,
    val schedule: ScheduleConfig,
    val retry: RetryPolicyConfig,
    val config: JobConfig,
)
