package me.varoa.ambil.service

import java.nio.file.Files
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CookieExportTest {
    @Test
    fun `keeps only game cookies in order`() {
        val cookies = listOf("game_token" to "t", "OptanonConsent" to "c", "game_uid" to "1", "game_openid" to "o")
        assertEquals("game_token=t; game_uid=1; game_openid=o", cookieHeader(cookies))
    }

    @Test
    fun `null without game_openid`() {
        assertNull(cookieHeader(listOf("game_token" to "t", "game_uid" to "1")))
    }

    @Test
    fun `null with blank game_token`() {
        assertNull(cookieHeader(listOf("game_token" to " ", "game_openid" to "o")))
    }

    @Test
    fun `writes and replaces the file atomically`() {
        val dir = Files.createTempDirectory("ambil-cookie").resolve("missing/config")
        val file = dir.resolve("cookie.txt")

        writeCookieFile(file, "game_token=a; game_openid=o")
        assertEquals("game_token=a; game_openid=o", file.readText())

        writeCookieFile(file, "game_token=b; game_openid=o")
        assertEquals("game_token=b; game_openid=o", file.readText())
        assertEquals(listOf(file), dir.listDirectoryEntries())

        if (Files.getFileStore(file).supportsFileAttributeView(PosixFileAttributeView::class.java)) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)))
        }
    }
}
