#!/bin/bash

PIDFILE="./ambil.pid"

if [ -f "$PIDFILE" ]; then
  PID=$(cat "$PIDFILE")
  echo "🛑 Stopping JAR (PID $PID)..."
  kill "$PID" && rm "$PIDFILE"
  echo "✅ Stopped."
else
  echo "⚠️ No PID file found. Is JAR running?"
fi