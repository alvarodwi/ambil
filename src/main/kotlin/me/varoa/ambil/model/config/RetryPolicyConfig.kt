package me.varoa.ambil.model.config

import kotlinx.serialization.Serializable

@Serializable
data class RetryPolicyConfig(
    val maxAttempts: Int,
    val initialDelaySeconds: Long,
    val maxDelaySeconds: Long,
    val multiplier: Double,
) {
    /** Throws [IllegalArgumentException] naming [jobName] and the bad field. */
    fun requireValid(jobName: String) {
        require(maxAttempts >= 1) { "Job '$jobName': retry.maxAttempts must be at least 1, was $maxAttempts" }
        require(initialDelaySeconds >= 0) {
            "Job '$jobName': retry.initialDelaySeconds must not be negative, was $initialDelaySeconds"
        }
        require(maxDelaySeconds >= 0) { "Job '$jobName': retry.maxDelaySeconds must not be negative, was $maxDelaySeconds" }
        require(multiplier >= 1) { "Job '$jobName': retry.multiplier must be at least 1, was $multiplier" }
    }
}
