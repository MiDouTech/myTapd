#!/bin/bash
set -euo pipefail

# Session startup hook for miduo-ticket-platform
# Only runs in Claude Code remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
STARTUP_SCRIPT="/workspace/scripts/cloud-agent-startup.sh"

log() { echo "[session-start] $*"; }

if [ ! -f "${STARTUP_SCRIPT}" ]; then
  log "Startup script not found, skip bootstrap: ${STARTUP_SCRIPT}"
  exit 0
fi

log "Running cloud agent startup bootstrap: ${STARTUP_SCRIPT}"
bash "${STARTUP_SCRIPT}"
log "Session setup complete for miduo-ticket-platform!"
