# Development flow

## Branches

| Branch | Purpose |
|---|---|
| `main` | Released code only. Every commit on `main` is a tagged release (`vX.Y.Z`). Never commit to it directly. |
| `develop` | Integration branch. All change branches start from it and merge back into it. |
| `<type>/<change-name>` | One branch per OpenSpec change. `<type>` is `feat`, `fix` or `chore`. `<change-name>` is the OpenSpec change name, e.g. `feat/add-shadow-jar`. |

`v1.0.0` marks the first working version on Linux.

## One change

1. Branch from `develop` before running `openspec propose`:
   ```sh
   git switch develop
   git switch -c feat/<change-name>
   ```
2. Propose, apply and archive the change on that branch, so the specs and the code land together.
3. Merge into `develop` without fast-forward, then delete the branch:
   ```sh
   git switch develop
   git merge --no-ff feat/<change-name>
   git branch -d feat/<change-name>
   ```

## Release

Release only after several changes have piled up in `develop`.

1. On `develop`, bump `version` in `build.gradle.kts` using semver:
   - major: breaking change to `config.yaml`, scraper definitions or CLI usage
   - minor: new feature
   - patch: fixes only
2. Commit the bump: `chore(release): vX.Y.Z`.
3. Merge into `main` and tag it:
   ```sh
   git switch main
   git merge --no-ff develop
   git tag -a vX.Y.Z -m "scraver X.Y.Z"
   ```
4. Build the artifact from the tagged commit: `./gradlew clean shadowJar`. The runnable jar is `build/libs/scraver-X.Y.Z-all.jar`.
5. Switch back to `develop` to continue work.
