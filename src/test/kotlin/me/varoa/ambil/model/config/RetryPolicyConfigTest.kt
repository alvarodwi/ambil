package me.varoa.ambil.model.config

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class RetryPolicyConfigTest {
    private val valid = RetryPolicyConfig(maxAttempts = 3, initialDelaySeconds = 30, maxDelaySeconds = 300, multiplier = 2.0)

    private fun assertRejected(
        policy: RetryPolicyConfig,
        field: String,
    ) {
        val e = assertFailsWith<IllegalArgumentException> { policy.requireValid("discord") }
        assertContains(e.message.orEmpty(), "Job 'discord'")
        assertContains(e.message.orEmpty(), field)
    }

    @Test
    fun `valid policy passes`() {
        valid.requireValid("discord")
    }

    @Test
    fun `edge values pass`() {
        RetryPolicyConfig(maxAttempts = 1, initialDelaySeconds = 0, maxDelaySeconds = 0, multiplier = 1.0).requireValid("discord")
    }

    @Test
    fun `zero attempts rejected`() = assertRejected(valid.copy(maxAttempts = 0), "retry.maxAttempts")

    @Test
    fun `negative initial delay rejected`() = assertRejected(valid.copy(initialDelaySeconds = -1), "retry.initialDelaySeconds")

    @Test
    fun `negative max delay rejected`() = assertRejected(valid.copy(maxDelaySeconds = -1), "retry.maxDelaySeconds")

    @Test
    fun `multiplier below 1 rejected`() = assertRejected(valid.copy(multiplier = 0.5), "retry.multiplier")
}
