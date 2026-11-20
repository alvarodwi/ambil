# blablalink-claim Specification

## Purpose
Claims the blablalink (NIKKE community site) daily sign-in reward and completes the daily like and read-post missions, using a real browser session the user logged into by hand.

## Requirements

### Requirement: Persistent browser session
The system SHALL drive a Chrome browser using a persistent profile stored under the user's home directory, so a login survives between runs and restarts.

#### Scenario: Next day run
- **WHEN** the user logged in once and the session has not expired
- **THEN** the next run is already logged in without user action

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
- **WHEN** two enabled blablalink jobs exist
- **THEN** startup stops before any login window opens, as "Single blablalink job" requires

#### Scenario: No blablalink job enabled
- **WHEN** every blablalink job is disabled
- **THEN** no browser opens at startup

### Requirement: Expired session detection
At the start of each run the system SHALL run the session state check. If it reports logged out, the run SHALL fail as "session expired", and the user SHALL be told to restart ambil where a display is available and log in in the window that opens.

#### Scenario: Session expired
- **WHEN** the session state check reports logged out at the start of a run
- **THEN** the run fails with a session-expired result, and a notification tells the user to restart ambil and log in

### Requirement: Claim daily sign-in
The system SHALL open the mission page, dismiss the cookie banner if shown, and click the Daily Sign-in claim button. It SHALL report "claimed" only when the site confirms success, and "already claimed" only when the site shows the reward was already taken.

#### Scenario: Reward available
- **WHEN** the daily sign-in is unclaimed
- **THEN** the system clicks it, sees the success confirmation, and reports it claimed

#### Scenario: Already claimed today
- **WHEN** the daily sign-in was already claimed today
- **THEN** the system reports it already claimed and the run still succeeds

#### Scenario: Page changed
- **WHEN** neither the claim button nor an already-claimed state can be found
- **THEN** the sign-in step fails instead of reporting success

### Requirement: Like and read posts
After the sign-in step, the system SHALL go through the Outpost feed and like up to 5 posts that are not already liked, opening each liked post's detail page to count as read. It SHALL NOT click the like button of a post the site already shows as liked. A post SHALL count only when the site shows it liked after the click and its detail page opened. It SHALL scroll to load more posts when needed and skip posts it cannot interact with.

#### Scenario: Five new likes
- **WHEN** the feed has at least 5 unliked posts
- **THEN** the system likes 5 posts, opens each, and reports "Liked 5 posts"

#### Scenario: Feed runs out
- **WHEN** no more posts load after scrolling
- **THEN** the system stops and reports how many posts it liked

#### Scenario: Post already liked
- **WHEN** a feed post already shows as liked
- **THEN** the system does not click its like button, the post stays liked, and it does not count toward the total

#### Scenario: Like does not register
- **WHEN** the system clicks like on a post and the site does not show it as liked within the wait
- **THEN** the post does not count, and the system moves on to the next post

#### Scenario: Detail page does not open
- **WHEN** a post is liked but its detail page does not open
- **THEN** the post does not count toward "Liked N posts", and the run continues with the next post

#### Scenario: Headless and visible runs
- **WHEN** a run uses headless mode, or a visible window on a desktop-size screen
- **THEN** the like step finds, likes and opens Outpost posts in both cases

### Requirement: Run summary
A successful blablalink run SHALL report both the sign-in outcome and the number of posts liked.

#### Scenario: Summary message
- **WHEN** a run claims the reward and likes 5 posts
- **THEN** the job message includes the sign-in result and "Liked 5 posts"

### Requirement: Headless setting
Scheduled blablalink runs SHALL use headless mode when the top-level `headless` setting is `true`, and a visible window when it is `false`. The manual login flow SHALL always use a visible window.

#### Scenario: Headless scheduled run
- **WHEN** `headless` is `true` and a scheduled run starts
- **THEN** no browser window is shown

### Requirement: Browser always closed
The system SHALL close the browser at the end of every run and login flow, whether it succeeded or failed.

#### Scenario: Run crashes
- **WHEN** a run throws partway through
- **THEN** the browser process is closed before the result is reported

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

### Requirement: Single blablalink job
The system SHALL support one blablalink account, held in the one persistent browser profile. It SHALL reject at startup a configuration with more than one enabled blablalink job, with an error naming those jobs, before any browser opens or any job runs. Disabled blablalink jobs SHALL NOT count.

#### Scenario: Two enabled blablalink jobs
- **WHEN** jobs `blablalink` and `blablalink-alt` are both enabled blablalink jobs
- **THEN** the system exits at startup with an error naming both jobs, and no browser opens

#### Scenario: Second blablalink job disabled
- **WHEN** one blablalink job is enabled and another has `enabled: false`
- **THEN** the config loads and the enabled job runs

### Requirement: Cookie export
When `cookieExportPath` is configured, the system SHALL write the browser's `game_*` cookies to that file as one `name=value; name=value` line every time the session check finds the browser logged in. The file SHALL be replaced atomically and be readable only by its owner. A cookie set without both `game_token` and `game_openid` SHALL NOT be written. An export failure SHALL be logged as a warning and SHALL NOT fail the session check, the run or startup.

#### Scenario: Logged-in check exports cookies
- **WHEN** `cookieExportPath` is set and the session check finds the browser logged in
- **THEN** the file holds the current `game_*` cookies as one Cookie header line, including `game_token` and `game_openid`

#### Scenario: Export not configured
- **WHEN** `cookieExportPath` is absent
- **THEN** no file is written and behaviour is unchanged

#### Scenario: Required cookie missing
- **WHEN** the browser is logged in but has no `game_token` or no `game_openid`
- **THEN** the existing file is left untouched and a warning is logged

#### Scenario: Export fails
- **WHEN** writing the file fails
- **THEN** a warning is logged and the run continues as if the export had not been configured
