# Spec Delta

## Purpose

Tells the user through a Telegram bot how each job run ended and when manual action, such as a blablalink re-login, is needed.

## ADDED Requirements

### Requirement: Telegram delivery
The system SHALL send notifications as messages from the bot identified by `telegram.token` to the chat `telegram.chatId`.

#### Scenario: Message sent
- **WHEN** a notification is raised
- **THEN** the configured chat receives it from the configured bot

### Requirement: One outcome message per job run
After a job run finishes, including all retries, the system SHALL send exactly one outcome message. It SHALL state the job name, whether it succeeded, failed, or crashed, and the job's result message or error.

#### Scenario: Success
- **WHEN** the Discord job succeeds
- **THEN** one success message is sent naming the job and its result

#### Scenario: Failure after retries
- **WHEN** a blablalink job fails on all 5 attempts
- **THEN** one failure message is sent for that run, not one per attempt or per component

### Requirement: Action-needed alert
When a run cannot succeed without the user, such as an expired blablalink session, the system SHALL send an alert saying what the user must do. It SHALL send this at most once per job run.

#### Scenario: Session expired alert
- **WHEN** a blablalink run detects an expired session
- **THEN** the user gets one message telling them to log in manually in a visible browser

### Requirement: Message text preserved
Message text SHALL arrive as written, with characters that Telegram formatting treats as special escaped so they show literally.

#### Scenario: Special characters
- **WHEN** an error message contains `_`, `*`, `.` or `(`
- **THEN** the Telegram message shows those characters as-is

### Requirement: Notification failure does not fail jobs
If sending a notification fails, the system SHALL log the failure and SHALL NOT change the job's result or stop the scheduler.

#### Scenario: Telegram unreachable
- **WHEN** the Telegram API cannot be reached
- **THEN** the failure is logged and the job's outcome stays as it was
