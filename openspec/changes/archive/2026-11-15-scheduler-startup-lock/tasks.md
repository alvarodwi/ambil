# Tasks

## 1. Locked startup run

- [x] 1.1 Add `SchedulerManager.runNow(name)` that runs a registered job once on a new thread through the same overlap lock as scheduled runs, and verify a unit test where a second `runNow` during a blocked first run is skipped passes with `./gradlew test`
- [x] 1.2 In `Main.kt`, start `runOnStartup` runs with `scheduler.runNow(name)` instead of `Thread { executeJob(...) }`, and verify `./gradlew check` passes

## Workflow follow-up

- Branch: `fix/scheduler-startup-lock`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff fix/scheduler-startup-lock`, then `git branch -d fix/scheduler-startup-lock`.
