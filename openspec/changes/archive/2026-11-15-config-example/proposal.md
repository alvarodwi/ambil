# Proposal

## Why

A new user cannot start from the example config. The missing-config error says "Copy config.example.json", but the file is `config.json.example`. The example itself is not valid JSON (it has `...` and `#` notes), and it lacks `telegram`, which the app requires. It also lacks the `type` discriminator on `config`, and it uses `name: ""`, `maxAttempts: 0` and the removed blablalink `username`/`password`. A copied example therefore fails to parse, or never runs. These are backlog items #7-#11 in the baseline design.

## What Changes

- The missing-config error names `config.json.example`, the file that exists. The file is not renamed: `.gitignore` ignores `*.json`, so a `config.example.json` would drop out of version control.
- `config.json.example` becomes valid JSON that `ConfigLoader` decodes into `AppConfig` as-is:
  - a `telegram` object with placeholder `token` and `chatId`
  - `headless: true`, the default, which also works over SSH
  - two jobs: one `"type": "discord"` with every interaction field, and one `"type": "blablalink"` with no credentials
  - non-blank unique names, every optional field shown (`enabled`, `runOnStartup`, `timezone`, `component_type`, interaction `name`), and a working retry policy (`maxAttempts: 3`)
  - both jobs set `enabled: false`, so a copy with placeholder values does not fire requests until the user enables a job
- A unit test decodes `config.json.example`, so the example cannot drift from the config model again.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. `configuration` already requires this ("Config missing" names the path; "Secrets stay out of version control" requires an example that documents every field). The code does not meet it yet, so this change sets `skip_specs: true`.

## Impact

- Files: `config.json.example`, `ConfigLoader.kt` (error text), `ConfigLoaderTest.kt` (new test).
- Users: no change for an existing `config.json`.
- Not included: startup validation of blank or duplicate names and of `maxAttempts` (backlog #12, `fix/validate-retry-policy`). Field explanations stay in the Kotlin model comments, since JSON has no comments.
