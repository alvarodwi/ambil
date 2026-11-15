# Proposal

## Why

The Telegram outcome message hides what happened. A successful blablalink run says only "Sign-in success": it drops whether the reward was claimed or already taken, and how many posts were liked. A failed Discord run says only "Some interactions failed", without saying which reward failed or why. This is backlog item #19 in the baseline design. The live `config.json` already gives each Discord interaction a `name` (`nikke daily`, `reverse1999 daily`), but the code ignores it.

## What Changes

- A successful blablalink run reports the sign-in outcome and the like count, for example "Already claimed today. Liked 5 posts."
- Discord interactions accept an optional `name`.
- A Discord run reports each interaction by its `name`, or by its position ("interaction 2") when it has no name. A failed run lists each failed interaction with its error, and also lists the ones that succeeded.
- Not changed: failure text for blablalink, which already carries the cause.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `discord-claim`: "Discord job configuration" gains the optional `name`. "Job outcome" says how interactions are named in the message, and that a success message lists the claimed interactions.
- `blablalink-claim`: none. "Run summary" already requires the sign-in result and like count. The code does not meet it yet.

## Impact

- Code: `DiscordInteractionConfig.kt`, `DiscordClient.kt` (`clickAllButtons`), `DiscordJob.kt`, `BlablalinkJob.kt`, `config.json.example`.
- Tests: unit tests for the Discord outcome text and for parsing `name`.
- Config: backward compatible. Configs without `name` still load.
- Depends on `single-outcome-notification` landing first, so each run's text reaches Telegram once.
