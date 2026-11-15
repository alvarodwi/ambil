# Proposal

## Why

`maxAttempts: 0` is accepted. `RetryExecutor` then never enters its loop, so the job never runs, and every day it reports "Failed after 0 attempts: no attempt made". Negative delays make `Thread.sleep` throw, and a `multiplier` below 1 shrinks the backoff instead of growing it. None of this is caught at startup. This is backlog item #12 in the baseline design.

## What Changes

- At startup, before the blablalink session check and before any job is scheduled or run, every enabled job's retry policy is checked:
  - `maxAttempts` is at least 1
  - `initialDelaySeconds` and `maxDelaySeconds` are not negative
  - `multiplier` is at least 1
- A bad policy stops startup with an error that names the job and the bad field, for example `Job 'discord': retry.maxAttempts must be at least 1, was 0`.
- Disabled jobs are not checked, as `job-scheduling` "Only enabled jobs run" requires.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. `job-scheduling` already has "Valid retry policy" with the "Zero attempts" scenario. The code does not meet it yet, so this change sets `skip_specs: true`.

## Impact

- Code: `RetryPolicyConfig.kt` (a `requireValid(jobName)` check), `Main.kt` (one call per enabled job, before `ensureSession`).
- Tests: unit tests for each rejected field, plus a valid policy and the edge values (`maxAttempts: 1`, delays `0`, `multiplier: 1.0`).
- Users: a `config.json` with a bad policy on an enabled job now fails at startup instead of failing silently each day.
- Not included: blank and duplicate names, `dailyTime` and `timezone` are already checked in `SchedulerManager.schedule`. Those checks run inside the scheduling loop, so an earlier job's `runOnStartup` thread can start before a later job's bad name stops startup. That ordering gap is left for its own change.
