## ADDED Requirements

### Requirement: Single blablalink job
The system SHALL support one blablalink account, held in the one persistent browser profile. It SHALL reject at startup a configuration with more than one enabled blablalink job, with an error naming those jobs, before any browser opens or any job runs. Disabled blablalink jobs SHALL NOT count.

#### Scenario: Two enabled blablalink jobs
- **WHEN** jobs `blablalink` and `blablalink-alt` are both enabled blablalink jobs
- **THEN** the system exits at startup with an error naming both jobs, and no browser opens

#### Scenario: Second blablalink job disabled
- **WHEN** one blablalink job is enabled and another has `enabled: false`
- **THEN** the config loads and the enabled job runs

## MODIFIED Requirements

### Requirement: Manual login flow
At startup, when at least one enabled blablalink job exists, the system SHALL check the saved session once. Only if it is logged out SHALL it open a visible browser at the mission page, start login from its "Log In" entry, and wait up to 10 minutes for the user to log in. Success SHALL be confirmed by a fresh session check. A failed or timed-out login SHALL be logged and SHALL NOT stop startup.

#### Scenario: Already logged in at startup
- **WHEN** the app starts and the saved session is logged in
- **THEN** no login window opens and startup continues

#### Scenario: User completes login
- **WHEN** the login flow runs and the user finishes logging in within 10 minutes
- **THEN** the session is saved and later runs use it

#### Scenario: Login popup closed without logging in
- **WHEN** the user closes the login popup without logging in
- **THEN** the follow-up session check reports logged out, and the failure is logged, not reported as success

#### Scenario: Login times out
- **WHEN** the login window opens and nobody logs in within 10 minutes
- **THEN** the window closes, the timeout is logged, the scheduler starts, and other jobs run on schedule

#### Scenario: Several blablalink jobs
- **WHEN** two enabled blablalink jobs exist
- **THEN** startup stops before any login window opens, as "Single blablalink job" requires

#### Scenario: No blablalink job enabled
- **WHEN** every blablalink job is disabled
- **THEN** no browser opens at startup
