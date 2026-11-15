# Design

## Context

See proposal.md - Why. Today `Main.createTask` calls `BlablalinkClient.initializeSession()` once per blablalink job, before the scheduler starts. That always opens a browser at `/login`, waits 15 seconds for the login panel and then up to 600 seconds for it to disappear, and rethrows on any failure. Because `main` doesn't catch it, the process dies. Each run also calls `ensureLoggedIn()`, which assumes a logged-out `/mission` redirects to `/login`.

Found during apply: that assumption is false. `/mission` never redirects, and `/login` is a 404. So neither the startup check nor the per-run check could ever see a logged-out session. The old login flow only worked because the user opened the popup by hand. A logged-out `/mission`, rendered with a throwaway profile, shows a clickable "Log In" chip (default avatar) next to "Onboarding Mission". The owner confirmed it is gone when logged in.

## Goals / Non-Goals

**Goals:**
- One session check per startup, and a login window only when needed.
- Startup never dies because of blablalink.
- Unit-testable decisions, without driving a real browser in tests.

**Non-Goals:**
- Sign-in detection, feed-loop hang (#15, #29): `fix/blablalink-signin-detection`.
- Retry and notification behaviour on session expiry (#17, #18).
- Per-account profiles (#21).

## Decisions

- **Detect login state from the "Log In" chip.** `isLoggedIn()` opens `/mission`, waits up to 15 seconds for the `Daily Sign-in` row (the page rendered), then returns false if `//div[normalize-space(text())='Log In']` is displayed. If the row never shows, it throws, so an unrendered page is never read as logged in. Startup and every run use the same check. Alternative: click Daily Sign-in and see whether the login popup opens. Rejected, because when logged in that claims the reward as a side effect of the check.
- **Start login from the chip, with the user as fallback.** The visible login flow opens `/mission`, dismisses the cookie banner, and tries a native click on the chip's clickable parent. In testing, an earlier JS click did not open the panel, so it also logs a hint for the user to click `Log In`. One 10-minute budget covers both the panel appearing and the login finishing. It then calls `isLoggedIn()` again and throws if still logged out, so `ensureSession` logs a closed popup as a failure.
- **Pure marker check.** `sessionState(listRendered: Boolean, logInVisible: Boolean)` maps the two observations to logged in, logged out, or an unknown error, and is unit-tested. The browser code only gathers the two booleans.
- **Startup session step lives in `Main`, once.** Before any job is scheduled or started (so `runOnStartup` runs see the session), if any enabled job is `BlablalinkConfig`, call one `BlablalinkClient.ensureSession()` and catch any exception into a log line. `createTask` no longer touches the browser.
- **Testable seam.** Put the decision in a small top-level function, `ensureSession(isLoggedIn: () -> Boolean, login: () -> Unit, log)`: it calls `login` only when `isLoggedIn()` is false, and turns any exception into a warning. `BlablalinkClient` passes its browser-backed lambdas. Tests pass fakes. Alternative: an interface plus a fake client. Rejected, since that's more code for the same coverage.
- **Login window always visible.** `buildDriver(headless)` takes the mode as a parameter. The login flow passes `false`, and runs pass the config value.
- **Chrome arguments as a pure function.** `chromeArguments(headless, profileDir): List<String>` builds the argument list, so a test can check that the two removed flags stay gone. Keep `--disable-dev-shm-usage` (small `/dev/shm` on servers) and drop `--no-sandbox` and `--remote-allow-origins=*`. Selenium 4.43's HTTP client does not need the origins workaround.
- **Profile permissions.** Before launch, create the profile dir if missing and set it to `rwx------` with `Files.setPosixFilePermissions`. Linux-only is acceptable, since v1 targets Linux.
- **`BlablalinkConfig` becomes `data object`.** It stays `@Serializable @SerialName("blablalink")`. `ignoreUnknownKeys` drops legacy `username`/`password`, and a test pins this.
- **Delete dead code.** Remove `login()` and the redirect branch in `loginAndClaimDailyReward()`. The run's `ensureLoggedIn()` already throws `SessionExpiredException` before that point.
- **Test setup.** `testImplementation(kotlin("test"))`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` (Gradle 9 needs it explicitly), and `tasks.test { useJUnitPlatform() }`. There's no mocking library, since lambdas are enough.

## Risks / Trade-offs

- [Chrome refuses to start without `--no-sandbox` when run as root or in some containers] → Run ambil as a normal user. The task list includes a manual start check on the home server. If it fails, the error appears in the log and the run fails, and other jobs are unaffected.
- [Startup check costs one headless browser launch, about a few seconds] → It happens once per start, which is acceptable.
- [The 10-minute login window still blocks startup while open] → That's intended: the user is at the machine. Timeouts no longer kill the process.
- [Site renames or moves the "Log In" chip] → A logged-out session would read as logged in. Runs then fail later on missing elements, not silently. A selector change is a one-line fix in `isLoggedIn()`.
