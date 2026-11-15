# Tasks

## 1. Policy check

- [x] 1.1 Add `RetryPolicyConfig.requireValid(jobName)` that throws `IllegalArgumentException` naming the job and field when `maxAttempts < 1`, `initialDelaySeconds < 0`, `maxDelaySeconds < 0` or `multiplier < 1`, and verify new unit tests for each rejected field and for the edge values `maxAttempts: 1`, delays `0`, `multiplier: 1.0` pass with `./gradlew test`

## 2. Startup wiring

- [x] 2.1 In `Main.kt`, call `requireValid(name)` for every enabled job right after `enabledJobs` is built, before `ensureSession`, and verify by reading the code that no job is scheduled, started or given a browser session before the check, and that `./gradlew check` passes

## Workflow follow-up

- Branch: `fix/validate-retry-policy`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff fix/validate-retry-policy`, then `git branch -d fix/validate-retry-policy`.
