# Proposal

## Why

ambil automates claiming daily rewards for a gacha game (NIKKE) from two sources: the blablalink community website and Discord bot buttons. It has run for months with no written contract of what it should do, so fixes risk changing behaviour nobody wrote down. This change records the intended behaviour as baseline specs before the known inconsistencies are fixed one change at a time.

## What Changes

- Add baseline specs for the five behaviours ambil already provides: configuration loading, daily job scheduling with retry, Discord button claims, blablalink daily claims, and Telegram notifications.
- Specs describe **intended** behaviour, never current bugs. Where the code does not yet meet a requirement, `design.md` lists the gap as a known deviation for a follow-up change. Behaviour nobody has decided on yet (for example, whether an expired session is retried) is left out of the specs.
- No code, script, or config changes.

## Capabilities

### New Capabilities
- `configuration`: loading and validating `config.json`, its job list, and per-job typed settings.
- `job-scheduling`: running each enabled job daily at a local time, optionally on startup, with retry and backoff, without overlapping runs.
- `discord-claim`: claiming rewards by replaying the button-click interaction a user captured from the Discord client.
- `blablalink-claim`: claiming the blablalink daily sign-in and completing the like/read post missions in a persistent, manually logged-in browser session.
- `notification`: reporting job outcomes and session problems to the user via Telegram.

### Modified Capabilities
- None (no specs exist yet).

## Impact

- New files under `openspec/specs/` after archive. No runtime impact.
- Follow-up changes (see `design.md` - Known deviations) will reference these specs.
