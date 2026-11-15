# Tasks

## 1. Discord interaction name

- [x] 1.1 Add `val name: String? = null` to `DiscordInteractionConfig`, and add a `name` to each interaction in `config.json.example`. Verify with a `ConfigLoaderTest` case: a discord job whose interaction has `"name": "nikke daily"` parses with that name, and one without `name` parses with `null`

## 2. Discord outcome text

- [x] 2.1 Change `DiscordClient.clickAllButtons()` to return `List<Pair<String, Result<Unit>>>`, with labels as in design.md (blank `name` counts as missing). Verify `./gradlew build` passes
- [x] 2.2 Add `discordJobResult(...)` in `DiscordJob.kt` with the format in design.md, and make `DiscordJob.execute()` use it. Verify with a new `DiscordJobTest`: all succeed lists both names; one HTTP 401 failure fails and names the failing interaction, "HTTP 401", and the claimed one; an unnamed second interaction shows as "interaction 2"

## 3. Blablalink summary

- [x] 3.1 Make `BlablalinkJob.execute()` return `"${result.signIn}. ${result.likes}"` on success. Verify `./gradlew build` passes, and by reading the code that the failure branch is unchanged

## 4. Manual check on home server

- [x] 4.1 Add `name` to the live `config.json` interactions if missing. Run once with `runOnStartup: true`. Verify the blablalink Telegram message shows the sign-in result and "Liked N posts", and the Discord message names each interaction

## Workflow follow-up

- Implement on branch `fix/job-result-messages` after `fix/single-outcome-notification` is merged into `develop`. Archive the change, then merge into `develop` with `--no-ff` per `CONTRIBUTING.md`.
