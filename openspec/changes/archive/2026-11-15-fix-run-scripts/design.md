# Design

## Context

Today `start.sh` runs `nohup ./run.sh >> ambil.log`. `run.sh` then runs `nohup java ... >> ambil.log` and writes `ambil.pid`. So there are two nohups, and both append to the same log. `stop.sh` reads `./ambil.pid` relative to the caller's directory. The app logs only to stdout (`logback.xml` has a single `ConsoleAppender`), so the scripts decide where logs go. Commit `45971a1` on this branch added comments to `run.sh` and `start.sh` about a visible Chrome needing a desktop display. Those comments must survive the merge into `start.sh`.

## Goals / Non-Goals

**Goals:**
- `sh start.sh`, `bash start.sh` and `./start.sh` behave the same.
- Session logs on disk stay bounded across many test starts.

**Non-Goals:**
- Size limits inside one session. One long-running session can still grow. Logback `RollingFileAppender` is the upgrade path if that becomes a problem.
- systemd unit, or restart on crash.

## Decisions

- **POSIX `sh` instead of forcing bash.** `[ ]` replaces `[[ ]]`. `pipefail` is dropped, since no pipe's failure matters here. Alternative: keep bash and re-exec with `exec bash "$0"` when run under `sh`. Rejected: more code for the same result.
- **Merge `run.sh` into `start.sh`.** One `nohup java ... > "$LOG" 2>&1 &`, then `echo $! > ambil.pid`. `$!` is the java PID itself, so `kill -0` and `stop.sh` act on the real process. The DISPLAY/XAUTHORITY echo from `run.sh` moves into `start.sh`, along with its comment from `45971a1`.
- **Per-session file plus a symlink.** `logs/ambil-$(date +%Y%m%d-%H%M%S).log`, then `ln -sfn` from `ambil.log` to it. `tail -f ambil.log` keeps working for the current session. Alternative: logback rolling per startup. Rejected: needs an app change, and stdout capture would still be needed for JVM crash output.
- **Prune by mtime, keep 5.** `ls -1t logs/ambil-*.log | tail -n +6` lists the files to delete. Filenames are generated with no spaces, so plain word splitting is safe. Prune runs after the new log is created, so 5 includes the current session.
- **Liveness via `kill -0`.** After `sleep 1`, `kill -0 "$PID"` tells "started" from "died at once". The same check guards double start, and lets `stop.sh` tell a stale pidfile from a live process.
- **`stop.sh` waits for exit.** After `kill`, poll `kill -0` for up to 10 s, then remove `ambil.pid`. The pidfile is removed only once the process is gone.

## Risks / Trade-offs

- [The first start after upgrade finds `ambil.log` as a regular file] → `start.sh` moves it to `logs/ambil-legacy.log` before creating the symlink. It matches the prune glob, so it ages out after 5 more starts.
- [PID reuse: a stale `ambil.pid` points at an unrelated live process] → Accepted. Start then refuses, and the message names the PID so the user can check it. Matching the process command line would cost more than the risk is worth.
- [`sleep 1` misses a JVM that dies after 2 s, for example on a config error] → The start message still points to `ambil.log`. Accepted.

## Migration Plan

Deploy by copying `start.sh` and `stop.sh`, and deleting `run.sh` on the server. Rollback: restore the old scripts. They only append to `ambil.log`, which then follows the symlink into the newest session file.
