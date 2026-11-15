package me.varoa.ambil.service

import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchedulerManagerTest {
    @Test
    fun `runNow is skipped while the same job is still running`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val runs = AtomicInteger()
        val scheduler = SchedulerManager()
        scheduler.schedule("job", "08:00", "Asia/Jakarta") {
            runs.incrementAndGet()
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
        }

        scheduler.runNow("job")
        assertTrue(started.await(5, TimeUnit.SECONDS))
        scheduler.runNow("job")
        Thread.sleep(200) // give the second run time to be skipped
        release.countDown()

        assertEquals(1, runs.get())
        scheduler.stop()
    }
}

class NextRunTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `time later today`() {
        val after = ZonedDateTime.of(2026, 11, 15, 7, 0, 0, 0, jakarta)
        assertEquals(ZonedDateTime.of(2026, 11, 15, 8, 0, 0, 0, jakarta), SchedulerManager.nextRun(LocalTime.of(8, 0), jakarta, after))
    }

    @Test
    fun `time already passed or exactly now goes to tomorrow`() {
        val passed = ZonedDateTime.of(2026, 11, 15, 9, 0, 0, 0, jakarta)
        val exact = ZonedDateTime.of(2026, 11, 15, 8, 0, 0, 0, jakarta)
        val tomorrow = ZonedDateTime.of(2026, 11, 16, 8, 0, 0, 0, jakarta)
        assertEquals(tomorrow, SchedulerManager.nextRun(LocalTime.of(8, 0), jakarta, passed))
        assertEquals(tomorrow, SchedulerManager.nextRun(LocalTime.of(8, 0), jakarta, exact))
    }

    @Test
    fun `daylight saving change keeps local time`() {
        // Clocks move forward on 2026-03-08 in New York, so this day is 23 hours long
        val after = ZonedDateTime.of(2026, 3, 7, 8, 0, 0, 0, newYork)
        val next = SchedulerManager.nextRun(LocalTime.of(8, 0), newYork, after)
        assertEquals(LocalTime.of(8, 0), next.toLocalTime())
        assertEquals(Duration.ofHours(23), Duration.between(after, next))
    }
}
