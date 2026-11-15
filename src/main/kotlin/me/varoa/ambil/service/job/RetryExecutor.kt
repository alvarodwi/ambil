package me.varoa.ambil.service.job

import me.varoa.ambil.model.config.RetryPolicyConfig
import me.varoa.ambil.model.data.JobResult
import me.varoa.ambil.model.data.JobTask
import me.varoa.ambil.service.TelegramNotifier
import org.slf4j.LoggerFactory

class RetryExecutor(
    private val telegram: TelegramNotifier,
) {
    private val log = LoggerFactory.getLogger(RetryExecutor::class.java)

    fun runWithRetry(
        job: JobTask,
        policy: RetryPolicyConfig,
    ): JobResult {
        var attempt = 1
        var delaySeconds = policy.initialDelaySeconds

        while (attempt <= policy.maxAttempts) {
            log.info("[${job.name}] Attempt $attempt")
            try {
                val result = job.execute()
                if (result.success) {
                    log.info("[${job.name}] Success")
                    return result
                }
                log.warn("[${job.name}] Failed: ${result.message}")
            } catch (e: Exception) {
                log.error("[${job.name}] Exception", e)
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

        val msg = "[${job.name}] Failed after ${policy.maxAttempts} attempts"
        telegram.send(msg)
        return JobResult(false, msg)
    }
}
