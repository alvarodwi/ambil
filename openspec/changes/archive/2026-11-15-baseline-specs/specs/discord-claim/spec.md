# Spec Delta

## Purpose

Claims daily rewards from Discord bots by replaying, as the user's own account, the button-click interaction the user captured from the Discord web client.

## ADDED Requirements

### Requirement: Discord job configuration
A `discord` job config SHALL contain `userToken` and a non-empty `interactions` list. Each interaction SHALL contain `guild_id`, `channel_id`, `message_id`, `application_id`, `session_id` and `custom_id`, taken from the captured request body, and an optional `component_type` that defaults to `2` (button).

#### Scenario: Interaction from captured request
- **WHEN** the user copies the fields from a captured `POST /api/v9/interactions` body into an interaction entry
- **THEN** the job can replay that click without further input

### Requirement: Replay button interaction
For each configured interaction, the system SHALL send a component interaction to Discord as the user, authenticated with `userToken`, carrying the configured IDs and a freshly generated nonce, in the same shape the Discord client sends.

#### Scenario: Click accepted
- **WHEN** Discord answers the interaction with HTTP 200 or 204
- **THEN** that interaction counts as claimed

### Requirement: Look like the Discord client
Requests SHALL carry the browser identity headers the Discord web client sends (user agent, super-properties, locale, origin, referer), and those values SHALL be consistent with each other and with a current client build.

#### Scenario: Consistent identity
- **WHEN** an interaction request is sent
- **THEN** its user agent, super-properties browser fields and locale describe the same browser

### Requirement: Interactions run in order with spacing
The system SHALL send configured interactions one at a time in config order, waiting a short delay between consecutive interactions to stay under rate limits.

#### Scenario: Two interactions
- **WHEN** a job has two interactions
- **THEN** the second is sent only after the first completes and a delay has passed

### Requirement: Interaction failure reporting
Any non-2xx response, including HTTP 429, or a network error SHALL mark that interaction as failed, recording the status and response body. Other interactions in the same run SHALL still be attempted.

#### Scenario: Rate limited
- **WHEN** Discord answers the first interaction with HTTP 429
- **THEN** that interaction fails with the response body recorded, and the second interaction is still sent

### Requirement: Job outcome
A Discord job run SHALL succeed only when every interaction succeeds. A failed run's message SHALL identify each failed interaction and its error.

#### Scenario: One of two fails
- **WHEN** one interaction succeeds and the other returns HTTP 401
- **THEN** the run fails and its message names the failing interaction and the 401
