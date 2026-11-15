# Proposal

## Why

`DiscordClient` builds its JSON with Gson, but `build.gradle.kts` never declares Gson. It only compiles because Selenium or WebDriverManager pull Gson in. A dependency bump on their side could remove it and break the build. The rest of the app uses kotlinx.serialization. `versions.properties` also keeps eight ktlint entries that refreshVersions marks `## unused`. These are backlog items #24 and the leftover part of #26 in the baseline design.

## What Changes

- `DiscordClient` builds the interaction payload and the super-properties with kotlinx.serialization `buildJsonObject`. The JSON sent to Discord keeps the same fields and values.
- The unused ktlint entries are removed from `versions.properties`. `kotlinter` brings its own ktlint.
- No behaviour change.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. Code hygiene only, so this change sets `skip_specs: true`.

## Impact

- Code: `DiscordClient.kt`, `versions.properties`.
- Tests: a unit test that the payload JSON has the expected fields and values.
