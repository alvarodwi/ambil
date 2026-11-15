# configuration Specification

## Purpose
Defines how ambil finds, parses and validates its single JSON configuration file, which declares the notifier, browser mode and every claim job.

## Requirements

### Requirement: Config file location
The system SHALL read its configuration from a file named `config.json` located next to the running application jar, and SHALL fall back to `config.json` in the current working directory when no file exists next to the jar.

#### Scenario: Config next to jar
- **WHEN** `config.json` exists in the same directory as the jar
- **THEN** the system loads that file, even if the working directory also has one

#### Scenario: Config in working directory
- **WHEN** no `config.json` exists next to the jar and one exists in the working directory
- **THEN** the system loads the working-directory file

#### Scenario: Config missing
- **WHEN** no `config.json` exists in either location
- **THEN** the system exits at startup with an error naming the path it looked for

### Requirement: Top-level settings
The configuration SHALL contain a `telegram` object with `token` and `chatId`, an optional `headless` boolean that defaults to `true`, and a `jobs` list.

#### Scenario: Headless omitted
- **WHEN** the config has no `headless` field
- **THEN** browser-based jobs run headless

#### Scenario: Telegram missing
- **WHEN** the config has no `telegram` object
- **THEN** the system exits at startup with a parse error

### Requirement: Job entry fields
Each job entry SHALL have a `name`, an optional `enabled` flag (default `true`), an optional `runOnStartup` flag (default `false`), a `schedule` with `dailyTime` in 24-hour `HH:mm` and optional `timezone` (IANA ID, default `Asia/Jakarta`), a `retry` policy, and a typed `config`.

#### Scenario: Defaults applied
- **WHEN** a job entry omits `enabled`, `runOnStartup` and `schedule.timezone`
- **THEN** the job is enabled, does not run on startup, and is scheduled in `Asia/Jakarta`

### Requirement: Job names are unique and non-blank
The system SHALL reject a configuration where any enabled job has a blank name or shares its name with another enabled job.

#### Scenario: Duplicate name
- **WHEN** two enabled jobs are both named `discord`
- **THEN** the system exits at startup with an error naming the duplicate

#### Scenario: Blank name
- **WHEN** an enabled job has `name: ""`
- **THEN** the system exits at startup with an error

### Requirement: Schedule values validated at startup
The system SHALL reject an enabled job whose `dailyTime` is not a valid time or whose `timezone` is not a valid zone ID, before any job runs.

#### Scenario: Invalid timezone
- **WHEN** an enabled job has `timezone: "Mars/Olympus"`
- **THEN** the system exits at startup with an error

### Requirement: Job type discriminator
The job `config` object SHALL carry a `type` field whose value selects the job kind: `discord` or `blablalink`. Fields for each kind are defined by the `discord-claim` and `blablalink-claim` capabilities.

#### Scenario: Discord job
- **WHEN** a job config has `"type": "discord"`
- **THEN** the system creates a Discord claim job from it

#### Scenario: Unknown type
- **WHEN** a job config has `"type": "steam"`
- **THEN** the system exits at startup with a parse error

### Requirement: Unknown keys ignored
The system SHALL ignore keys in the configuration that it does not recognise.

#### Scenario: Extra key
- **WHEN** a job entry contains an unrecognised key `note`
- **THEN** the config loads and the key has no effect

### Requirement: Secrets stay out of version control
The repository SHALL NOT track `config.json`. It SHALL provide an example configuration that documents every field.

#### Scenario: Fresh clone
- **WHEN** a user clones the repository
- **THEN** no real tokens or passwords are present, and an example config shows the expected shape
