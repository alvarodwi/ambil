# Tasks

## 1. Liked state

- [x] 1.1 Add `LikeState` (`LIKED`, `NOT_LIKED`, `UNKNOWN`) and `likeState(iconStyle: String?)` next to `signInState`, as in design.md. Verify with unit tests in `BlablalinkSessionTest.kt` for `color: var(--brand-1);`, `color: var(--text-3);`, null and an unrelated style, and `./gradlew test` passes

## 2. Feed loop

- [x] 2.1 Add a feed URL constant `https://www.blablalink.com/?pc_layout=legacy&plate_type=outpost` and use it in `browseAndLike()`. This replaces the uncommitted `?plate_type= outpost` edit. Verify `grep -n "plate_type" src/main` shows only the constant
- [x] 2.2 Rewrite the post loop in `browseAndLike()` with the locators and per-post flow from design.md:
  - posts from `infinite-scroll/div[@plate_name]`, indexed into the `findElements` list
  - `NOT_LIKED`: JS-click like, wait 5s for `LIKED`
  - `LIKED` or `UNKNOWN`: log the skip and do not click
  - open `./div[2]` with a JS-click fallback, wait for `/post/detail`, go back
  - count the post only after the detail page opened
  - the whole per-post body inside the `try`; reload the feed URL if a failure left the browser off it

  Verify `./gradlew build` passes, and by reading the code that no path clicks like on a `LIKED` or `UNKNOWN` post, and that the counter changes only after `/post/detail` was reached
- [x] 2.3 When the index passes the loaded posts, `scrollIntoView` the last post, and make `waitForMorePosts()` count the post locator. Remove the `scrollTop` script. Verify `./gradlew build` passes, and by reading the code that every path through the `while` loop either advances the index or exits

## 3. Manual check on home server

- [x] 3.1 Run once with `"headless": false` and `runOnStartup: true`. Verify the feed shows the classic layout, the log reports "Liked N posts" with N > 0, and the counted posts show as liked on the site
- [x] 3.2 Run again the same day. Verify the log shows the posts liked in 3.1 skipped as already liked, and they are still liked on the site
- [x] 3.3 Run once with `"headless": true`, then set `headless` back to its previous value. Verify the log reports "Liked N posts" with N > 0 and no post was unliked

## Workflow follow-up

- Branch: `fix/blablalink-like-loop`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff fix/blablalink-like-loop`, then `git branch -d fix/blablalink-like-loop`.
