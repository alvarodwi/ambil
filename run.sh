#!/bin/bash

set -euo pipefail

JAR_NAME="ambil.jar"
JAVA_OPTS="-Xms512m -Xmx1G"
PIDFILE="./ambil.pid"

# If DISPLAY or XAUTHORITY aren't set (possible if running locally or in a different context), set them
if [[ -z "${DISPLAY:-}" || -z "${XAUTHORITY:-}" ]]; then
  echo "🔴 Missing DISPLAY or XAUTHORITY, skipping GUI binding"
else
  echo "🖥️ Using display: $DISPLAY and XAUTHORITY=$XAUTHORITY"
fi

# Run the JAR
echo "🚀 Starting the JAR: $JAR_NAME"
nohup java $JAVA_OPTS -jar "$JAR_NAME" \
  >> ./ambil.log 2>&1 &

# Capture the PID of the Java process
APP_PID=$!
echo "$APP_PID" > "$PIDFILE"

# Check if the application is running
if ps -p "$APP_PID" > /dev/null; then
  echo "JAR started successfully with PID $APP_PID"
else
  echo "Failed to start JAR. Check logs for errors."
  exit 1
fi
