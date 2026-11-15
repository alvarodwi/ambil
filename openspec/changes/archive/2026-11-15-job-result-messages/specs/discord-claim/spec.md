# Spec Delta

## MODIFIED Requirements

### Requirement: Discord job configuration
A `discord` job config SHALL contain `userToken` and a non-empty `interactions` list. Each interaction SHALL contain `guild_id`, `channel_id`, `message_id`, `application_id`, `session_id` and `custom_id`, taken from the captured request body, an optional `component_type` that defaults to `2` (button), and an optional `name` that labels the interaction in messages.

#### Scenario: Interaction from captured request
- **WHEN** the user copies the fields from a captured `POST /api/v9/interactions` body into an interaction entry
- **THEN** the job can replay that click without further input

#### Scenario: Named interaction
- **WHEN** an interaction entry has `"name": "nikke daily"`
- **THEN** the config loads, and messages about that interaction call it "nikke daily"

#### Scenario: Unnamed interaction
- **WHEN** an interaction entry has no `name`
- **THEN** the config loads, and messages call it by its 1-based position, for example "interaction 2"

### Requirement: Job outcome
A Discord job run SHALL succeed only when every interaction succeeds. A successful run's message SHALL list the claimed interactions. A failed run's message SHALL identify each failed interaction and its error, and SHALL list the interactions that succeeded.

#### Scenario: One of two fails
- **WHEN** one interaction succeeds and the other returns HTTP 401
- **THEN** the run fails and its message names the failing interaction and the 401

#### Scenario: All succeed
- **WHEN** interactions "nikke daily" and "reverse1999 daily" both succeed
- **THEN** the run succeeds and its message names both
