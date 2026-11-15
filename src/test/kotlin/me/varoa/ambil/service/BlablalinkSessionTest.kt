package me.varoa.ambil.service

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlablalinkSessionTest {
    private val log = LoggerFactory.getLogger(BlablalinkSessionTest::class.java)

    @Test
    fun `logged in skips login`() {
        var logins = 0
        ensureSession(isLoggedIn = { true }, login = { logins++ }, log = log)
        assertEquals(0, logins)
    }

    @Test
    fun `logged out logs in once`() {
        var logins = 0
        ensureSession(isLoggedIn = { false }, login = { logins++ }, log = log)
        assertEquals(1, logins)
    }

    @Test
    fun `failed login does not throw`() {
        ensureSession(isLoggedIn = { false }, login = { error("timed out") }, log = log)
    }

    @Test
    fun `failed session check does not throw`() {
        ensureSession(isLoggedIn = { error("chrome failed") }, login = {}, log = log)
    }

    @Test
    fun `rendered page with log in chip is logged out`() {
        assertFalse(sessionState(listRendered = true, logInVisible = true))
    }

    @Test
    fun `rendered page without log in chip is logged in`() {
        assertTrue(sessionState(listRendered = true, logInVisible = false))
    }

    @Test
    fun `unrendered page is an error, not logged in`() {
        assertFailsWith<IllegalStateException> { sessionState(listRendered = false, logInVisible = false) }
    }

    @Test
    fun `claimed sign-in icon`() {
        val cls = "w-[24px] h-[24px] bg-[length:100%_100%] mr-[19px] bg-[url('@/assets/imgs/common/icon-gift-true.png')]"
        assertEquals(SignInState.CLAIMED, signInState(cls))
    }

    @Test
    fun `unclaimed sign-in icon`() {
        val cls = "w-[24px] h-[24px] bg-[length:100%_100%] mr-[19px] bg-[url('@/assets/imgs/common/icon-gift.png')]"
        assertEquals(SignInState.UNCLAIMED, signInState(cls))
    }

    @Test
    fun `missing or unrelated sign-in icon is unknown`() {
        assertEquals(SignInState.UNKNOWN, signInState(null))
        assertEquals(SignInState.UNKNOWN, signInState("w-[24px] bg-[url('@/assets/imgs/common/icon-star.png')]"))
    }

    @Test
    fun `chrome arguments keep security protections`() {
        val profile = Path.of("/tmp/profile")
        val args = chromeArguments(headless = true, profileDir = profile)
        assertFalse("--no-sandbox" in args)
        assertFalse(args.any { it.startsWith("--remote-allow-origins") })
        assertTrue("--headless=new" in args)
        assertTrue("--user-data-dir=/tmp/profile" in args)
    }

    @Test
    fun `visible browser has no headless flag`() {
        assertFalse("--headless=new" in chromeArguments(headless = false, profileDir = Path.of("/tmp/p")))
    }

    @Test
    fun `profile dir is owner only`() {
        val dir = Files.createTempDirectory("ambil-test").resolve("profile")
        prepareProfileDir(dir)
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)))

        // existing dir with loose permissions gets tightened
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxr-xr-x"))
        prepareProfileDir(dir)
        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(dir)))
    }
}
