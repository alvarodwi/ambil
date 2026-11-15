#!/bin/bash

set -euo pipefail

cd "$(dirname "$0")" || exit 1

LOGFILE="./ambil.log"
PIDFILE="./ambil.pid"
RUN_SCRIPT="./run.sh"

echo "🚀 Starting JAR in background..."
echo "📁 Working dir: $(pwd)"
echo "📄 Logging to: $LOGFILE"

# If running over SSH, attach to graphical display session
if [[ -n "${SSH_CONNECTION:-}" || -n "${SSH_CLIENT:-}" ]]; then
  export DISPLAY=:0
  export XAUTHORITY="$HOME/.Xauthority"
  echo "🌐 Detected SSH — using DISPLAY=$DISPLAY and XAUTHORITY=$XAUTHORITY"
fi

# Run the app in background (run.sh will handle PID)
nohup "$RUN_SCRIPT" >> "$LOGFILE" 2>&1 &

# Wait for the process to start and check its PID
sleep 1

# Check if the application is running by reading from the PID file
if [[ -f "$PIDFILE" && -n "$(cat "$PIDFILE")" ]]; then
  echo "JAR started with PID $(cat "$PIDFILE")"
else
  echo "Failed to start JAR. Check $LOGFILE for details."
fi