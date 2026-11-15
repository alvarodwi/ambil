package me.varoa.ambil.service

import io.github.bonigarcia.wdm.WebDriverManager
import me.varoa.ambil.model.data.BlablalinkResult
import me.varoa.ambil.model.exception.SessionExpiredException
import org.openqa.selenium.By
import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration

class BlablalinkClient(
    private val headless: Boolean = true,
) {
    private val log = LoggerFactory.getLogger(BlablalinkClient::class.java)
    private lateinit var driver: WebDriver
    private val js get() = driver as JavascriptExecutor

    fun run(): BlablalinkResult {
        driver = buildDriver(headless)

        return try {
            ensureLoggedIn()

            val signInMessage = loginAndClaimDailyReward()
            val likesMessage = browseAndLike()

            BlablalinkResult(
                signIn = signInMessage,
                likes = likesMessage,
                success = true,
            )
        } catch (e: SessionExpiredException) {
            // Let RetryExecutor see it, so it stops instead of retrying
            throw e
        } catch (e: Exception) {
            log.error("Automation failed", e)
            BlablalinkResult(
                signIn = "Failed: ${e.message}",
                likes = "-",
                success = false,
            )
        } finally {
            driver.quit()
        }
    }

    private fun loginAndClaimDailyReward(): String {
        driver.get(MISSION_URL)
        dismissCookieBanner()

        // Login state was already checked by ensureLoggedIn().
        val row = waitFor(15).until(ExpectedConditions.visibilityOfElementLocated(SIGN_IN_ROW))

        when (readSignInState()) {
            SignInState.CLAIMED -> {
                log.info("[BlaBlaLink] Already claimed today")
                return "Already claimed today"
            }

            SignInState.UNKNOWN -> {
                error("Daily Sign-in state not recognised, page may have changed")
            }

            SignInState.UNCLAIMED -> {}
        }

        val button = row.findElement(SIGN_IN_BUTTON)
        try {
            button.click()
        } catch (_: ElementClickInterceptedException) {
            js.executeScript("arguments[0].click();", button) // an overlay covers the box
        }

        try {
            waitFor(10).until { readSignInState() == SignInState.CLAIMED }
        } catch (_: TimeoutException) {
            error("Clicked Daily Sign-in but it did not switch to claimed")
        }
        log.info("[BlaBlaLink] Daily sign-in reward claimed")
        return "Sign-in reward claimed"
    }

    /** Re-reads the icon each time: the site re-renders the row after a click. */
    private fun readSignInState(): SignInState = signInState(driver.findElements(SIGN_IN_ICON).firstOrNull()?.getDomAttribute("class"))

    /**
     * Dismisses the cookie banner and waits until it is fully gone from the DOM,
     * so it cannot intercept subsequent clicks.
     */
    private fun dismissCookieBanner() {
        try {
            val cookieButton =
                waitFor(5).until(
                    ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")),
                )
            js.executeScript("arguments[0].click();", cookieButton)

            // Wait until the entire banner container is gone — not just invisible
            waitFor(5).until(
                ExpectedConditions.stalenessOf(cookieButton),
            )
            log.info("[BlaBlaLink] Cookie banner dismissed")
        } catch (_: Exception) {
            // Banner not present, nothing to do
        }
    }

    private fun browseAndLike(): String {
        val numOfPosts = 5
        driver.get(FEED_URL)
        waitFor(15).until(ExpectedConditions.visibilityOfElementLocated(POST))

        var postsLiked = 0
        var index = 0

        while (postsLiked < numOfPosts) {
            val posts = driver.findElements(POST)

            if (index >= posts.size) {
                // The scrolling element is an ancestor of the feed, so move the last post into view instead
                posts.lastOrNull()?.let { js.executeScript("arguments[0].scrollIntoView({block:'end'})", it) }
                // Wait for new posts to appear rather than a fixed sleep; none means the feed is exhausted
                if (!waitForMorePosts(posts.size)) break
                continue
            }

            try {
                if (likeAndRead(posts[index], index + 1)) postsLiked++
            } catch (e: Exception) {
                log.info("[BlaBlaLink] Skipping post #{}: {}", index + 1, e.message)
            }
            returnToFeed()
            index++
        }

        log.info("[BlaBlaLink] Liked $postsLiked posts")
        return "Liked $postsLiked posts."
    }

    /**
     * Likes [post] only when the site shows it not liked, so an existing like is never removed,
     * then opens it for the "read post" mission. True only when the like registered and the post opened.
     */
    private fun likeAndRead(
        post: WebElement,
        number: Int,
    ): Boolean {
        js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'})", post)
        Thread.sleep(800)

        fun readLikeState() = likeState(post.findElement(LIKE_ICON).getDomAttribute("style"))

        val state = readLikeState()
        if (state != LikeState.NOT_LIKED) {
            log.info("[BlaBlaLink] Skipping post #{}: like state {}", number, state)
            return false
        }

        js.executeScript("arguments[0].click();", post.findElement(LIKE_BUTTON)) // bypass overlay blocks
        try {
            waitFor(5).until { readLikeState() == LikeState.LIKED }
        } catch (_: TimeoutException) {
            log.info("[BlaBlaLink] Skipping post #{}: like did not register", number)
            return false
        }
        Thread.sleep(800)

        val openTarget = post.findElement(POST_OPEN)
        try {
            openTarget.click()
        } catch (_: ElementClickInterceptedException) {
            js.executeScript("arguments[0].click();", openTarget) // an overlay covers the post
        }
        waitFor(5).until(ExpectedConditions.urlContains("/post/detail"))
        Thread.sleep(800)
        driver.navigate().back()
        return true
    }

    /** Ends each post back on the feed; reloads it when a failure left the browser elsewhere. */
    private fun returnToFeed() {
        if (appears(POST, seconds = 5)) return
        driver.get(FEED_URL)
        waitFor(15).until(ExpectedConditions.visibilityOfElementLocated(POST))
    }

    /**
     * After scrolling, waits until the post count increases beyond [previousCount].
     * Returns false if no new posts appear within the timeout.
     */
    private fun waitForMorePosts(
        previousCount: Int,
        timeoutSeconds: Long = 5,
    ): Boolean =
        try {
            waitFor(timeoutSeconds).until { d -> d.findElements(POST).size > previousCount }
            true
        } catch (_: Exception) {
            log.warn("[BlaBlaLink] No new posts loaded after scroll, stopping: feed exhausted")
            false
        }

    /**
     * Startup step: checks the saved session once and opens a visible login window only
     * when it is logged out. Never throws, so a failed login cannot stop other jobs.
     */
    fun ensureSession() =
        ensureSession(
            isLoggedIn = { withDriver(headless) { isLoggedIn() } },
            login = ::loginInteractively,
            log = log,
        )

    /**
     * Visible window: opens the login popup from the mission page's "Log In" chip, then the
     * user logs in and solves the captcha within 10 minutes. Throws if still logged out after.
     */
    private fun loginInteractively() =
        withDriver(headless = false) {
            driver.get(MISSION_URL)
            dismissCookieBanner()

            val logInChip = waitFor(15).until(ExpectedConditions.visibilityOfElementLocated(LOG_IN_CHIP))
            // shortcut: auto-click is best effort (a JS click did not open the panel); the user can click it
            try {
                logInChip.findElement(LOG_IN_CHIP_CLICKABLE).click()
            } catch (e: Exception) {
                log.info("[BlaBlaLink] Could not click Log In: {}", e.message)
            }
            log.info("[BlaBlaLink] If the login panel does not open, click Log In. You have 10 minutes.")

            // One 10-minute budget covers opening the panel and finishing the login
            val deadline = System.nanoTime() + Duration.ofMinutes(10).toNanos()

            fun remainingSeconds() = maxOf(1, Duration.ofNanos(deadline - System.nanoTime()).seconds)

            val loginPanelLocator = By.xpath("//div[@data-cname='login-panel']")
            waitFor(remainingSeconds()).until(ExpectedConditions.visibilityOfElementLocated(loginPanelLocator))

            log.info("[BlaBlaLink] Login panel open. Log in and solve the captcha.")
            waitFor(remainingSeconds()).until(ExpectedConditions.invisibilityOfElementLocated(loginPanelLocator))

            check(isLoggedIn()) { "Login panel closed but session is still logged out" }
            log.info("[BlaBlaLink] Logged in. Session saved.")
        }

    private fun <T> withDriver(
        headless: Boolean,
        block: () -> T,
    ): T {
        driver = buildDriver(headless)
        try {
            return block()
        } finally {
            driver.quit()
        }
    }

    /**
     * Reads login state from the mission page without claiming anything.
     * /mission never redirects when logged out; it shows a "Log In" chip instead.
     */
    private fun isLoggedIn(): Boolean {
        driver.get(MISSION_URL)
        val listRendered = appears(DAILY_SIGN_IN_ROW, seconds = 15)
        // Short grace period: the chip may render just after the mission list
        val logInVisible = listRendered && appears(LOG_IN_CHIP, seconds = 3)
        return sessionState(listRendered, logInVisible)
    }

    private fun appears(
        locator: By,
        seconds: Long,
    ): Boolean =
        try {
            waitFor(seconds).until(ExpectedConditions.visibilityOfElementLocated(locator))
            true
        } catch (_: TimeoutException) {
            false
        }

    private fun ensureLoggedIn() {
        if (!isLoggedIn()) throw SessionExpiredException()
    }

    private fun waitFor(seconds: Long): WebDriverWait = WebDriverWait(driver, Duration.ofSeconds(seconds))

    private fun buildDriver(headless: Boolean): WebDriver {
        WebDriverManager.chromedriver().setup()

        val prefs =
            mapOf(
                "credentials_enable_service" to false,
                "profile.password_manager_enabled" to false,
                "autofill.profile_enabled" to false,
            )

        // persistent profile holding the logged-in session
        val profileDir = Paths.get(System.getProperty("user.home"), ".blablalink-profile")
        prepareProfileDir(profileDir)

        val options =
            ChromeOptions().apply {
                setExperimentalOption("prefs", prefs)
                addArguments(chromeArguments(headless, profileDir))
                setExperimentalOption("excludeSwitches", listOf("enable-automation"))
                setExperimentalOption("useAutomationExtension", false)
            }
        return ChromeDriver(options)
    }
}

/**
 * Chrome flags for every launch. Sandbox stays on and DevTools keeps its origin check:
 * the profile holds a logged-in session while the browser opens user-written posts.
 */
internal fun chromeArguments(
    headless: Boolean,
    profileDir: Path,
): List<String> =
    buildList {
        add("--disable-notifications")
        add("--disable-infobars")
        if (headless) add("--headless=new")
        add("--user-data-dir=${profileDir.toAbsolutePath()}")
        add("--profile-directory=Default")
        add("--disable-dev-shm-usage")
        add("--disable-gpu")
        add("--disable-blink-features=AutomationControlled")
        add("--window-size=1280,800")
    }

private const val MISSION_URL = "https://www.blablalink.com/mission"

// Classic layout: visible runs then share the post structure headless runs always get (the site saves the choice)
private const val FEED_URL = "https://www.blablalink.com/?pc_layout=legacy&plate_type=outpost"
private val POST = By.xpath("//div[@data-cname='infinite-scroll']/div[@plate_name]")
private val LIKE_BUTTON = By.xpath(".//span[@data-cname='like']")
private val LIKE_ICON = By.xpath(".//span[@data-cname='like']//span[@data-cname='svg-icon']")
private val POST_OPEN = By.xpath("./div[2]")
private val DAILY_SIGN_IN_ROW = By.xpath("//div[contains(text(), 'Daily Sign-in')]")
private val LOG_IN_CHIP = By.xpath("//div[normalize-space(text())='Log In']")
private val LOG_IN_CHIP_CLICKABLE = By.xpath("./ancestor::div[contains(@class, 'cursor-pointer')][1]")
private const val SIGN_IN_ROW_XPATH = "//div[normalize-space(text())='Daily Sign-in']/ancestor::div[@data-cname='index'][1]"
private val SIGN_IN_ROW = By.xpath(SIGN_IN_ROW_XPATH)
private val SIGN_IN_BUTTON = By.xpath(".//div[contains(@class, 'cursor-pointer')]")
private val SIGN_IN_ICON =
    By.xpath("$SIGN_IN_ROW_XPATH//div[contains(@class, 'cursor-pointer')]//div[contains(@class, 'icon-gift')]")

internal enum class SignInState { CLAIMED, UNCLAIMED, UNKNOWN }

/** The Daily Sign-in icon is `icon-gift.png` before the claim and `icon-gift-true.png` after. */
internal fun signInState(iconClass: String?): SignInState =
    when {
        iconClass == null -> SignInState.UNKNOWN
        "icon-gift-true" in iconClass -> SignInState.CLAIMED
        "icon-gift" in iconClass -> SignInState.UNCLAIMED
        else -> SignInState.UNKNOWN
    }

internal enum class LikeState { LIKED, NOT_LIKED, UNKNOWN }

/** The like icon's inline style is `color: var(--text-3)` before a like and `color: var(--brand-1)` after. */
internal fun likeState(iconStyle: String?): LikeState =
    when {
        iconStyle == null -> LikeState.UNKNOWN
        "--brand-1" in iconStyle -> LikeState.LIKED
        "--text-3" in iconStyle -> LikeState.NOT_LIKED
        else -> LikeState.UNKNOWN
    }

/**
 * Maps mission-page observations to logged in (true) or logged out (false).
 * An unrendered page is an error, never "logged in".
 */
internal fun sessionState(
    listRendered: Boolean,
    logInVisible: Boolean,
): Boolean {
    check(listRendered) { "Mission page did not render, login state unknown" }
    return !logInVisible
}

/**
 * Calls [login] only when [isLoggedIn] is false. Any failure is logged, never thrown:
 * runs then fail with a session-expired alert instead of stopping startup.
 */
internal fun ensureSession(
    isLoggedIn: () -> Boolean,
    login: () -> Unit,
    log: Logger,
) {
    try {
        if (isLoggedIn()) {
            log.info("[BlaBlaLink] Saved session is logged in")
            return
        }
        log.info("[BlaBlaLink] Saved session is logged out, opening login window")
        login()
    } catch (e: Exception) {
        log.warn("[BlaBlaLink] Session setup failed, runs will report session expired: {}", e.message)
    }
}

/** Creates the profile dir if missing and restricts it to its owner (session cookies live here). */
internal fun prepareProfileDir(dir: Path) {
    Files.createDirectories(dir)
    Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"))
}
