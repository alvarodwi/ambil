# Design

## Context

See proposal.md - Why. Today `browseAndLike()` opens `?plate_type=outpost` (the working tree has an uncommitted `?plate_type= outpost`, which loads the "recommend" feed). It treats every `div[@data-cname='index']` under `div[@data-cname='infinite-scroll']` as a post, finds the like button with `./div[last() - 1]/div[last()]//span[3]//span[@data-cname='like']`, and reads `getCssValue("color")` on that span, expecting `62, 175, 255`.

**How the site picks a layout** (from its JS bundle, `isLegacyPcLayout` and `syncPcLayoutMode`). It reads `window.screen`, not the window size:

```
window.screen
  min(w,h) <= 600 -------------------------> pure mobile  (no layout switch)
  width < 1200 ----------------------------> legacy PC    (forced)
  ?pc_layout=legacy|modern ----------------> that mode, saved to localStorage
  localStorage __ss_storage_ls_cache_pc_layout_mode__ == "legacy" --> legacy PC
  otherwise -------------------------------> modern (default)
```

Headless Chrome reports `screen` as 800x600, so headless runs always get pure mobile. A visible window on a desktop screen gets modern. The "Switch to Classic Layout" button calls the same `setPcLayoutMode()` as the query parameter, then reloads.

**Logged-out DOM, checked with headless Chrome** (`--screen-info={1920x1080}` for the PC layouts):

| check | pure mobile | modern | legacy PC |
|---|---|---|---|
| posts: `infinite-scroll/div[@plate_name]` | 10 | 10 | 10 |
| `.//div[@data-cname='index']` (counted as posts today) | 52 | 52 | 52 |
| today's like path | 0 | 10 | 0 |
| exactly one `.//span[@data-cname='like']` per post | 10 | 10 | 10 |
| `./div[2]` (post title, clicked to open) | 10 | 10 | 10 |
| mission page sign-in and "Log In" selectors | ok | ok | ok |

Modern posts have an extra trailing divider (`div.w-full.h-[6PX]`). That is why `div[last() - 1]` reaches the action row only on modern. Pure mobile and legacy PC have the same post structure.

**Liked state** (from the owner's logged-in markup). `span[data-cname='like']` carries no color. Its inner `span[data-cname='svg-icon']` has `style="color: var(--text-3);"` when not liked and `style="color: var(--brand-1);"` when liked. Today's check reads the outer span's inherited color, so it never sees a liked post.

**Opening a post.** On the legacy layout, clicking a post opens `https://www.blablalink.com/post/detail?post_uuid=<id>` (checked by the owner). The URL carries no `pc_layout` parameter; the saved layout applies, and adding `pc_layout=legacy` to it also works.

**Scrolling.** `infinite-scroll` is `relative w-full`; the element that scrolls is an `overflow-y-auto` ancestor. `scrollTop = scrollHeight` on `infinite-scroll` does nothing.

## Goals / Non-Goals

**Goals:**
- One post structure for headless and visible runs.
- Never unlike a post. Count only confirmed likes on posts that were also opened.

**Non-Goals:**
- Changing the sign-in step or the mission page: its selectors already work on every layout.
- Changing the like target (5), the wait times or the fixed sleeps.

## Decisions

- **Classic layout through the URL, not the button.** The feed URL becomes `https://www.blablalink.com/?pc_layout=legacy&plate_type=outpost`. The site saves the choice in the profile's localStorage. The parameter needs no reload and does not depend on the site language, while the button's `aria-label` is translated. Headless runs ignore it and stay on pure mobile, which has the same structure. Alternative: click `[data-cname='pc-layout-switch']`. Rejected: headless never renders it, and a click reloads the page mid-step. Only the feed URL gets the parameter, because the mission page works on every layout.
- **Locators work on every layout.**
  - Post: `//div[@data-cname='infinite-scroll']/div[@plate_name]` (the post root carries `plate_id` and `plate_name`).
  - Like button: `.//span[@data-cname='like']` inside the post.
  - Like icon: `.//span[@data-cname='like']//span[@data-cname='svg-icon']`.
  - Open target: `./div[2]` (unchanged).

  No positional XPath: the loop indexes into the list `findElements` returns.
- **Pure liked-state mapping, tested.** `likeState(iconStyle: String?)` returns `LIKED` when the style contains `--brand-1`, `NOT_LIKED` when it contains `--text-3`, and `UNKNOWN` otherwise. This mirrors `signInState`. It reads the style attribute (`getDomAttribute("style")`), not the computed color, so the theme does not matter. Alternative: the svg `viewBox` (`0 0 48 48` vs `0 0 41 40`). Rejected: icon geometry changes more often than the theme token.
- **Per-post flow.** `NOT_LIKED`: JS-click the like span (as today), then wait up to 5 seconds, re-reading the icon each poll, for `LIKED`. `LIKED` or `UNKNOWN`: skip without clicking, so the run can never unlike. After a confirmed like, click `./div[2]`, falling back to a JS click on `ElementClickInterceptedException` (as sign-in does). Wait for `/post/detail`, then go back. Count the post only after the detail page opened.
- **Failures stay inside one post.** The whole per-post body, including the post lookup, is inside the `try`. A failed post is logged and skipped. If a failure leaves the browser off the feed, reload the feed URL before the next post.
- **Load more by scrolling the last post into view.** When the index passes the loaded posts, `scrollIntoView` the last post, then `waitForMorePosts()` counts the post locator. This works whichever ancestor scrolls. No growth means the feed is exhausted, as today.

## Risks / Trade-offs

- [The feed re-renders in a different order after going back, so the index revisits or skips a post] → A revisited post reads `LIKED` and is skipped, so nothing is unliked. A skipped post only costs one candidate.
- [The like icon's style token changes] → `UNKNOWN` skips every post without clicking, so the run reports "Liked 0 posts" and nothing is unliked. A one-line fix to the mapping is needed.
- [Logged-in markup differs from what was checked logged out] → The liked-state style comes from the owner's logged-in markup. A visible run and a headless run during apply confirm the rest.
- [The profile keeps the legacy layout, so the visible login window also shows it] → The mission page and "Log In" selectors were checked on legacy PC, so login is unaffected.

## Migration Plan

No config change. To return the profile to the modern layout, open `https://www.blablalink.com/?pc_layout=modern` once with that profile. Rollback is reverting the branch.
