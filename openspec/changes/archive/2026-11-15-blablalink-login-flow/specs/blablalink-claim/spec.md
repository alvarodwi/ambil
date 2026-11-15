# Spec Delta

## MODIFIED Requirements

### Requirement: Manual login only
The system SHALL NOT log in automatically or store blablalink credentials. Login SHALL happen by the user in a visible browser window, solving any captcha themselves.

#### Scenario: No credentials needed
- **WHEN** a `blablalink` job config has only `"type": "blablalink"`
- **THEN** the job loads and runs using the saved browser session

#### Scenario: Legacy credentials ignored
- **WHEN** a `blablalink` job config still contains `username` and `password`
- **THEN** the config loads, and the values are never used or sent anywhere

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
- **WHEN** two enabled blablalink jobs exist and the session is logged out
- **THEN** only one login window opens at startup

#### Scenario: No blablalink job enabled
- **WHEN** every blablalink job is disabled
- **THEN** no browser opens at startup

### Requirement: Expired session detection
At the start of each run the system SHALL run the session state check. If it reports logged out, the run SHALL fail as "session expired", and the user SHALL be told to restart ambil where a display is available and log in in the window that opens.

#### Scenario: Session expired
- **WHEN** the session state check reports logged out at the start of a run
- **THEN** the run fails with a session-expired result, and a notification tells the user to restart ambil and log in

## ADDED Requirements

### Requirement: Session state check
The system SHALL decide login state from the mission page without claiming anything: wait up to 15 seconds for the mission list, then treat a visible "Log In" entry as logged out and its absence as logged in. If the mission list does not appear, the state SHALL be reported as unknown (an error), never as logged in.

#### Scenario: Logged-out profile
- **WHEN** the mission page shows the "Log In" entry
- **THEN** the session is logged out

#### Scenario: Logged-in profile
- **WHEN** the mission page shows the mission list and no "Log In" entry
- **THEN** the session is logged in, and no reward is claimed by the check

#### Scenario: Page does not load
- **WHEN** the mission list does not appear within 15 seconds
- **THEN** the check fails with an error instead of reporting logged in

### Requirement: Browser security protections stay on
The browser SHALL run with Chrome's sandbox enabled and SHALL NOT accept DevTools connections from arbitrary origins. The persistent profile directory SHALL be readable and writable only by its owner.

#### Scenario: Browser launch
- **WHEN** the system launches Chrome for a run or the login flow
- **THEN** it does not pass `--no-sandbox` or `--remote-allow-origins=*`

#### Scenario: Profile permissions
- **WHEN** the system launches Chrome
- **THEN** the profile directory exists with owner-only permissions (`0700`)
