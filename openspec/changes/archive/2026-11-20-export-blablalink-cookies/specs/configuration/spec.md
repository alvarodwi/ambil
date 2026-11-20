# Spec Delta

## MODIFIED Requirements

### Requirement: Top-level settings
The configuration SHALL contain a `telegram` object with `token` and `chatId`, an optional `headless` boolean that defaults to `true`, an optional `cookieExportPath` string (no default, no export when absent), and a `jobs` list.

#### Scenario: Headless omitted
- **WHEN** the config has no `headless` field
- **THEN** browser-based jobs run headless

#### Scenario: Telegram missing
- **WHEN** the config has no `telegram` object
- **THEN** the system exits at startup with a parse error

#### Scenario: Cookie export path omitted
- **WHEN** the config has no `cookieExportPath` field
- **THEN** the config loads and no cookie file is written
