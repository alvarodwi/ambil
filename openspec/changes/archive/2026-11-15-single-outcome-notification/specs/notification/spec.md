# Spec Delta

## MODIFIED Requirements

### Requirement: One outcome message per job run
After a job run finishes, including all retries, the system SHALL send exactly one outcome message. It SHALL state the job name, whether it succeeded, failed, or crashed, and the job's result message or error.

#### Scenario: Success
- **WHEN** the Discord job succeeds
- **THEN** one success message is sent naming the job and its result

#### Scenario: Failure after retries
- **WHEN** a blablalink job fails on all 5 attempts
- **THEN** one failure message is sent for that run, not one per attempt or per component

#### Scenario: Failure carries the last error
- **WHEN** every attempt of a job fails and the last attempt fails with "HTTP 401"
- **THEN** the failure message includes the attempt count and "HTTP 401"

### Requirement: Action-needed alert
When a run cannot succeed without the user, such as an expired blablalink session, the system SHALL send an alert saying what the user must do. It SHALL send this at most once per job run. The alert SHALL be the run's outcome message, not an extra message.

#### Scenario: Session expired alert
- **WHEN** a blablalink run detects an expired session
- **THEN** the user gets one message telling them to log in manually in a visible browser

#### Scenario: No second message
- **WHEN** a blablalink run fails because the session expired
- **THEN** the run sends exactly one Telegram message in total
