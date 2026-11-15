# Proposal

## Why

One failed run can send up to 3 Telegram messages: one from `BlablalinkClient`, one from `RetryExecutor` ("Failed after N attempts") and one from `Main`. An expired blablalink session is also retried on every attempt, so the user gets the "session expired" alert up to 5 times per run, though no retry can fix it. These are backlog items #17 and #18 in the baseline design.

## What Changes

- Only the job runner (`Main`) sends Telegram messages for a job run. `BlablalinkClient` and `RetryExecutor` stop sending.
- A run that fails after all retries reports the last attempt's error, not only "Failed after N attempts".
- An expired session stops the run after the first attempt, with no retries. Its outcome message tells the user what to do, so the run still sends exactly one message.
- The startup login flow is unchanged. It never sent Telegram messages.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `notification`: "One outcome message per job run" gains a scenario for the last error. "Action-needed alert" now states that the alert is the run's outcome message, not a second message.
- `job-scheduling`: new requirement that a run needing user action is not retried.

## Impact

- Code: `RetryExecutor.kt`, `BlablalinkClient.kt`, `Main.kt`, `SessionExpiredException.kt`.
- `RetryExecutor` and `BlablalinkClient` no longer take a `TelegramNotifier`.
- Tests: new `RetryExecutorTest` with a fake `JobTask`.
- Behaviour: fewer Telegram messages, and failure messages now carry the cause.
