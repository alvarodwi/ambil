# Proposal

## Why

shiniri-cli (`../shiniri-cli`) calls the blablalink game API with a copied Cookie header in its
`config/cookie.txt`. That copy goes stale, and the user pastes a new one by hand. ambil already keeps a
logged-in Chrome profile on the same homeserver and checks it at startup and on every blablalink run.
Exporting that session's cookies to a file lets shiniri-cli pick up a fresh cookie without user action
whenever both run on the same machine.

## What Changes

- New optional top-level config value `cookieExportPath` (string, a file path). If it is absent, nothing
  changes.
- Whenever the session check (`isLoggedIn()`) finds the browser logged in, ambil writes the browser's
  blablalink cookies to `cookieExportPath` as one Cookie header line, `name=value; name=value`. That covers
  the startup check, the check after a manual login, and every daily run.
- The file is replaced atomically and gets owner-only permissions.
- A cookie set without both `game_token` and `game_openid` is not written, so a working file is never
  replaced by a useless one.
- Export failures are logged as warnings. They never fail the job or startup, and they send no Telegram
  message.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `blablalink-claim`: adds "Cookie export".
- `configuration`: "Top-level settings" gains the optional `cookieExportPath`.

## Impact

- Code:
  - `AppConfig.kt`: the new field.
  - `BlablalinkClient.kt`: a constructor parameter, the export call in `isLoggedIn()`, and pure helpers.
  - `Main.kt`: passes the path in at both `BlablalinkClient` constructions.
- Tests: unit tests for the header builder and the file writer.
- `config.json.example`: shows the field.
- Users: nothing changes unless `cookieExportPath` is set. For shiniri-cli, set it to
  `/path/to/shiniri-cli/config/cookie.txt`.
- **Not decided here:** whether the exported cookie keeps the game API answering with fresh data. The user
  will test that directly by running both programs.
