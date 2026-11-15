# Proposal

## Why

Two startup checks are missing or come too late.

- A Discord job with an empty `interactions` list loads, and every run reports success without claiming anything. This is backlog item #28 in the baseline design.
- Blank or duplicate job names, a bad `dailyTime` and a bad `timezone` are checked in `SchedulerManager.schedule`, inside the loop that also starts `runOnStartup` threads. An earlier job's startup run can therefore begin before a later job's bad value stops startup. The blablalink session check also runs before these values are checked, so the user may be asked to log in only for startup to fail afterwards. This gap was noted in the `validate-retry-policy` change.

## What Changes

- Every enabled Discord job must have at least one interaction. Otherwise startup stops with `Job '<name>': interactions must not be empty`.
- Startup order becomes:
  1. Check each enabled job's retry policy and Discord interactions.
  2. Register every enabled job with the scheduler, which checks names, `dailyTime` and `timezone`.
  3. Run the blablalink session check.
  4. Start the scheduler, then start the `runOnStartup` runs.
- Nothing runs and no browser opens until every enabled job has passed every check.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. `discord-claim` "Discord job configuration" already requires a non-empty `interactions` list. `configuration` "Schedule values validated at startup" already requires the check "before any job runs". The code does not meet either yet, so this change sets `skip_specs: true`.

## Impact

- Code: `DiscordConfig.kt` (a `requireValid(jobName)` check), `Main.kt` (startup order).
- Tests: a unit test for the empty and non-empty `interactions` cases.
- Users: a config with an empty interaction list on an enabled Discord job now fails at startup.
