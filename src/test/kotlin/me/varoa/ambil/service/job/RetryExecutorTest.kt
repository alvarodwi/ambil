package me.varoa.ambil.service.job

import me.varoa.ambil.model.config.RetryPolicyConfig
import me.varoa.ambil.model.data.JobResult
import me.varoa.ambil.model.data.JobTask
import me.varoa.ambil.model.exception.SessionExpiredException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetryExecutorTest {
    private fun policy(maxAttempts: Int) = RetryPolicyConfig(maxAttempts, 0, 0, 1.0)

    private class FakeJob(
        private val attempt: (Int) -> JobResult,
    ) : JobTask {
        override val name = "fake"
        var calls = 0

        override fun execute(): JobResult = attempt(++calls)
    }

    @Test
    fun `all attempts fail reports count and last error`() {
        val job = FakeJob { JobResult(false, "boom") }
        val result = RetryExecutor().runWithRetry(job, policy(3))
        assertEquals(3, job.calls)
        assertFalse(result.success)
        assertTrue("3 attempts" in result.message, result.message)
        assertTrue("boom" in result.message, result.message)
    }

    @Test
    fun `exception message is kept as last error`() {
        val job = FakeJob { throw IllegalStateException("kaboom") }
        val result = RetryExecutor().runWithRetry(job, policy(2))
        assertEquals(2, job.calls)
        assertTrue("kaboom" in result.message, result.message)
    }

    @Test
    fun `expired session is not retried`() {
        val job = FakeJob { throw SessionExpiredException() }
        val result = RetryExecutor().runWithRetry(job, policy(5))
        assertEquals(1, job.calls)
        assertFalse(result.success)
        assertTrue(result.message.startsWith("Action needed"), result.message)
    }

    @Test
    fun `succeeds on retry`() {
        val job = FakeJob { if (it == 1) JobResult(false, "first") else JobResult(true, "ok") }
        val result = RetryExecutor().runWithRetry(job, policy(3))
        assertEquals(2, job.calls)
        assertTrue(result.success)
        assertEquals("ok", result.message)
    }
}
