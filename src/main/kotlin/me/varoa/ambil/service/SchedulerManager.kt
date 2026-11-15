package me.varoa.ambil.service

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SchedulerManager {
    private val log = LoggerFactory.getLogger(SchedulerManager::class.java)

    private data class ScheduledJob(
        val name: String,
        val dailyTime: String,
        val timezone: String,
        val task: () -> Unit,
    )

    private val executor: ScheduledExecutorService =
        Executors.newScheduledThreadPool(4)

    private val jobs = mutableListOf<ScheduledJob>()

    private val scheduledFutures =
        mutableListOf<ScheduledFuture<*>>()

    /**
     * Prevent overlapping execution of the same job.
     */
    private val runningJobs =
        ConcurrentHashMap<String, AtomicBoolean>()

    @Volatile
    private var started = false

    /**
     * Register a job into the scheduler.
     *
     * This DOES NOT start execution immediately.
     * Jobs only become active after start() is called.
     */
    fun schedule(
        name: String,
        dailyTime: String,
        timezone: String,
        task: () -> Unit,
    ) {
        check(!started) {
            "Cannot register jobs after scheduler has started"
        }

        require(name.isNotBlank()) {
            "Job name cannot be blank"
        }

        require(jobs.none { it.name == name }) {
            "Duplicate job name detected: $name"
        }

        // Validate timezone early
        ZoneId.of(timezone)

        // Validate time early
        LocalTime.parse(dailyTime)

        jobs +=
            ScheduledJob(
                name = name,
                dailyTime = dailyTime,
                timezone = timezone,
                task = task,
            )

        log.info(
            "Registered job '$name' " +
                "at $dailyTime ($timezone)",
        )
    }

    /**
     * Activate all registered jobs.
     */
    fun start() {
        check(!started) {
            "Scheduler already started"
        }

        started = true

        log.info("Starting scheduler")

        jobs.forEach { job ->

            val zoneId = ZoneId.of(job.timezone)

            val localTime = LocalTime.parse(job.dailyTime)

            val initialDelay =
                computeInitialDelay(
                    targetTime = localTime,
                    zoneId = zoneId,
                )

            val period = Duration.ofDays(1).toMillis()

            val future =
                executor.scheduleAtFixedRate(
                    {
                        executeJob(job)
                    },
                    initialDelay,
                    period,
                    TimeUnit.MILLISECONDS,
                )

            scheduledFutures += future

            log.info(
                "Activated '${job.name}' " +
                    "→ ${job.dailyTime} (${job.timezone})",
            )
        }

        log.info(
            "Scheduler started with ${jobs.size} jobs",
        )
    }

    /**
     * Stop all scheduled executions gracefully.
     */
    fun stop() {
        log.info("Stopping scheduler")

        scheduledFutures.forEach {
            it.cancel(true)
        }

        executor.shutdown()

        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn(
                    "Executor did not terminate in time, forcing shutdown",
                )

                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            log.error("Shutdown interrupted", e)

            executor.shutdownNow()

            Thread.currentThread().interrupt()
        }

        log.info("Scheduler stopped")
    }

    /**
     * Execute a single scheduled job safely.
     */
    private fun executeJob(job: ScheduledJob) {
        val lock =
            runningJobs.computeIfAbsent(job.name) {
                AtomicBoolean(false)
            }

        // Prevent overlapping executions
        if (!lock.compareAndSet(false, true)) {
            log.warn(
                "Job '${job.name}' is already running, skipping",
            )

            return
        }

        try {
            log.info(
                "================================================",
            )

            log.info(
                "Starting scheduled job '${job.name}'",
            )

            val start = System.currentTimeMillis()

            job.task()

            val duration =
                System.currentTimeMillis() - start

            log.info(
                "Finished '${job.name}' in ${duration}ms",
            )
        } catch (e: Exception) {
            log.error(
                "Scheduled job '${job.name}' crashed",
                e,
            )
        } finally {
            lock.set(false)

            log.info(
                "================================================",
            )
        }
    }

    /**
     * Compute milliseconds until next execution time.
     */
    private fun computeInitialDelay(
        targetTime: LocalTime,
        zoneId: ZoneId,
    ): Long {
        val now = ZonedDateTime.now(zoneId)

        var nextRun =
            now
                .withHour(targetTime.hour)
                .withMinute(targetTime.minute)
                .withSecond(0)
                .withNano(0)

        if (nextRun.isBefore(now)) {
            nextRun = nextRun.plusDays(1)
        }

        return ChronoUnit.MILLIS.between(now, nextRun)
    }
}
