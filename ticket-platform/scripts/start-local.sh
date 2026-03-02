#!/bin/bash

echo "=========================================="
echo "  Ticket Platform - 本地启动"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR" || exit 1

echo "[1/3] 启动基础设施 (MySQL + Redis + MinIO)..."
docker compose -f deployment/docker/docker-compose.yml up -d

echo "[2/3] 等待 MySQL 启动..."
sleep 10

echo "[3/3] 编译并启动应用..."
mvn clean package -DskipTests -pl ticket-bootstrap -am
java -jar ticket-bootstrap/target/ticket-bootstrap-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

echo "=========================================="
echo "  启动完成: http://localhost:8080"
echo "  Swagger: http://localhost:8080/swagger-ui.html"
echo "=========================================="
