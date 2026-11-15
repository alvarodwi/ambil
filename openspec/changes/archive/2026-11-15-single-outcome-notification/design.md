# Design

## Context

Telegram is sent from three places today:

```
BlablalinkClient.run()   -- catch SessionExpired -> notifySessionExpired()   (msg 1, every attempt)
                         -- catch Exception      -> "automation failed"      (msg 1, every attempt)
RetryExecutor            -- after last attempt   -> "Failed after N attempts" (msg 2)
Main.executeJob          -- success / failed / crashed                       (msg 3)
```

`BlablalinkClient.run()` turns a `SessionExpiredException` into a failed `BlablalinkResult`, so `RetryExecutor` cannot tell it apart from other failures and retries it. `RetryExecutor` also drops the last attempt's message, so the final text says only "Failed after N attempts".

## Goals / Non-Goals

**Goals:**
- `Main.executeJob` is the only sender for a job run.
- `RetryExecutor` stops early on an expired session and keeps the last error.

**Non-Goals:**
- Reshaping job result text (`fix/job-result-messages`).
- A general "action needed" mechanism for other job types. Only blablalink has such a case today.
- Truncating long messages for Telegram's 4096-character limit.

## Decisions

- **`Main` is the only sender.** It already sends the success, failure and crash messages, and it knows the job name. Remove `notifier` from `BlablalinkClient` and `telegram` from `RetryExecutor`. Alternative: keep senders and add dedup. Rejected, because removing two calls is smaller than any dedup.
- **Expired session propagates as an exception.** `BlablalinkClient.run()` stops catching `SessionExpiredException`. The `finally` still closes the browser. `RetryExecutor` catches it before the generic `Exception` catch and returns `JobResult(false, "Action needed: <message>")` with no retry. Alternative: a new `actionNeeded` field on `JobResult`. Rejected, because it touches every job for one case. Alternative: a generic `ActionNeededException`. Rejected for now: one subclass, one caller. `SessionExpiredException` is already shared model code.
- **The exception carries the user instruction.** Its message becomes "BlaBlaLink session expired. Restart ambil where a display is available and log in in the window that opens." This keeps the text that `notifySessionExpired()` sent today, and `notifySessionExpired()` is deleted.
- **Keep the last error.** `RetryExecutor` stores the last failed `result.message`, or for an exception `e.message ?: e::class.simpleName`. The final message is "Failed after N attempts: <last error>". `Main` already adds the job name, so drop the `[name]` prefix from the message.
- **Generic blablalink errors stay a failed result.** The `catch (e: Exception)` in `run()` keeps returning `BlablalinkResult(signIn = "Failed: ...")` and only drops its `notifier.send`. These errors (slow page, changed selector) can pass on retry.

## Risks / Trade-offs

- [`InterruptedException` from `Thread.sleep` between attempts] → Not changed. It still reaches `Main`'s crash path, as today.
- [A user who relied on the per-attempt "automation failed" message to see early errors] → The final failure message now carries the last error, and every attempt is still logged.

## Migration Plan

No config or data change. Deploy as usual. Roll back by reverting the merge.
