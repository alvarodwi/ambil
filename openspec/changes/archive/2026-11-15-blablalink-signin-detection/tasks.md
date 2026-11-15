# Tasks

## 1. Sign-in state

- [x] 1.1 Add `signInState(iconClass: String?)` returning `CLAIMED`, `UNCLAIMED` or `UNKNOWN` as in design.md. Verify with unit tests using the claimed and unclaimed class strings from design.md, plus null and an unrelated class
- [x] 1.2 Rewrite `loginAndClaimDailyReward()`:
  - wait 15s for the Daily Sign-in row, then read its icon class
  - `CLAIMED`: return without clicking
  - `UNCLAIMED`: click, then wait 10s for `CLAIMED`
  - otherwise: throw
  - remove the toast wait, the swallowed click error and its `shortcut:` comment

  Verify `./gradlew build` passes and `grep -n "Sign In Success" src/main` finds nothing

## 2. Feed end

- [x] 2.1 Make `waitForMorePosts()` return whether the post count grew, and break the like loop when it did not. Verify `./gradlew build` passes, and by reading the code that every path through the `while` loop either increments `currentIndex` or exits

## 3. Manual check on home server

- [x] 3.1 Run once today with `runOnStartup: true` (the reward is already claimed). Verify the log shows "Already claimed today", the run succeeds, and the like step ends
- [ ] 3.2 Let tomorrow's 07:30 run happen. Verify the log shows the reward claimed via the icon switch, and the mission page shows the claimed icon

## Workflow follow-up

- Archive the change, then merge `fix/blablalink-signin-detection` into `develop` with `--no-ff` per `CONTRIBUTING.md`.
