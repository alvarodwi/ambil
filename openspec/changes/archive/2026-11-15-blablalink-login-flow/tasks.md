# Tasks

## 1. Test setup

- [x] 1.1 Add `testImplementation(kotlin("test"))`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` and `tasks.test { useJUnitPlatform() }` to `build.gradle.kts`, add one trivial test under `src/test/kotlin`, and verify `./gradlew test` runs it and passes

## 2. Credential-free blablalink config

- [x] 2.1 Change `BlablalinkConfig` to a `@Serializable @SerialName("blablalink") data object`, and remove the commented-out config param in `BlablalinkClient`. Verify with a `ConfigLoader.json` test that a job config with only `"type": "blablalink"` and one with legacy `username`/`password` both decode to `BlablalinkConfig`

## 3. Browser hardening

- [x] 3.1 Extract `chromeArguments(headless, profileDir)` and drop `--no-sandbox` and `--remote-allow-origins=*`. Verify with a test that the list lacks both flags, contains `--headless=new` only when headless, and contains `--user-data-dir=<profileDir>`
- [x] 3.2 Before each Chrome launch, create the profile dir if missing and set it to `rwx------`. Verify with a test on a temp dir that the permissions are exactly owner `rwx`

## 4. Startup session check

- [x] 4.1 Add `ensureSession(isLoggedIn, login, log)`, which calls `login` only when `isLoggedIn()` is false and logs, rather than throws, any exception. Verify with tests that cover: logged in (login not called), logged out (called once), login throws (no exception escapes), and the check throws (no exception escapes)
- [x] 4.2 In `BlablalinkClient`, split `isLoggedIn()` out of `ensureLoggedIn()`. Let `buildDriver` take `headless`, make the login flow always visible, and expose `ensureSession()` wiring the two through 4.1. Verify `./gradlew build` passes, including kotlinter
- [x] 4.3 In `Main.kt`, remove `initializeSession()` from `createTask`. Before scheduling or starting any job, call `ensureSession()` once if any enabled job is `BlablalinkConfig`. Verify by reading the code: no browser call remains in `createTask`, and no exception path from the session step reaches `main`
- [x] 4.4 Delete `login()` and the redirect branch in `loginAndClaimDailyReward()`. Change the session-expired alert to say "restart ambil where a display is available and log in in the window that opens". Verify `./gradlew build` passes and `grep -n "fun login" src/main` finds nothing

- [x] 4.5 Add `sessionState(listRendered, logInVisible)` (rendered + chip means logged out, rendered without chip means logged in, not rendered throws). Rewrite `isLoggedIn()` to wait 15s for the `Daily Sign-in` row, then check for the "Log In" chip, with no sleep and no URL check. Verify with tests for all three `sessionState` cases
- [x] 4.6 Rewrite the login flow to open `/mission`, dismiss the cookie banner, click the "Log In" chip, wait for `login-panel` to appear then disappear, and then throw if `isLoggedIn()` is still false. Verify `./gradlew build` passes and `grep -n 'blablalink.com/login' src/main` finds nothing

## 5. Manual check on home server (re-run both after 4.5 and 4.6)

- [x] 5.1 Deploy the jar and start it with a logged-in profile. Verify that no login window opens, the log shows the session check passing, and Chrome starts without `--no-sandbox`
- [x] 5.2 Start it with a logged-out profile (temporarily rename `~/.blablalink-profile`) and let the window time out. Verify the log shows the timeout, the scheduler starts, and the Discord job still runs. Then restore the profile

## Workflow follow-up

- Archive the change, then merge `fix/blablalink-login-flow` into `develop` with `--no-ff` per `CONTRIBUTING.md`.
