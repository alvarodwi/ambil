package me.varoa.ambil.service.job

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordJobTest {
    private val ok = Result.success(Unit)
    private val unauthorized = Result.failure<Unit>(RuntimeException("HTTP 401: Unauthorized"))

    @Test
    fun `all succeed lists every name`() {
        val result = discordJobResult(listOf("nikke daily" to ok, "reverse1999 daily" to ok))
        assertTrue(result.success)
        assertEquals("Claimed: nikke daily, reverse1999 daily", result.message)
    }

    @Test
    fun `one failure names it with its error and lists the claimed one`() {
        val result = discordJobResult(listOf("nikke daily" to ok, "reverse1999 daily" to unauthorized))
        assertFalse(result.success)
        assertEquals("Failed: reverse1999 daily: HTTP 401: Unauthorized\nClaimed: nikke daily", result.message)
    }

    @Test
    fun `all fail has no claimed line`() {
        val result = discordJobResult(listOf("interaction 1" to unauthorized, "interaction 2" to unauthorized))
        assertFalse(result.success)
        assertEquals(
            "Failed: interaction 1: HTTP 401: Unauthorized\nFailed: interaction 2: HTTP 401: Unauthorized",
            result.message,
        )
    }
}
