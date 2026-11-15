# Spec Delta

## Purpose

Claims the blablalink (NIKKE community site) daily sign-in reward and completes the daily like and read-post missions, using a real browser session the user logged into by hand.

## ADDED Requirements

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

### Requirement: Manual login flow
The system SHALL offer a login flow that opens a visible browser at the blablalink login page and waits up to 10 minutes for the user to finish logging in, then saves the session to the profile. It SHALL run only when the saved session is not logged in.

#### Scenario: Already logged in at startup
- **WHEN** the app starts and the saved session is logged in
- **THEN** no login window opens and startup continues

#### Scenario: User completes login
- **WHEN** the login flow runs and the user finishes logging in within 10 minutes
- **THEN** the session is saved and later runs use it

### Requirement: Expired session detection
At the start of each run the system SHALL check whether the session is logged in. If blablalink redirects to its login page, the run SHALL fail as "session expired" and the user SHALL be told to log in manually.

#### Scenario: Session expired
- **WHEN** opening the mission page redirects to the login page
- **THEN** the run fails with a session-expired result and a notification asks the user to log in manually

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
After the sign-in step, the system SHALL go through the Outpost feed and like up to 5 posts that are not already liked, opening each liked post's detail page to count as read. It SHALL scroll to load more posts when needed and skip posts it cannot interact with.

#### Scenario: Five new likes
- **WHEN** the feed has at least 5 unliked posts
- **THEN** the system likes 5 posts, opens each, and reports "Liked 5 posts"

#### Scenario: Feed runs out
- **WHEN** no more posts load after scrolling
- **THEN** the system stops and reports how many posts it liked

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
