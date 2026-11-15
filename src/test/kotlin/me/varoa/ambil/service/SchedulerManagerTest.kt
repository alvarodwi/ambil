package me.varoa.ambil.service

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
