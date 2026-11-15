# Proposal

## Why

`SchedulerManager` computes the delay to the first `dailyTime` once, then repeats every 24 hours with `scheduleAtFixedRate`. When a job's timezone shifts its clocks for daylight saving, the run moves one hour off its configured local time and stays there. A long run or a clock adjustment can push it off too. This is backlog item #22 in the baseline design. The default zone `Asia/Jakarta` has no daylight saving, so this only affects jobs with another `timezone`.

## What Changes

- Each run is scheduled once. When the run ends, the next run is computed from the wall clock as the next `dailyTime` in the job's zone, and scheduled again.
- The next run is always strictly after the run that just happened. A timer that fires slightly early can therefore not run the job twice on one day.
- `stop()` cancels each job's pending run and schedules no new ones.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. `job-scheduling` "Daily run at local time" already has the "Daylight saving shift" scenario. The code does not meet it yet, so this change sets `skip_specs: true`.

## Impact

- Code: `SchedulerManager.kt` (self-rescheduling runs, a pure `nextRun` function).
- Tests: `nextRun` unit tests for a time later today, a time already passed, and a daylight saving change.
