# Proposal

## Why

The like step no longer does what the daily missions need. blablalink added a modern desktop layout about two months ago, and the like loop now breaks in two ways:

- On the modern layout (visible runs, `"headless": false`), it never sees a post as liked. It reads the color from the wrong element, so it clicks posts that are already liked, which unlikes them, and still counts them. Each day it toggles the same top posts, so the like mission works one day and fails the next.
- On the mobile layout (headless runs, which always get it because headless Chrome reports an 800x600 screen), the like button path matches no post, so every post is skipped and the run likes nothing.

A post also counts as liked before its detail page opens, so "Liked 5 posts" can overstate the read mission (backlog #31).

## What Changes

- Open the Outpost feed in the site's classic layout (`?pc_layout=legacy`), so visible and headless runs see the same post structure.
- Find posts, like buttons and the liked state with locators that work on every layout.
- Skip posts that are already liked. Never click a liked post.
- After clicking like, wait until the site shows the post as liked. Count the post only once it is liked and its detail page opened.
- Open the post with a JS-click fallback when the normal click is intercepted.
- Load more posts by scrolling the last loaded post into view.
- A post that cannot be found or used is skipped, never a run failure.
- Restore the Outpost feed URL. The uncommitted `?plate_type= outpost` (with a space) loads the "recommend" feed instead.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `blablalink-claim`: "Like and read posts" now forbids unliking an already-liked post, and counts a post only when the site confirms the like and its detail page opened.

## Impact

- Code: `BlablalinkClient.kt` (`browseAndLike`, `waitForMorePosts`, feed URL and locators).
- Tests: a new pure function that maps the like icon's style to liked or not liked, with unit tests in `BlablalinkSessionTest.kt`.
- Behaviour: runs stop removing existing likes. The like count in the run summary now matches posts that are both liked and read, so it can be lower than before on a bad day.
- No config change.
