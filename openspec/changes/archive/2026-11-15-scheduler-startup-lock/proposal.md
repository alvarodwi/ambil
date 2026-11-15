# Proposal

## Why

A `runOnStartup` run is started from `Main.kt` on its own thread, outside `SchedulerManager`. It never takes the per-job overlap lock. If a startup run is still going when the job's `dailyTime` arrives, the scheduled run starts anyway. Two runs of the same job then drive the same Discord account, or the same Chrome profile, at once. This is backlog item #20 in the baseline design.

## What Changes

- `SchedulerManager` gains `runNow(name)`, which starts one run of a registered job on its own thread, through the same overlap lock as scheduled runs.
- `Main.kt` starts startup runs with `runNow` instead of its own thread.
- A scheduled run that arrives while the startup run is still going is skipped and logged, like any overlapping run.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. `job-scheduling` "No overlapping runs" already covers startup runs, with the "Startup run still going at daily time" scenario. The code does not meet it yet, so this change sets `skip_specs: true`.

## Impact

- Code: `SchedulerManager.kt` (`runNow`), `Main.kt` (startup runs).
- Tests: a unit test that a second run of a job is skipped while the first is still going.
