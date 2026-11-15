# Design

## Context

See proposal.md - Why. Today `loginAndClaimDailyReward()` clicks the sign-in box and swallows any click error. It then waits 5 seconds for a `Sign In Success` toast, and otherwise returns "Sign in again tomorrow!". `browseAndLike()` loops until 5 likes. When `currentIndex` passes the loaded posts, it scrolls, calls `waitForMorePosts()` (which only logs on timeout), and `continue`s with no exit.

The Daily Sign-in row (a `div[data-cname='index']` holding the `Daily Sign-in` text) has an action box with an icon div whose class names the state. The owner supplied the claimed markup; the unclaimed markup comes from a rendered logged-out page:

```
unclaimed: bg-[var(--brand-1)]   + bg-[url('@/assets/imgs/common/icon-gift.png')]
claimed:   bg-[var(--fill-1-60)] + bg-[url('@/assets/imgs/common/icon-gift-true.png')]
```

Clicking an already-claimed box shows a "Sign in again tomorrow" message (seen by the owner). The success toast text after a fresh claim has not been observed.

## Goals / Non-Goals

**Goals:**
- Report "claimed" or "already claimed" only from an observed page state, and fail otherwise.
- The like step always ends.

**Non-Goals:**
- Passing sign-in and likes text into the job result and Telegram (#19, `fix/job-result-messages`).
- Retry and notification changes (#17, #18).

## Decisions

- **The row's icon is the state signal, not the toast.** Locate the row as the nearest `data-cname='index'` ancestor of the `Daily Sign-in` text (`//div[normalize-space(text())='Daily Sign-in']/ancestor::div[@data-cname='index'][1]`). Outer wrappers share that attribute, so a plain contains-match picks the wrong block. Read the icon div inside the row's `cursor-pointer` box. The icon class is the same signal before and after a click, and it is already confirmed for both states. Alternative: wait for the toast. Rejected, because its text after a fresh claim is unknown, and today's bug comes from depending on it.
- **Pure mapping, tested.** `signInState(iconClass: String?)` returns `CLAIMED` when the class contains `icon-gift-true`, `UNCLAIMED` when it contains `icon-gift` otherwise, and `UNKNOWN` when it is null or neither. The browser code only reads the class attribute.
- **Claim flow.**
  - `CLAIMED`: return "Already claimed today" without clicking.
  - `UNCLAIMED`: click the box (native click, as with the Log In chip), wait up to 10 seconds for the icon to read `CLAIMED`, and return "Sign-in reward claimed". If it doesn't switch in time, throw.
  - `UNKNOWN` or no row within 15 seconds: throw.

  Throws go through `run()`'s existing failure path (log plus alert, unsuccessful result).
- **Feed end.** `waitForMorePosts()` returns whether the post count grew. When it didn't, `browseAndLike()` breaks out of the loop and reports `Liked N posts`. It still counts as a successful run, per the "Feed runs out" scenario.

## Risks / Trade-offs

- [The site renames the icon asset] → `UNKNOWN` fails the run loudly instead of reporting success, so a one-line selector fix is needed.
- [The icon flips before the server records the claim] → It's the same signal the user sees. Accepted.
- [The feed is slow to load, not exhausted] → Today's 5-second wait after a scroll stays. A false "exhausted" ends the run early with fewer likes, and the next day's run tries again.

## Found during verification

- **#31 (backlog):** in the like step, a post is counted before its detail page is opened. On the 3.1 run, opening posts #1 and #4 failed with `element click intercepted`, so "Liked 5 posts" can overstate the read mission. This breaks `blablalink-claim`: Like and read posts. Fix in a later change: use a JS-click fallback for opening the post, and count a post only once it is both liked and read.
- **3.2 left open at archive:** the owner chose to archive before the first fresh claim (the next 07:30 run). If that run fails, the fix goes in a new change.
