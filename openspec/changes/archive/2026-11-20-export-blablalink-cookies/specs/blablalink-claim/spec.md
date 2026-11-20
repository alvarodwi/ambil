# Spec Delta

## ADDED Requirements

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
