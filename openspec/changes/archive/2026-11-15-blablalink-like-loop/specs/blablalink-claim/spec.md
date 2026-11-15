## MODIFIED Requirements

### Requirement: Like and read posts
After the sign-in step, the system SHALL go through the Outpost feed and like up to 5 posts that are not already liked, opening each liked post's detail page to count as read. It SHALL NOT click the like button of a post the site already shows as liked. A post SHALL count only when the site shows it liked after the click and its detail page opened. It SHALL scroll to load more posts when needed and skip posts it cannot interact with.

#### Scenario: Five new likes
- **WHEN** the feed has at least 5 unliked posts
- **THEN** the system likes 5 posts, opens each, and reports "Liked 5 posts"

#### Scenario: Feed runs out
- **WHEN** no more posts load after scrolling
- **THEN** the system stops and reports how many posts it liked

#### Scenario: Post already liked
- **WHEN** a feed post already shows as liked
- **THEN** the system does not click its like button, the post stays liked, and it does not count toward the total

#### Scenario: Like does not register
- **WHEN** the system clicks like on a post and the site does not show it as liked within the wait
- **THEN** the post does not count, and the system moves on to the next post

#### Scenario: Detail page does not open
- **WHEN** a post is liked but its detail page does not open
- **THEN** the post does not count toward "Liked N posts", and the run continues with the next post

#### Scenario: Headless and visible runs
- **WHEN** a run uses headless mode, or a visible window on a desktop-size screen
- **THEN** the like step finds, likes and opens Outpost posts in both cases
