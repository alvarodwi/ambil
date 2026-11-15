# Design

## Context

- `BlablalinkClient.run()` already returns `signIn` ("Already claimed today" or "Sign-in reward claimed") and `likes` ("Liked N posts."). `BlablalinkJob` throws both away on success.
- `DiscordClient.clickAllButtons()` returns `List<Result<Unit>>` in config order. `DiscordJob` only checks `any { it.isFailure }`.
- `ConfigLoader` uses `ignoreUnknownKeys = true`, which is why the live `name` field loads today and is silently dropped.

## Goals / Non-Goals

**Goals:**
- Outcome text a user can act on from the Telegram message alone.

**Non-Goals:**
- Message formatting beyond plain lines (no Markdown styling).
- Shortening long Discord error bodies for Telegram's 4096-character limit.
- Rejecting an empty `interactions` list (#28, `fix/config-example`).

## Decisions

- **Label resolved in the client.** `clickAllButtons()` returns `List<Pair<String, Result<Unit>>>`, with the label `interaction.name ?: "interaction ${index + 1}"`. The client already loops with the index and the config. Alternative: give `DiscordJob` the config too. Rejected, because the job would then duplicate the loop order.
- **A blank `name` counts as no name.** Use `name?.takeIf { it.isNotBlank() }`, because the example config ships `"name": ""` for jobs.
- **Pure formatter for the Discord outcome.** Add a top-level `discordJobResult(results: List<Pair<String, Result<Unit>>>): JobResult` in `DiscordJob.kt`, so it can be unit tested without HTTP. Format:
  - success: `Claimed: nikke daily, reverse1999 daily`
  - failure: one line per failed interaction, `Failed: reverse1999 daily: HTTP 401: {...}`, then `Claimed: nikke daily` if any succeeded.
  The error text is `exception.message ?: exception::class.simpleName`.
- **Blablalink success text.** `BlablalinkJob` returns `JobResult(true, "${result.signIn}. ${result.likes}")`, for example "Already claimed today. Liked 5 posts.". The failure branch keeps `result.signIn`, which holds "Failed: <cause>".

## Risks / Trade-offs

- [Discord error bodies are JSON and can be long] → Telegram escaping already handles the special characters. Length is out of scope. If a message ever exceeds 4096 characters, Telegram rejects it, and that is logged per the notification spec.
- [Changing the `clickAllButtons()` return type] → It has one caller, `DiscordJob`, so the change stays in two files.

## Migration Plan

No config change is required. Optionally add `name` to each Discord interaction in `config.json`. Roll back by reverting the merge. An added `name` still loads on the old build, because unknown keys are ignored.
