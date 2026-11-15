# Proposal

## Why

`DiscordClient` claims to be Chrome 124 with client build `290510`. Both are about two years old. The user agent was also typed out twice, once in the `User-Agent` header and once in the super-properties, so the two could drift apart. `discord-claim` "Look like the Discord client" requires identity headers that agree with each other and with a current client build. This is backlog item #23 in the baseline design.

## What Changes

- One browser identity, defined once and used by every header: Firefox 149 on Windows 10, client build `520959`, locale `en-US`. These values come from a Discord web client request the owner captured.
- The super-properties gain `has_client_mods: false`, which the captured client sends.
- The identity is still hardcoded. A code comment marks it as a shortcut to re-capture when Discord starts rejecting clicks.
- Not included: a signal for an expired `session_id`. Nobody has decided what Discord's reply to a stale session looks like or what ambil should do about it, so the specs leave it out.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. `discord-claim` "Look like the Discord client" already requires this. The code does not meet it yet, so this change sets `skip_specs: true`.

## Impact

- Code: `DiscordClient.kt`.
- Tests: a unit test that the super-properties' user agent, browser, version and locale match the headers.
