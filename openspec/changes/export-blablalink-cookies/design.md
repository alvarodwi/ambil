# Design

## Context

See proposal.md, "Why". `BlablalinkClient.isLoggedIn()` opens `/mission` in the persistent profile
(`~/.blablalink-profile`) and is the single session check. `ensureSession()` (startup, through
`withDriver`), `loginInteractively()` (its final check) and `run()` (`ensureLoggedIn`) all go through it.
shiniri-cli's `parse_cookie` accepts a plain `name=value; ...` line, with or without a `Cookie:` prefix, and
requires `game_token` and `game_openid`. It re-reads the file on every check, so ambil only has to keep the
file current.

## Goals / Non-Goals

**Goals:**
- A fresh cookie file every time ambil confirms it is logged in.
- shiniri-cli never reads a half-written or useless file.
- No new failure mode for the claim job.

**Non-Goals:**
- Extra session checks on a timer. The startup check plus the daily run decide how often the cookie is
  refreshed. If testing shows that's not often enough, a separate change can add a cookie-only job.
- Telling shiniri-cli anything beyond the file contents, such as a signal or a lock.
- Verifying that the cookie works against the game API. The user tests that directly.

## Decisions

- **Export from `isLoggedIn()`, only when it returns true.** It is the one place every path confirms the
  session, so no caller changes.
  - Alternative: export at the end of `run()`. Rejected, because it would miss the startup check and the
    manual-login confirmation, which are exactly when a new session appears.
- **Use the cookies Selenium can see on `/mission`.** `driver.manage().cookies` returns the cookies that
  apply to `www.blablalink.com`, including those on `.blablalink.com`, which is where the API cookies live.
  Write every one. shiniri-cli ignores the extras.
- **Pure, testable helpers.**
  - `cookieHeader(cookies: List<Pair<String, String>>): String?` joins them as `name=value; name=value` in
    the browser's order. It returns `null` if `game_token` or `game_openid` is missing or blank.
  - `writeCookieFile(path: Path, header: String)` creates the parent directories and writes
    `<name>.tmp` next to the target. It sets `rw-------` when the file system supports POSIX permissions,
    then runs `Files.move(tmp, path, ATOMIC_MOVE, REPLACE_EXISTING)`.
- **Never fail because of the export.** The browser side
  (`exportCookies()`, called inside `isLoggedIn()`) catches every exception and logs
  `[BlaBlaLink] Cookie export failed: <msg>`. A `null` header logs
  `[BlaBlaLink] Cookie export skipped: game_token or game_openid missing`. Success logs
  `[BlaBlaLink] Cookies exported to <path>`, without the values.
- **The config is top-level, not per job.** There is one browser profile per app (only one blablalink job
  is allowed), and the startup check isn't tied to a job. `AppConfig.cookieExportPath: String? = null`, and
  `Main` passes `Paths.get(it)` into both `BlablalinkClient(...)` constructions, the startup one and the one
  in `createTask`.
- **Permissions.** The file holds a live session, so it gets the same owner-only treatment as the profile
  directory. Linux is the target. On a file system without POSIX permissions, skip the chmod.

## Risks / Trade-offs

- The cookie refreshes only on ambil's checks, which run at startup and on the daily run. If the game token
  lasts less than a day, shiniri-cli still sees `cookie rejected` between runs. Testing shows whether that
  happens (see the Non-Goals).
- Both programs must run as the same user, or the owner-only file is unreadable to shiniri-cli. The user
  runs both on the homeserver.
