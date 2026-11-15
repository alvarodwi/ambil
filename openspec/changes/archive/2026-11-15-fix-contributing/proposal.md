# Proposal

## Why

`CONTRIBUTING.md` was copied from another project, "scraver". Its release steps name the wrong config file (`config.yaml`), talk about "scraper definitions", tag with the message `scraver X.Y.Z`, and point at a jar `build/libs/scraver-X.Y.Z-all.jar` that this build never makes. The build makes `build/libs/ambil.jar`, which `start.sh` runs. The doc also says every commit on `main` is a tagged release, but `main` starts with the untagged baseline-specs commit. These are backlog items #3 and #4 in the baseline design.

## What Changes

- Release steps name `config.json`, job config, `ambil X.Y.Z` and `build/libs/ambil.jar`.
- The `main` rule says every merge into `main` is a tagged release, and that the initial baseline-specs commit is the one untagged commit.
- No code changes.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. Repo tooling is not runtime behaviour (baseline design, Non-Goals), so this change sets `skip_specs: true`.

## Impact

- `CONTRIBUTING.md` only.
- Not included: the baseline proposal's "a gacha game (NIKKE)" wording. It lives only in the archived baseline proposal, which stays as history. No current spec repeats it.
