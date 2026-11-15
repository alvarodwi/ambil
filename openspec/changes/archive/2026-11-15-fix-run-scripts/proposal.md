# Proposal

## Why

`sh start.sh` runs the script under `dash`, which ignores the `#!/bin/bash` line. The `[[ ]]` tests fail, so the script prints "Failed to start JAR" while the JAR is running. All output also goes into one `ambil.log`. `start.sh` and `run.sh` both append to it, so lines are written twice, and the file grows without limit across test runs. This is backlog item #5/#6 ("log written twice") in the baseline design.

## What Changes

- `start.sh` and `stop.sh` run the same under `sh` and `bash`: POSIX syntax only, `#!/bin/sh`.
- **BREAKING (tooling)**: `run.sh` is removed. `start.sh` launches the JAR itself with a single `nohup`.
- `start.sh` checks the process with `kill -0` after start, instead of only checking that `ambil.pid` exists.
- `start.sh` refuses to start when the PID in `ambil.pid` is still alive.
- `stop.sh` works from any directory and removes a stale `ambil.pid`.
- Each start writes to its own `logs/ambil-YYYYmmdd-HHMMSS.log`. `ambil.log` is a symlink to the newest one. Only the newest 5 session logs are kept.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- None. The baseline design keeps repo tooling (`run.sh`, `start.sh`, `stop.sh`) out of the specs, so this change sets `skip_specs: true`.

## Impact

- Files: `start.sh`, `stop.sh`, `run.sh` (deleted), `.gitignore` (`logs/`, `ambil.log`, `ambil.pid`).
- Deploy: anything that calls `run.sh` directly must call `start.sh`. The user confirmed nothing does.
- Existing `ambil.log` regular file: `start.sh` replaces it with the symlink. The old contents are moved into `logs/` so they are not lost.
- No app code or `logback.xml` change.
