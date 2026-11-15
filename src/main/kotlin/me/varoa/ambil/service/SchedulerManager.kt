package me.varoa.ambil.service

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

    // The pending run of each job
    private val scheduledFutures =
        ConcurrentHashMap<String, ScheduledFuture<*>>()

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

            scheduleAt(
                job = job,
                runAt = nextRun(LocalTime.parse(job.dailyTime), zoneId, ZonedDateTime.now(zoneId)),
            )

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
     * One-shot run at [runAt]; when it ends, the next run is computed from the wall clock again,
     * so it stays on dailyTime across daylight saving changes.
     */
    private fun scheduleAt(
        job: ScheduledJob,
        runAt: ZonedDateTime,
    ) {
        if (executor.isShutdown) return
        val delay = Duration.between(ZonedDateTime.now(runAt.zone), runAt).toMillis().coerceAtLeast(0)
        scheduledFutures[job.name] =
            executor.schedule(
                {
                    try {
                        executeJob(job)
                    } finally {
                        val now = ZonedDateTime.now(runAt.zone)
                        // Never before runAt, so a timer firing early cannot run the job twice that day
                        val after = if (now.isAfter(runAt)) now else runAt
                        scheduleAt(job, nextRun(LocalTime.parse(job.dailyTime), runAt.zone, after))
                    }
                },
                delay,
                TimeUnit.MILLISECONDS,
            )
    }

    /**
     * Run a registered job once now, on its own thread, skipped if that job is already running.
     */
    fun runNow(name: String) {
        val job = jobs.first { it.name == name }
        Thread { executeJob(job) }.start()
    }

    /**
     * Stop all scheduled executions gracefully.
     */
    fun stop() {
        log.info("Stopping scheduler")

        // Shut down first, so a run ending now schedules no next run
        executor.shutdown()

        scheduledFutures.values.forEach {
            it.cancel(true)
        }

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

    companion object {
        /** First [dailyTime] in [zone] strictly after [after]; a time inside a DST gap moves forward. */
        fun nextRun(
            dailyTime: LocalTime,
            zone: ZoneId,
            after: ZonedDateTime,
        ): ZonedDateTime {
            val date = after.withZoneSameInstant(zone).toLocalDate()
            val today = date.atTime(dailyTime).atZone(zone)
            return if (today.isAfter(after)) today else date.plusDays(1).atTime(dailyTime).atZone(zone)
        }
    }
}
