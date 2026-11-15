# Tasks

## 1. Drop Gson

- [x] 1.1 Build the interaction payload and super-properties in `DiscordClient` with `buildJsonObject`, remove every Gson import, and verify a unit test of the payload JSON passes with `./gradlew test` and `grep -rn gson src` finds nothing

## 2. Unused versions

- [x] 2.1 Remove the `## unused` ktlint entries from `versions.properties`, and verify `./gradlew check` still passes, including `lintKotlin`

## Workflow follow-up

- Branch: `chore/drop-gson`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff chore/drop-gson`, then `git branch -d chore/drop-gson`.
