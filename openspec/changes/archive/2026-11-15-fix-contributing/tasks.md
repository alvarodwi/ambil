# Tasks

## 1. CONTRIBUTING.md

- [x] 1.1 Replace `config.yaml`, "scraper definitions", `scraver X.Y.Z` and `build/libs/scraver-X.Y.Z-all.jar` with `config.json`, job config, `ambil X.Y.Z` and `build/libs/ambil.jar`, and verify `grep -n scraver CONTRIBUTING.md` finds nothing
- [x] 1.2 Reword the `main` rule so the untagged initial commit is the stated exception, and verify it matches `git log main`

## Workflow follow-up

- Branch: `chore/fix-contributing`, cut from `develop`.
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff chore/fix-contributing`, then `git branch -d chore/fix-contributing`.
