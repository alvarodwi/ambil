# Tasks

## 1. Wall-clock schedule

- [x] 1.1 Add a pure `nextRun(dailyTime, zone, after)` that returns the first `dailyTime` in `zone` strictly after `after`, and verify unit tests for later today, already passed, and the `America/New_York` 2026-03-08 daylight saving change pass with `./gradlew test`
- [x] 1.2 Replace `scheduleAtFixedRate` with a one-shot schedule per run that schedules the next run with `nextRun` when the run ends, and have `stop()` cancel pending runs and schedule no more. Verify `./gradlew check` passes

## Workflow follow-up

- Branch: `fix/scheduler-wall-clock`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff fix/scheduler-wall-clock`, then `git branch -d fix/scheduler-wall-clock`.
