#!/bin/bash

echo "停止 Ticket Platform..."

PID=$(pgrep -f "ticket-bootstrap")
if [ -n "$PID" ]; then
    kill "$PID"
    echo "应用已停止 (PID: $PID)"
else
    echo "应用未运行"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "停止基础设施..."
docker compose -f "$PROJECT_DIR/deployment/docker/docker-compose.yml" down

echo "全部已停止"
