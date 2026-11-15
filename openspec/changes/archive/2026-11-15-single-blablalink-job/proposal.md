# Proposal

## Why

Every blablalink job uses the same browser profile, `~/.blablalink-profile`, which holds one logged-in account. A blablalink job config has no field to pick another account. A second enabled blablalink job therefore claims the same account again, and if both run at once, the second Chrome fails because the profile is already in use. The overlap lock is per job name, so it does not stop that. This is backlog item #21 in the baseline design. The owner decided: one blablalink account, no per-account profiles.

## What Changes

- At startup, more than one enabled blablalink job stops ambil with an error naming the jobs, for example `Only one enabled blablalink job is supported, found 2: 'blablalink', 'blablalink-alt'`.
- Disabled blablalink jobs do not count.
- The "Several blablalink jobs" scenario under "Manual login flow" now says startup stops before any login window opens, since that case can no longer start.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `blablalink-claim`: adds "Single blablalink job". "Manual login flow" rewrites the "Several blablalink jobs" scenario.

## Impact

- Code: `Main.kt` (one startup check, next to the other per-job checks).
- Users: the live `config.json` has one blablalink job, so it keeps loading.
- Not included: per-account profiles. If a second account is ever needed, an optional `profile` field can map to its own profile directory, with no default change for existing configs.
