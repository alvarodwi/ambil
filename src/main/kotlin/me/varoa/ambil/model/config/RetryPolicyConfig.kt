package me.varoa.ambil.model.config

import kotlinx.serialization.Serializable

@Serializable
data class RetryPolicyConfig(
    val maxAttempts: Int,
    val initialDelaySeconds: Long,
    val maxDelaySeconds: Long,
    val multiplier: Double,
)
