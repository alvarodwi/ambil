# Design

## Context

This change only writes specs. The code already exists and is the reference for what ambil does. See proposal.md - Why.

Runtime shape today:

```
config.json --> ConfigLoader --> AppConfig.jobs[] (type: discord | blablalink)
                                     |
             Main.createTask --------+------------------------+
                  |                                           |
          DiscordClient (OkHttp)                  BlablalinkClient (Selenium)
          POST /api/v9/interactions               ~/.blablalink-profile (Chrome)
          once per interaction                    mission sign-in + like 5 posts
                  |                                           |
                  +--------> RetryExecutor --> TelegramNotifier
                                     ^
          SchedulerManager (24h fixed rate)  +  runOnStartup Thread
```

## Goals / Non-Goals

**Goals:**
- Give every follow-up fix a requirement to point at.
- Record each known gap between code and intent in one list, so it can become its own change.

**Non-Goals:**
- Fixing any gap in this change.
- Specifying repo tooling (`run.sh`, `start.sh`, `stop.sh`, `CONTRIBUTING.md`). Those are not runtime behaviour; their fixes are `chore/` changes without spec deltas.

## Decisions

- **Intended behaviour, not as-is.** Specs state what ambil should do. A requirement the code does not meet yet is listed below rather than weakened. Alternative: spec the current behaviour and change it later. Rejected, because that writes bugs such as triple notifications into the contract.
- **Leave undecided behaviour out.** For example, whether an expired session should be retried, or whether one blablalink profile per account is needed. These stay out of the specs until a change decides them.
- **Manual login is the only blablalink login.** The code disables automatic login and relies on the persistent profile. `username`/`password` are therefore not part of the intended config.
- **Five flat capabilities.** One per source (`discord-claim`, `blablalink-claim`), plus `configuration`, `job-scheduling` and `notification`. A new reward source becomes a new `<source>-claim` capability.

## Known deviations

Each row is a follow-up change. Numbers match the explore-session scan.

| # | Gap | Spec / area | Suggested change |
|---|-----|-------------|------------------|
| 3, 4 | `CONTRIBUTING.md` copied from "scraver": wrong config name, jar name, tag message; initial commit on `main` conflicts with "every main commit is a release" | tooling | `chore/fix-contributing` |
| 5, 6 | `run.sh` passes Spring args, needs `./config`, writes `gachatracker.pid` while `start.sh`/`stop.sh` use `ambil.pid`; log written twice | tooling | `chore/fix-run-scripts` |
| 7 | Error message names `config.example.json`, file is `config.json.example` | configuration: Config file location | `fix/config-example` |
| 8-11 | Example config is not valid JSON, lacks `telegram`, lacks `type`, uses blank `name` | configuration: Secrets stay out of version control | `fix/config-example` |
| 12 | `maxAttempts: 0` is accepted and makes the job never run | job-scheduling: Valid retry policy | `fix/validate-retry-policy` |
| 13 | `BlablalinkConfig` still requires unused `username`/`password` | blablalink-claim: Manual login only | `fix/blablalink-login-flow` |
| 14 | Login flow runs on every startup for every blablalink job, crashes startup if already logged in, ignores headless rule | blablalink-claim: Manual login flow | `fix/blablalink-login-flow` |
| 15 | Missing success toast is reported as "already signed in" | blablalink-claim: Claim daily sign-in | `fix/blablalink-signin-detection` |
| 16 | Dead redirect/`login()` branch in sign-in step | blablalink-claim (cleanup) | `fix/blablalink-login-flow` |
| 17 | One failure sends up to 3 Telegram messages (client, retry executor, main) | notification: One outcome message per job run | `fix/single-outcome-notification` |
| 18 | Expired session is retried and re-alerted each attempt | notification: Action-needed alert | `fix/single-outcome-notification` |
| 19 | Blablalink result drops sign-in/likes text; Discord result does not say which interaction failed | blablalink-claim: Run summary; discord-claim: Job outcome | `fix/job-result-messages` |
| 20 | Startup run bypasses overlap lock | job-scheduling: No overlapping runs | `fix/scheduler-startup-lock` |
| 21 | All blablalink jobs share one profile | undecided, not specced | decide in its own change |
| 22 | 24h fixed-rate schedule drifts on DST | job-scheduling: Daily run at local time | `fix/scheduler-wall-clock` |
| 23 | Hardcoded Chrome 124 UA, old build number, `en-US`; no `session_id` expiry signal | discord-claim: Look like the Discord client | `feat/discord-client-identity` |
| 24 | Gson used but not declared; rest uses kotlinx.serialization | code hygiene | `chore/drop-gson` |
| 25 | Two HTTP clients (OkHttp, java.net.http) | code hygiene | `chore/single-http-client` |
| 26 | No tests; unused ktlint entries in `versions.properties` | code hygiene | `chore/test-setup` |
| 27 | OpenSpec skills duplicated in `.agents/skills` and `.claude/skills` | tooling | `chore/dedupe-skills` |
| 28 | An empty `interactions` list is accepted, and the job reports "All interactions succeeded" | discord-claim: Discord job configuration | `fix/config-example` |
| 29 | When the feed runs out before 5 likes, the like loop scrolls forever. The run never ends, and the overlap lock then skips every later daily run | blablalink-claim: Like and read posts | `fix/blablalink-signin-detection` |

Items 1 and 2 (`.codegraph/` and `.idea/` not ignored) were fixed in `.gitignore` before this change.

### Found after drafting (fold into the next propose)

- **Interaction `name`.** The live `config.json` gives Discord interactions a `name` (for example `nikke daily`, `reverse1999 daily`), but the code ignores it as an unknown key. Add it to `discord-claim` as an optional field, and use it in the Job outcome message (pairs with #19).
- **Scope wording.** The Discord route already claims rewards for games other than NIKKE (Reverse: 1999). The proposal's "a gacha game (NIKKE)" should read "gacha games, with NIKKE via blablalink". The `discord-claim` purpose is already game-neutral.
- **Re-review intended behaviour.** The owner will re-check the remaining requirements in the next propose.

### Suggested order

1. `fix/blablalink-login-flow` (#13, #14, #16). Every restart runs the login flow, and startup crashes when the session is already logged in. That is the biggest risk on the home server. Add the test setup (#26) here, so this and later fixes ship with tests.
2. `fix/blablalink-signin-detection` (#15, #29). A silent false "already claimed" means missed rewards, which is the one thing ambil exists to prevent. A hung like loop stops all later blablalink runs.
3. `fix/single-outcome-notification` + `fix/job-result-messages` (#17-#19, plus the interaction `name`). These cut Telegram noise and make failures readable. Do them together, because both reshape the result message.
4. `fix/config-example` + `fix/validate-retry-policy` (#7-#12, #28). Small, cheap, and they make a fresh setup work.
5. `chore/fix-run-scripts` + `chore/fix-contributing` (#3-#6). Do these before tagging `v1.0.0`, so the release flow they describe is real.
6. The rest (#20, #22-#25, #27, then decide #21) as time allows. #23 matters only if Discord starts rejecting requests.

## Risks / Trade-offs

- [Specs claim behaviour the code lacks] → Every such requirement is in the table above. Do not read the specs as a description of the current build.
- [Self-botting on Discord breaks ToS] → Accepted by the owner. Low request volume (one click per reward per day) keeps exposure small.
- [Site markup changes break selectors] → Not specced. The "Page changed" scenario makes it fail loudly instead of reporting success.
