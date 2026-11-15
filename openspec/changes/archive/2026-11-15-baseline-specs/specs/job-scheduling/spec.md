# Spec Delta

## Purpose

Defines when claim jobs run: once a day at a configured local time, optionally once at startup, with retries on failure and no overlapping runs of the same job.

## ADDED Requirements

### Requirement: Only enabled jobs run
The system SHALL schedule and run only jobs whose `enabled` flag is `true`. Disabled jobs SHALL NOT be validated beyond parsing, scheduled, or initialised.

#### Scenario: Disabled job
- **WHEN** a job has `enabled: false`
- **THEN** it never runs and no browser or network session is opened for it

### Requirement: Daily run at local time
The system SHALL run each enabled job once per day at its `dailyTime` in its `timezone`, staying aligned to that wall-clock time across days.

#### Scenario: Time later today
- **WHEN** the app starts at 07:00 Asia/Jakarta and a job is set to `08:00` Asia/Jakarta
- **THEN** the job first runs at 08:00 that day and then at 08:00 each following day

#### Scenario: Time already passed
- **WHEN** the app starts at 09:00 and a job is set to `08:00` in the same zone
- **THEN** the job first runs at 08:00 the next day

#### Scenario: Daylight saving shift
- **WHEN** a job's timezone moves its clocks by one hour
- **THEN** the job still runs at its configured local `dailyTime`

### Requirement: Run on startup
The system SHALL run a job once immediately after startup when its `runOnStartup` flag is `true`, in addition to its daily schedule.

#### Scenario: Startup run
- **WHEN** the app starts and a job has `runOnStartup: true`
- **THEN** the job runs once right away and still runs at its next `dailyTime`

### Requirement: No overlapping runs
The system SHALL NOT run a job while a previous run of the same job is still in progress, whether that run was scheduled or triggered on startup. A run that would overlap SHALL be skipped and logged.

#### Scenario: Startup run still going at daily time
- **WHEN** a startup run of a job is still in progress when its `dailyTime` arrives
- **THEN** the scheduled run is skipped with a log entry

### Requirement: Jobs are independent
A failure or crash in one job SHALL NOT stop other jobs or the scheduler.

#### Scenario: One job throws
- **WHEN** the Discord job throws an unexpected error
- **THEN** the error is logged and reported, and the blablalink job still runs at its time

### Requirement: Retry with exponential backoff
When a job run fails, the system SHALL retry it up to `retry.maxAttempts` total attempts. The wait before the first retry SHALL be `initialDelaySeconds`; each later wait SHALL be the previous one times `multiplier`, capped at `maxDelaySeconds`.

#### Scenario: Succeeds on retry
- **WHEN** a job with `maxAttempts: 3`, `initialDelaySeconds: 10`, `multiplier: 2` fails once then succeeds
- **THEN** the system waits 10 seconds, runs it again, and reports success

#### Scenario: All attempts fail
- **WHEN** every attempt of a job with `maxAttempts: 3` fails
- **THEN** the system makes exactly 3 attempts, waiting 10 then 20 seconds, and reports the job as failed

#### Scenario: Delay capped
- **WHEN** the computed wait exceeds `maxDelaySeconds`
- **THEN** the system waits `maxDelaySeconds` instead

### Requirement: Valid retry policy
The system SHALL reject at startup an enabled job whose retry policy has `maxAttempts` below 1, a negative delay, or a `multiplier` below 1.

#### Scenario: Zero attempts
- **WHEN** an enabled job has `maxAttempts: 0`
- **THEN** the system exits at startup with an error naming the job

### Requirement: Long-running process with graceful shutdown
The system SHALL keep running until stopped, and on a termination signal SHALL stop scheduling new runs and wait a bounded time for running jobs before exiting.

#### Scenario: SIGTERM
- **WHEN** the process receives SIGTERM while idle
- **THEN** it logs the shutdown and exits cleanly
