# Tasks

## 1. Retry executor

- [x] 1.1 Remove the `telegram` parameter and the `telegram.send` call from `RetryExecutor`. Track the last error from a failed result or an exception, and return `JobResult(false, "Failed after N attempts: <last error>")`. Verify with a new `RetryExecutorTest`: a fake `JobTask` that always fails with "boom" under `maxAttempts: 3`, delays `0`, gets 3 calls and a message containing "3 attempts" and "boom"
- [x] 1.2 Catch `SessionExpiredException` in `RetryExecutor` before the generic catch, and return `JobResult(false, "Action needed: ${e.message}")` without retrying. Verify with a test: a fake job that throws it under `maxAttempts: 5` gets exactly 1 call, and the message starts with "Action needed"
- [x] 1.3 Add a test that a fake job failing once and then succeeding under `maxAttempts: 3` gets 2 calls and returns success. Verify `./gradlew test` passes

## 2. Blablalink client

- [x] 2.1 Set the `SessionExpiredException` message to the manual-login instruction in design.md. Remove the `SessionExpiredException` catch, `notifySessionExpired()`, the `notifier.send` in the generic catch, and the `notifier` constructor parameter from `BlablalinkClient`. Verify `grep -n "notifier" src/main/kotlin/me/varoa/ambil/service/BlablalinkClient.kt` finds nothing

## 3. Main

- [x] 3.1 Update `Main.kt`: build `RetryExecutor()` and `BlablalinkClient(headless = ...)` without notifiers, and drop the `notifier` parameter from `createTask`. Verify `./gradlew build` passes and `grep -rn "\.send(" src/main` finds only the three calls in `executeJob` and the one in `TelegramNotifier`

## 4. Manual check on home server

- [x] 4.1 Stop the network to Discord, or set a wrong `userToken` in a copy of `config.json`. Run once with `runOnStartup: true` and a short retry policy. Verify Telegram gets exactly one "❌ Job Failed" message for the run, with the error text

## Workflow follow-up

- Implement on branch `fix/single-outcome-notification`, archive the change, then merge into `develop` with `--no-ff` per `CONTRIBUTING.md`.
- Do `fix/job-result-messages` next. It builds on the message path this change leaves.
