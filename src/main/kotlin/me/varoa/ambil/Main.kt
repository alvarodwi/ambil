package me.varoa.ambil

import me.varoa.ambil.config.ConfigLoader
import me.varoa.ambil.model.config.BlablalinkConfig
import me.varoa.ambil.model.config.DiscordConfig
import me.varoa.ambil.model.config.JobConfig
import me.varoa.ambil.model.config.ScheduledJobConfig
import me.varoa.ambil.model.data.JobTask
import me.varoa.ambil.service.BlablalinkClient
import me.varoa.ambil.service.DiscordClient
import me.varoa.ambil.service.SchedulerManager
import me.varoa.ambil.service.TelegramNotifier
import me.varoa.ambil.service.job.BlablalinkJob
import me.varoa.ambil.service.job.DiscordJob
import me.varoa.ambil.service.job.RetryExecutor
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("me.varoa.ambil.MainKt")

fun main() {
    log.info(" █████╗ ███╗   ███╗██████╗ ██╗██╗")
    log.info("██╔══██╗████╗ ████║██╔══██╗██║██║")
    log.info("███████║██╔████╔██║██████╔╝██║██║")
    log.info("██╔══██║██║╚██╔╝██║██╔══██╗██║██║")
    log.info("██║  ██║██║ ╚═╝ ██║██████╔╝██║███████╗")
    log.info("╚═╝  ╚═╝╚═╝     ╚═╝╚═════╝ ╚═╝╚══════╝")
    log.info("")

    val appConfig = ConfigLoader.load()
    log.info("Configuration loaded")
    log.info("Headless mode: ${appConfig.headless}")
    log.info("Configured jobs: ${appConfig.jobs.size}")

    val telegram = TelegramNotifier(appConfig.telegram)
    val retryExecutor = RetryExecutor()
    val scheduler = SchedulerManager()

    val enabledJobs = appConfig.jobs.filter { it.enabled }
    // Before any browser session, scheduling or startup run
    enabledJobs.forEach { it.retry.requireValid(it.name) }

    // Once, before any job (including runOnStartup) can use the shared browser profile
    if (enabledJobs.any { it.config is BlablalinkConfig }) {
        BlablalinkClient(headless = appConfig.headless).ensureSession()
    }

    enabledJobs.forEach { scheduledJob ->
        val task =
            createTask(
                config = scheduledJob.config,
                headless = appConfig.headless,
            )
        scheduler.schedule(
            name = scheduledJob.name,
            dailyTime = scheduledJob.schedule.dailyTime,
            timezone = scheduledJob.schedule.timezone,
        ) {
            executeJob(
                scheduledJob = scheduledJob,
                task = task,
                retryExecutor = retryExecutor,
                telegram = telegram,
            )
        }

        if (scheduledJob.runOnStartup) {
            log.info(
                "runOnStartup enabled for '${scheduledJob.name}'",
            )
            Thread {
                executeJob(
                    scheduledJob = scheduledJob,
                    task = task,
                    retryExecutor = retryExecutor,
                    telegram = telegram,
                )
            }.start()
        }
    }

    scheduler.start()

    log.info("Scheduler running")
    log.info("Press Ctrl+C to stop")

    Runtime.getRuntime().addShutdownHook(
        Thread {
            log.info("Shutdown signal received")
            try {
                scheduler.stop()
                log.info("Scheduler stopped cleanly")
            } catch (e: Exception) {
                log.error(
                    "Error during shutdown",
                    e,
                )
            }
            log.info("Application terminated")
        },
    )
    // Park the main thread — the scheduler's non-daemon thread keeps the JVM alive
    Thread.currentThread().join()
}

/**
 * Create executable task from polymorphic config.
 */
private fun createTask(
    config: JobConfig,
    headless: Boolean,
): JobTask =
    when (config) {
        is DiscordConfig -> {
            log.info(
                "Creating Discord job",
            )
            val client = DiscordClient(config)
            DiscordJob(client)
        }

        is BlablalinkConfig -> {
            log.info(
                "Creating BlaBlaLink job",
            )
            val client =
                BlablalinkClient(
                    headless = headless,
                )
            BlablalinkJob(client)
        }
    }

private fun executeJob(
    scheduledJob: ScheduledJobConfig,
    task: JobTask,
    retryExecutor: RetryExecutor,
    telegram: TelegramNotifier,
) {
    log.info(
        "Running job '${scheduledJob.name}'",
    )
    try {
        val result =
            retryExecutor.runWithRetry(
                job = task,
                policy = scheduledJob.retry,
            )

        if (result.success) {
            log.info(
                "Job '${scheduledJob.name}' completed successfully",
            )
            telegram.send(
                """
                ✅ Job Success
                
                Job: ${scheduledJob.name}
                Message: ${result.message}
                """.trimIndent(),
            )
        } else {
            log.error(
                "Job '${scheduledJob.name}' failed",
            )
            telegram.send(
                """
                ❌ Job Failed
                
                Job: ${scheduledJob.name}
                Message: ${result.message}
                """.trimIndent(),
            )
        }
    } catch (e: Exception) {
        log.error(
            "Job '${scheduledJob.name}' crashed",
            e,
        )
        telegram.send(
            """
            💥 Job Crashed
            
            Job: ${scheduledJob.name}
            Error: ${e.message}
            """.trimIndent(),
        )
    }
}
