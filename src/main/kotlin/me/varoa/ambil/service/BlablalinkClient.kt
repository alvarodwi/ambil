package me.varoa.ambil.service

import io.github.bonigarcia.wdm.WebDriverManager
import me.varoa.ambil.model.data.BlablalinkResult
import me.varoa.ambil.model.exception.SessionExpiredException
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration

class BlablalinkClient(
//    private val config: BlablalinkConfig,
    private val notifier: TelegramNotifier,
    private val headless: Boolean = true,
) {
    private val log = LoggerFactory.getLogger(BlablalinkClient::class.java)
    private lateinit var driver: WebDriver
    private val js get() = driver as JavascriptExecutor

    fun run(): BlablalinkResult {
        driver = buildDriver()

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
            notifySessionExpired()
            BlablalinkResult(
                signIn = "Session expired",
                likes = "-",
                success = false,
            )
        } catch (e: Exception) {
            notifier.send(
                """
            ❌ BlaBlaLink automation failed
            
            ${e.message}
            """.trimIndent()
            )

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

    private fun notifySessionExpired() {
        notifier.send(
            """
        ⚠️ BlaBlaLink session expired
        
        Manual login required.
        Please start the browser in non-headless mode and solve the captcha.
        """.trimIndent()
        )
    }

    private fun loginAndClaimDailyReward(): String {
        driver.get("https://www.blablalink.com/mission")
        dismissCookieBanner()

        val dailySignInXPath =
            "//div[contains(text(), 'Daily Sign-in')]//../following-sibling::div[contains(@class, 'cursor-pointer')]"

        // Click daily sign-in if visible, otherwise we may already be on login page
        try {
            waitFor(10)
                .until(
                    ExpectedConditions.elementToBeClickable(By.xpath(dailySignInXPath)),
                ).click()
        } catch (_: Exception) {
            // not present yet — may have redirected to login
        }

        val redirectedToLogin =
            try {
                waitFor(3).until(ExpectedConditions.urlToBe("https://www.blablalink.com/login"))
                true
            } catch (_: Exception) {
                false
            }

        if (redirectedToLogin) {
            login()
            driver.get("https://www.blablalink.com/mission")
            dismissCookieBanner()
            waitFor(10)
                .until(
                    ExpectedConditions.elementToBeClickable(By.xpath(dailySignInXPath)),
                ).click()
        }

        return try {
            waitFor(5).until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(text(), 'Sign In Success')]"),
                ),
            )
            log.info("[BlaBlaLink] Daily sign-in reward claimed!")
            "Sign in reward claimed!"
        } catch (_: Exception) {
            log.info("[BlaBlaLink] Already signed in today")
            "Sign in again tomorrow!"
        }
    }

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

    private fun login() {
        error(
            """
        Automatic login disabled.
        Use persistent Chrome profile.
        Login manually when session expires.
        """.trimIndent()
        )
    }

    private fun browseAndLike(): String {
        val numOfPosts = 5
        driver.get("https://www.blablalink.com/?plate_type=outpost")

        // Wait until the infinite scroll container has at least one post loaded
        val scrollContainerXPath = "//div[@data-cname='infinite-scroll']"
        val firstPostXPath = "$scrollContainerXPath//div[@data-cname='index'][1]"
        waitFor(15).until(
            ExpectedConditions.visibilityOfElementLocated(By.xpath(firstPostXPath)),
        )

        var postsLiked = 0
        var currentIndex = 1

        while (postsLiked < numOfPosts) {
            val scrollContainer =
                waitFor(10).until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(scrollContainerXPath)),
                )
            val posts = scrollContainer.findElements(By.xpath(".//div[@data-cname='index']"))

            if (currentIndex > posts.size) {
                js.executeScript(
                    "arguments[0].scrollTop = arguments[0].scrollHeight",
                    scrollContainer,
                )
                // Wait for new posts to appear rather than a fixed sleep
                waitForMorePosts(scrollContainerXPath, posts.size)
                continue
            }

            val postXPath = "$scrollContainerXPath//div[@data-cname='index'][$currentIndex]"
            val post: WebElement = driver.findElement(By.xpath(postXPath))

            js.executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth', block:'center'})",
                post,
            )
            Thread.sleep(800)

            try {
                val likeButton =
                    post.findElement(
                        By.xpath("./div[last() - 1]/div[last()]//span[3]//span[@data-cname='like']"),
                    )
                val color = likeButton.getCssValue("color")
                val isLiked = color.contains("62, 175, 255")

                if (!isLiked) {
                    js.executeScript("arguments[0].click();", likeButton) // bypass overlay blocks
                    postsLiked++
                    Thread.sleep(800)

                    // Open post detail to satisfy "read post" mission, then go back
                    post.findElement(By.xpath("./div[2]")).click()
                    waitFor(5).until(ExpectedConditions.urlContains("/post/detail"))
                    Thread.sleep(800)
                    driver.navigate().back()

                    // Wait until the feed is visible again before continuing
                    waitFor(10).until(
                        ExpectedConditions.visibilityOfElementLocated(By.xpath(scrollContainerXPath)),
                    )
                }
            } catch (e: Exception) {
                log.info("[BlaBlaLink] Skipping post #{}: {}", currentIndex, e.message)
            }

            currentIndex++
        }

        log.info("[BlaBlaLink] Liked $postsLiked posts")
        return "Liked $postsLiked posts."
    }

    /**
     * After scrolling, waits until the post count increases beyond [previousCount].
     * Falls back gracefully if no new posts appear within the timeout.
     */
    private fun waitForMorePosts(
        scrollContainerXPath: String,
        previousCount: Int,
        timeoutSeconds: Long = 5,
    ) {
        try {
            waitFor(timeoutSeconds).until { d ->
                val container = d.findElements(By.xpath(scrollContainerXPath)).firstOrNull()
                val current = container?.findElements(By.xpath(".//div[@data-cname='index']"))?.size ?: 0
                current > previousCount
            }
        } catch (_: Exception) {
            log.warn("[BlaBlaLink] No new posts loaded after scroll — feed may be exhausted")
        }
    }

    fun initializeSession() {
    driver = buildDriver()
    try {
        driver.get("https://www.blablalink.com/login")
        
        log.info("Trigger the login dialog if it doesn't open automatically, then solve captcha.")
        val loginPanelLocator = By.xpath("//div[@data-cname='login-panel']")

        log.info("Waiting for login panel to load...")
        waitFor(15).until(
            ExpectedConditions.visibilityOfElementLocated(loginPanelLocator)
        )
        
        log.info("Login panel detected. Please log in manually and solve the captcha.")
        log.info("Waiting for login panel to disappear after successful authentication...")

        waitFor(600).until(
            ExpectedConditions.invisibilityOfElementLocated(loginPanelLocator)
        )

        log.info("Login panel gone. Session saved.")
    } catch (e: Exception) {
        log.error("Session initialization failed: ${e.message}")
        throw e
    } finally {
        driver.quit()
    }
}

    private fun ensureLoggedIn() {
        driver.get("https://www.blablalink.com/mission")

        Thread.sleep(3000)

        val url = driver.currentUrl

        if (url?.contains("/login") ?: false) {
            throw SessionExpiredException()
        }
    }

    private fun waitFor(seconds: Long): WebDriverWait = WebDriverWait(driver, Duration.ofSeconds(seconds))

    private fun buildDriver(): WebDriver {
        WebDriverManager.chromedriver().setup()

        val prefs =
            mapOf(
                "credentials_enable_service" to false,
                "profile.password_manager_enabled" to false,
                "autofill.profile_enabled" to false,
            )

        val options =
            ChromeOptions().apply {
                setExperimentalOption("prefs", prefs)
                addArguments("--disable-notifications", "--disable-infobars")
                if (headless) addArguments("--headless=new")

                // persistent profile
                val profileDir =
                    Paths.get(
                        System.getProperty("user.home"),
                        ".blablalink-profile",
                    )
                addArguments("--user-data-dir=${profileDir.toAbsolutePath()}")
                addArguments("--profile-directory=Default")

                addArguments(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--remote-allow-origins=*",
                    "--disable-blink-features=AutomationControlled",
                    "--window-size=1280,800",
                )
                setExperimentalOption("excludeSwitches", listOf("enable-automation"))
                setExperimentalOption("useAutomationExtension", false)
            }
        return ChromeDriver(options)
    }
}
