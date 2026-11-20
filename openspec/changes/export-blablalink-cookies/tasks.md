# Tasks

## 1. Pure helpers

- [x] 1.1 Add top-level `internal fun cookieHeader(cookies: List<Pair<String, String>>): String?` and `internal fun writeCookieFile(path: Path, header: String)` to `BlablalinkClient.kt`, as described in design.md "Pure, testable helpers". Add `CookieExportTest`, which covers:
  - `cookieHeader(listOf("game_token" to "t", "OptanonConsent" to "c", "game_uid" to "1", "game_openid" to "o")) == "game_token=t; game_uid=1; game_openid=o"` (only `game_*` names are kept, in order)
  - `null` when `game_openid` is missing, and when `game_token` is blank
  - `writeCookieFile` into a temp dir: it creates missing parent dirs, the file content equals the header, a second write replaces it, no `.tmp` file is left behind, and on POSIX the permissions are `rw-------`

  Verify with `./gradlew test`.

## 2. Config and wiring

- [x] 2.1 Add `val cookieExportPath: String? = null` to `AppConfig`. Add a `ConfigLoaderTest` case showing that a config without it loads with `null`, and that one with it loads the string. Add `"cookieExportPath": "/home/<user>/shiniri-cli/config/cookie.txt"` to `config.json.example`. Verify with `./gradlew test`.
- [x] 2.2 Give `BlablalinkClient` the constructor parameter `cookieExportPath: Path? = null`. Add a private `exportCookies()` that maps `driver.manage().cookies` to name/value pairs, then calls `cookieHeader` and `writeCookieFile`. It catches and logs everything, using the three log lines in design.md, and never logs the values. In `isLoggedIn()`, call it when the result is true and the path is set. In `Main.kt`, pass `appConfig.cookieExportPath?.let(Paths::get)` to both `BlablalinkClient(...)` constructions. The one in `createTask` gets it through a new `createTask` parameter, the same way `headless` arrives. Verify that `./gradlew check` passes.

## 3. Live check (user, on the homeserver)

- [ ] 3.1 Set `cookieExportPath` to shiniri-cli's `config/cookie.txt` and start ambil. Confirm that the log shows `Cookies exported to ...`, the file is `rw-------`, and `uv run shiniri-cli` can fetch (for example `union live` or the `watch` check) with no manual cookie paste. Also confirm that the file holds only `game_*` cookies. The next day, compare the file's `game_token` with the day before. If it is the same, A1 (design.md, "Token lifetime") holds. If it changed, or its expiry moved forward, A2 holds, so update that note.

## Workflow follow-up

- Branch: `feat/export-blablalink-cookies`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff feat/export-blablalink-cookies`, then `git branch -d feat/export-blablalink-cookies`.
