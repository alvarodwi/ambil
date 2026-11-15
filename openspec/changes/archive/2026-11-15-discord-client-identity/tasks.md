# Tasks

## 1. One current identity

- [x] 1.1 Define the user agent, browser version, locale and client build once, from the captured Firefox 149 request, and use them for the `User-Agent` and `X-Discord-Locale` headers and in the super-properties. Verify a unit test that the super-properties match the headers passes with `./gradlew check`

## Workflow follow-up

- Branch: `feat/discord-client-identity`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff feat/discord-client-identity`, then `git branch -d feat/discord-client-identity`.
