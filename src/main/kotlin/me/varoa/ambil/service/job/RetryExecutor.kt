package me.varoa.ambil.service.job

import me.varoa.ambil.model.config.RetryPolicyConfig
import me.varoa.ambil.model.data.JobResult
import me.varoa.ambil.model.data.JobTask
import me.varoa.ambil.model.exception.SessionExpiredException
import org.slf4j.LoggerFactory

class RetryExecutor {
    private val log = LoggerFactory.getLogger(RetryExecutor::class.java)

    fun runWithRetry(
        job: JobTask,
        policy: RetryPolicyConfig,
    ): JobResult {
        var attempt = 1
        var delaySeconds = policy.initialDelaySeconds
        var lastError = "no attempt made"

        while (attempt <= policy.maxAttempts) {
            log.info("[${job.name}] Attempt $attempt")
            try {
                val result = job.execute()
                if (result.success) {
                    log.info("[${job.name}] Success")
                    return result
                }
                log.warn("[${job.name}] Failed: ${result.message}")
                lastError = result.message
            } catch (e: SessionExpiredException) {
                // Only the user can fix this, so retrying would just repeat the failure
                log.warn("[${job.name}] Action needed: ${e.message}")
                return JobResult(false, "Action needed: ${e.message}")
            } catch (e: Exception) {
                log.error("[${job.name}] Exception", e)
                lastError = e.message ?: e::class.simpleName.orEmpty()
            }

            if (attempt == policy.maxAttempts) {
                break
            }

            log.info("[${job.name}] Retrying in ${delaySeconds}s")
            Thread.sleep(delaySeconds * 1000)
            delaySeconds =
                minOf(
                    (delaySeconds * policy.multiplier).toLong(),
                    policy.maxDelaySeconds,
                )

            attempt++
        }

        return JobResult(false, "Failed after ${policy.maxAttempts} attempts: $lastError")
    }
}
