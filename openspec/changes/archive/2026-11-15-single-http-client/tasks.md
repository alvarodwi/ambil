# Tasks

## 1. One HTTP client

- [x] 1.1 Send the Discord interaction with `java.net.http.HttpClient`, keeping timeouts, headers, body and status handling. Verify `./gradlew check` passes
- [x] 1.2 Remove OkHttp from `build.gradle.kts` and `versions.properties`, and verify `./gradlew shadowJar` builds a jar with no `okhttp3` classes

## Workflow follow-up

- Branch: `chore/single-http-client`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff chore/single-http-client`, then `git branch -d chore/single-http-client`.
