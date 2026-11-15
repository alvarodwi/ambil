# Proposal

## Why

Every startup opens the blablalink login window for every blablalink job, even when the saved session is still logged in. If the login panel does not show within 15 seconds, or the user is not at the machine within 10 minutes, startup crashes and no job runs, Discord included. The browser also runs with its sandbox off and its DevTools port open to any origin, while it holds the logged-in session and opens pages written by other users. Backlog items #13, #14, #16 and #30 in the baseline design.

## What Changes

- At startup, check the saved blablalink session once. Open the visible login window only when it is logged out.
- If the login window fails or times out, log it and keep running. Blablalink runs then fail with the session-expired alert, and other jobs run normally.
- Re-login after an expiry means restarting ambil. The session-expired alert says so.
- Remove `username`/`password` from the blablalink job config, and remove the dead automatic-login code path. Existing configs that still have them keep loading, because unknown keys are ignored.
- Stop passing `--no-sandbox` and `--remote-allow-origins=*` to Chrome. Keep the profile directory readable by its owner only.
- Add a unit-test setup (backlog #26), so this change and later ones ship with tests.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `blablalink-claim`: the login flow is a startup check that never blocks other jobs. Legacy credentials in config are ignored. A new requirement keeps browser security protections on.

## Impact

- Code: `Main.kt` (startup), `BlablalinkClient.kt` (session check, login flow, driver options, alert text), `BlablalinkConfig.kt`.
- Build: test dependencies and JUnit platform in `build.gradle.kts`. New `src/test/kotlin`.
- Ops: on the home server, the first start after deploy must still be able to launch Chrome without `--no-sandbox`. If ambil runs as root, Chrome refuses to start, so run it as a normal user.
- `config.json.example` keeps its blablalink credentials until `fix/config-example`.
