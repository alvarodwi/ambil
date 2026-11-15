# Proposal

## Why

ambil uses two HTTP clients for two small POST calls. `DiscordClient` uses OkHttp, and `TelegramNotifier` uses the JDK's `java.net.http`. That means two timeout setups, two error shapes and one extra dependency. This is backlog item #25 in the baseline design.

## What Changes

- `DiscordClient` sends its interaction with `java.net.http.HttpClient`, like `TelegramNotifier`. It keeps the 15-second connect timeout and the 15-second request timeout, and sends the same headers and body.
- Status handling stays the same: 200 and 204 succeed, 429 fails as "Rate limited", and any other status fails with `HTTP <code>: <body>`.
- OkHttp is removed from `build.gradle.kts` and `versions.properties`.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. Code hygiene only, so this change sets `skip_specs: true`.

## Impact

- Code: `DiscordClient.kt`, `build.gradle.kts`, `versions.properties`.
- The shadow jar no longer bundles OkHttp.
