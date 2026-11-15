# Tasks

## 1. Discord interactions check

- [x] 1.1 Add `DiscordConfig.requireValid(jobName)` that throws `IllegalArgumentException` with `Job '<name>': interactions must not be empty` when `interactions` is empty, and verify a unit test for the empty and non-empty cases passes with `./gradlew test`

## 2. Startup order

- [x] 2.1 In `Main.kt`, check retry and Discord config for every enabled job, then register every enabled job with the scheduler, then run `ensureSession`, then start the scheduler, then start the `runOnStartup` runs. Verify by reading the code that no run starts and no browser opens before every enabled job is registered, and that `./gradlew check` passes

## Workflow follow-up

- Branch: `fix/startup-validation`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff fix/startup-validation`, then `git branch -d fix/startup-validation`.
