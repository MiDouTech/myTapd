#!/bin/bash
set -euo pipefail

# Session startup hook for miduo-ticket-platform
# Only runs in Claude Code remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

log() { echo "[session-start] $*"; }

log "Session setup complete for miduo-ticket-platform!"
