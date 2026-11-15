# Tasks

## 1. Example config

- [x] 1.1 Rewrite `config.json.example` as valid JSON with `telegram`, `headless: true`, one disabled `"type": "discord"` job showing every `DiscordConfig` and `DiscordInteractionConfig` field, and one disabled `"type": "blablalink"` job, each with a non-blank unique name and `retry.maxAttempts: 3`, and verify `python3 -m json.tool config.json.example` succeeds
- [x] 1.2 Add a `ConfigLoaderTest` case that decodes `config.json.example` with `ConfigLoader.json` and asserts one `DiscordConfig` and one `BlablalinkConfig` job, all names non-blank and every `maxAttempts` at least 1, and verify `./gradlew test` passes

## 2. Error message

- [x] 2.1 Change the missing-config message in `ConfigLoader.load` to name `config.json.example`, and verify `grep -rn "config.example.json" src` finds nothing

## Workflow follow-up

- Branch: `fix/config-example`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff fix/config-example`, then `git branch -d fix/config-example`.
