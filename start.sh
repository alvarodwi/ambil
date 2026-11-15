#!/bin/sh

set -eu

cd "$(dirname "$0")" || exit 1

JAR_NAME="ambil.jar"
JAVA_OPTS="-Xms512m -Xmx1G"
PIDFILE="./ambil.pid"
LOGDIR="./logs"
LOGLINK="./ambil.log"
KEEP_LOGS=5

# Refuse a second start while the last one is still running
if [ -f "$PIDFILE" ]; then
  OLD_PID=$(cat "$PIDFILE")
  if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
    echo "⚠️ Already running with PID $OLD_PID. Run ./stop.sh first."
    exit 1
  fi
fi

mkdir -p "$LOGDIR"
# Keep the old single log file from before per-session logs
if [ -f "$LOGLINK" ] && [ ! -L "$LOGLINK" ]; then
  mv "$LOGLINK" "$LOGDIR/ambil-legacy.log"
fi
LOGFILE="$LOGDIR/ambil-$(date +%Y%m%d-%H%M%S).log"
: > "$LOGFILE"
ln -sfn "$LOGFILE" "$LOGLINK"
# Keep only the newest session logs (names have no spaces, so word splitting is safe)
# shellcheck disable=SC2012
ls -1t "$LOGDIR"/ambil-*.log | tail -n +$((KEEP_LOGS + 1)) | xargs -r rm -f --

echo "🚀 Starting JAR in background..."
echo "📁 Working dir: $(pwd)"
echo "📄 Logging to: $LOGFILE (latest: $LOGLINK)"

# If running over SSH, attach to graphical display session
# Note: this only helps when a desktop session is logged in on the server's screen (:0).
# Without one, a visible Chrome ("headless": false, or the blablalink login window) exits at
# start with "SessionNotCreatedException ... Chrome instance exited".
# Over SSH, set "headless": true in config.json. Do the blablalink login at the server's screen.
if [ -n "${SSH_CONNECTION:-}" ] || [ -n "${SSH_CLIENT:-}" ]; then
  export DISPLAY=:0
  export XAUTHORITY="$HOME/.Xauthority"
  echo "🌐 Detected SSH — using DISPLAY=$DISPLAY and XAUTHORITY=$XAUTHORITY"
fi

# If DISPLAY or XAUTHORITY aren't set (possible if running locally or in a different context), set them
# Note: without a usable display, only "headless": true works. A visible Chrome ("headless": false,
# or the blablalink login window) exits at start with "Chrome instance exited". See the SSH note above.
if [ -z "${DISPLAY:-}" ] || [ -z "${XAUTHORITY:-}" ]; then
  echo "🔴 Missing DISPLAY or XAUTHORITY, skipping GUI binding"
else
  echo "🖥️ Using display: $DISPLAY and XAUTHORITY=$XAUTHORITY"
fi

# JAVA_OPTS is split into separate JVM flags on purpose
# shellcheck disable=SC2086
nohup java $JAVA_OPTS -jar "$JAR_NAME" > "$LOGFILE" 2>&1 &
APP_PID=$!
echo "$APP_PID" > "$PIDFILE"

# Give the JVM a moment, then check it is still alive
sleep 1
if kill -0 "$APP_PID" 2>/dev/null; then
  echo "✅ JAR started with PID $APP_PID"
else
  rm -f "$PIDFILE"
  echo "❌ Failed to start JAR. Check $LOGFILE for details."
  exit 1
fi
