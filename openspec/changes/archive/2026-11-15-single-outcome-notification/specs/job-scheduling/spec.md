# Spec Delta

## ADDED Requirements

### Requirement: No retry when user action is needed
When a run fails because only the user can fix the cause, such as an expired blablalink session, the system SHALL stop that run without further attempts and report it as failed.

#### Scenario: Session expired on first attempt
- **WHEN** a blablalink job with `maxAttempts: 5` detects an expired session on its first attempt
- **THEN** the system makes no further attempts and reports the run as failed

#### Scenario: Ordinary failure still retried
- **WHEN** a job attempt fails for any other reason
- **THEN** the system retries it as the retry policy says
