# Tasks

## 1. start.sh

- [x] 1.1 Rewrite `start.sh` as POSIX `sh` (`#!/bin/sh`, `[ ]`, no `pipefail`) with the SSH DISPLAY/XAUTHORITY block and all its comments kept, and verify `shellcheck -s sh start.sh` reports nothing
- [x] 1.2 Merge `run.sh` into `start.sh`: one `nohup java $JAVA_OPTS -jar ambil.jar` writing `$!` to `ambil.pid`, keep the DISPLAY echo and its `45971a1` comment, delete `run.sh`, and verify `grep -r run.sh` outside `openspec/changes/archive` finds nothing
- [x] 1.3 Refuse to start when `ambil.pid` holds a live PID, and verify a second `sh start.sh` prints the refusal and starts no new java process
- [x] 1.4 Check `kill -0` after `sleep 1` and exit non-zero on failure, and verify `sh start.sh` and `bash start.sh` both print "started with PID" and the PID matches `pgrep -f ambil.jar`
- [x] 1.5 Write each session to `logs/ambil-YYYYmmdd-HHMMSS.log`, point the `ambil.log` symlink at it, and move an existing regular `ambil.log` to `logs/ambil-legacy.log`, then verify `readlink ambil.log` names the new file and the legacy contents are intact
- [x] 1.6 Prune `logs/ambil-*.log` to the newest 5, and verify that after 7 start/stop cycles `ls logs | wc -l` prints 5 and the current session is among them

## 2. stop.sh

- [x] 2.1 Rewrite `stop.sh` as POSIX `sh` with `cd "$(dirname "$0")"`, and verify `shellcheck -s sh stop.sh` reports nothing and `sh /full/path/stop.sh` from `/tmp` stops the app
- [x] 2.2 Remove a stale `ambil.pid` with a message, wait up to 10 s for a live process to exit before removing the pidfile, and verify both cases by hand

## 3. Repo housekeeping

- [x] 3.1 Add `logs/`, `ambil.log` and `ambil.pid` to `.gitignore`, and verify `git status` stays clean after a start

## Workflow follow-up

- Branch: `chore/fix-run-scripts` (already holds `45971a1`; merge `develop` into it or rebase before archive if conflicts appear).
- Archive the change on that branch and commit there.
- Merge into `develop` with `git merge --no-ff chore/fix-run-scripts`, then `git branch -d chore/fix-run-scripts`.
