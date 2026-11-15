# Proposal

## Why

The blablalink run reports "already signed in" whenever the success toast doesn't show up within 5 seconds. A changed selector, a slow page or a failed click all look like success, so a missed reward goes unnoticed. Separately, when the Outpost feed runs out before 5 likes, the like loop keeps scrolling forever. The run never ends, and the overlap lock then skips every later daily run. These are backlog items #15 and #29 in the baseline design.

## What Changes

- Read the Daily Sign-in row's state from the page before clicking: claimed (`icon-gift-true`) or unclaimed (`icon-gift`).
- Claimed means "already claimed" with no click. Unclaimed means click, then wait for the row to switch to claimed. A row that is missing or never switches fails the sign-in step.
- Stop the like loop when a scroll loads no new posts, and report how many posts were liked.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. `blablalink-claim` already requires both behaviours (Claim daily sign-in, including its "Page changed" scenario, and Like and read posts, including "Feed runs out"). The code does not meet them yet, so this change sets `skip_specs: true`.

## Impact

- Code: `BlablalinkClient.kt` (`loginAndClaimDailyReward`, `browseAndLike`, `waitForMorePosts`).
- Tests: a new pure state-mapping function with unit tests.
- Behaviour: a broken sign-in page now fails the run and alerts through the existing failure path, instead of reporting success.
