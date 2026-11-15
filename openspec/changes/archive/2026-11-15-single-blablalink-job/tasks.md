# Tasks

## 1. Startup check

- [x] 1.1 In `Main.kt`, next to the per-job retry and Discord checks, fail with `Only one enabled blablalink job is supported, found N: '<a>', '<b>'` when more than one enabled job has a blablalink config. Verify by reading the code that the check runs before `ensureSession`, scheduling and any startup run, and that `./gradlew check` passes

## Workflow follow-up

- Branch: `fix/single-blablalink-job`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff fix/single-blablalink-job`, then `git branch -d fix/single-blablalink-job`.
