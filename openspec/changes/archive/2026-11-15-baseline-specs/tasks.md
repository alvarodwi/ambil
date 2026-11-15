# Tasks

## 1. Validate baseline

- [x] 1.1 Run `openspec validate baseline-specs --strict` and verify it passes with no warnings
- [x] 1.2 Read each of the five spec files against the code and verify every requirement is either met by the code or listed in design.md Known deviations

## Workflow follow-up

- Archive the change so the five specs land in `openspec/specs/`, then run `openspec list --specs` and confirm all five appear.
- Make the initial commit on `main` with the archived specs and the `.gitignore` fix, then create `develop` from it per `CONTRIBUTING.md`.
- Open one change per row of design.md Known deviations.
