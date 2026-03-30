#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/deployment/docker/docker-compose.yml"

ENV_FILE="${1:-$SCRIPT_DIR/.env.test.local}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "❌ 未找到配置文件：$ENV_FILE"
  echo "请先执行：cp \"$SCRIPT_DIR/.env.test.example\" \"$SCRIPT_DIR/.env.test.local\""
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

START_INFRA="${START_INFRA:-true}"
BUILD_BACKEND="${BUILD_BACKEND:-true}"
SPRING_PROFILE="${SPRING_PROFILE:-test}"
APP_PORT="${APP_PORT:-8080}"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-ticket_platform_test}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?请通过环境变量或 .env.test.local 设置 MYSQL_PASSWORD}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?请通过环境变量或 .env.test.local 设置 MYSQL_ROOT_PASSWORD}"

REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_DB="${REDIS_DB:-1}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

JWT_SECRET="${JWT_SECRET:?请通过环境变量设置 JWT_SECRET}"
RUN_BASE_DIR="${RUN_BASE_DIR:-.run}"

RUN_DIR="$PROJECT_DIR/$RUN_BASE_DIR"
LOG_DIR="$RUN_DIR/logs"
PID_FILE="$RUN_DIR/ticket-bootstrap.pid"
APP_LOG="$LOG_DIR/ticket-bootstrap.log"

mkdir -p "$LOG_DIR"

info() { echo "[INFO] $*"; }
warn() { echo "[WARN] $*"; }
err() { echo "[ERROR] $*" >&2; }

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    err "缺少命令：$1"
    exit 1
  fi
}

is_true() {
  local value="${1:-}"
  [[ "$value" == "true" || "$value" == "TRUE" || "$value" == "1" || "$value" == "yes" || "$value" == "YES" ]]
}

wait_http_ok() {
  local url="$1"
  local seconds="$2"
  local i
  for ((i=1; i<=seconds; i++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_mysql_ready_in_container() {
  local container="$1"
  local seconds="$2"
  local i
  for ((i=1; i<=seconds; i++)); do
    if docker exec "$container" mysqladmin ping -h127.0.0.1 -uroot "-p$MYSQL_ROOT_PASSWORD" --silent >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_redis_ready_in_container() {
  local container="$1"
  local seconds="$2"
  local i
  for ((i=1; i<=seconds; i++)); do
    if docker exec "$container" redis-cli ping >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

stop_old_process_if_exists() {
  if [[ -f "$PID_FILE" ]]; then
    local old_pid
    old_pid="$(<"$PID_FILE")"
    if [[ -n "$old_pid" ]] && kill -0 "$old_pid" >/dev/null 2>&1; then
      info "停止旧进程 PID=$old_pid"
      kill "$old_pid" || true
      sleep 2
      if kill -0 "$old_pid" >/dev/null 2>&1; then
        warn "旧进程未退出，执行强制停止 PID=$old_pid"
        kill -9 "$old_pid" || true
      fi
    fi
    rm -f "$PID_FILE"
  fi
}

build_backend() {
  info "开始构建后端（Maven）"
  (
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -pl ticket-bootstrap -am
  )
  info "后端构建完成"
}

resolve_jar_file() {
  local jars=("$PROJECT_DIR"/ticket-bootstrap/target/ticket-bootstrap-*.jar)
  if [[ ! -e "${jars[0]}" ]]; then
    err "未找到可运行 jar，请先构建：$PROJECT_DIR/ticket-bootstrap/target/ticket-bootstrap-*.jar"
    exit 1
  fi
  echo "${jars[0]}"
}

start_backend() {
  local jar_file="$1"

  info "启动后端服务（Profile=$SPRING_PROFILE, Port=$APP_PORT）"

  local cmd=(
    java -jar "$jar_file"
    "--spring.profiles.active=$SPRING_PROFILE"
    "--server.port=$APP_PORT"
    "--spring.datasource.url=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/$MYSQL_DB?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
    "--spring.datasource.username=$MYSQL_USER"
    "--spring.datasource.password=$MYSQL_PASSWORD"
    "--spring.redis.host=$REDIS_HOST"
    "--spring.redis.port=$REDIS_PORT"
    "--spring.redis.database=$REDIS_DB"
    "--jwt.secret=$JWT_SECRET"
  )

  if [[ -n "$REDIS_PASSWORD" ]]; then
    cmd+=("--spring.redis.password=$REDIS_PASSWORD")
  fi

  if [[ -n "${WECOM_CORP_ID:-}" ]]; then
    cmd+=("--wecom.corp-id=$WECOM_CORP_ID")
  fi
  if [[ -n "${WECOM_AGENT_ID:-}" ]]; then
    cmd+=("--wecom.agent-id=$WECOM_AGENT_ID")
  fi
  if [[ -n "${WECOM_SECRET:-}" ]]; then
    cmd+=("--wecom.secret=$WECOM_SECRET")
  fi
  if [[ -n "${WECOM_CONTACT_SECRET:-}" ]]; then
    cmd+=("--wecom.contact-secret=$WECOM_CONTACT_SECRET")
  fi
  if [[ -n "${WECOM_CALLBACK_TOKEN:-}" ]]; then
    cmd+=("--wecom.callback-token=$WECOM_CALLBACK_TOKEN")
  fi
  if [[ -n "${WECOM_CALLBACK_AES_KEY:-}" ]]; then
    cmd+=("--wecom.callback-aes-key=$WECOM_CALLBACK_AES_KEY")
  fi
  if [[ -n "${WECOM_TRUSTED_DOMAIN:-}" ]]; then
    cmd+=("--wecom.trusted-domain=$WECOM_TRUSTED_DOMAIN")
  fi
  if [[ -n "${WECOM_MQ_ENABLED:-}" ]]; then
    cmd+=("--wecom.mq.enabled=$WECOM_MQ_ENABLED")
  fi

  nohup "${cmd[@]}" >"$APP_LOG" 2>&1 &
  local pid="$!"
  echo "$pid" > "$PID_FILE"
  info "后端进程已启动，PID=$pid"

  if wait_http_ok "http://127.0.0.1:$APP_PORT/actuator/health" 60; then
    info "健康检查通过：http://127.0.0.1:$APP_PORT/actuator/health"
  else
    err "健康检查失败，请查看日志：$APP_LOG"
    exit 1
  fi
}

start_infra_if_needed() {
  if ! is_true "$START_INFRA"; then
    info "已跳过基础设施启动（START_INFRA=$START_INFRA）"
    return
  fi

  if [[ ! -f "$COMPOSE_FILE" ]]; then
    err "未找到 docker compose 文件：$COMPOSE_FILE"
    exit 1
  fi

  info "启动本地基础设施：MySQL / Redis / MinIO"
  docker compose -f "$COMPOSE_FILE" up -d mysql redis minio

  if wait_mysql_ready_in_container "ticket-mysql" 60; then
    info "MySQL 容器就绪"
  else
    err "MySQL 容器未就绪，请执行 docker logs ticket-mysql 排查"
    exit 1
  fi

  if wait_redis_ready_in_container "ticket-redis" 30; then
    info "Redis 容器就绪"
  else
    err "Redis 容器未就绪，请执行 docker logs ticket-redis 排查"
    exit 1
  fi

  info "确保测试数据库存在：$MYSQL_DB"
  docker exec ticket-mysql mysql -uroot "-p$MYSQL_ROOT_PASSWORD" \
    -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DB\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
}

print_summary() {
  echo ""
  echo "=========================================="
  echo "✅ 测试环境后端发布完成"
  echo "=========================================="
  echo "应用地址:        http://127.0.0.1:$APP_PORT"
  echo "健康检查:        http://127.0.0.1:$APP_PORT/actuator/health"
  echo "Swagger:         http://127.0.0.1:$APP_PORT/swagger-ui.html"
  echo "运行日志:        $APP_LOG"
  echo "进程 PID 文件:   $PID_FILE"
  if is_true "$START_INFRA"; then
    echo "MySQL:           127.0.0.1:3306 (DB=$MYSQL_DB)"
    echo "Redis:           127.0.0.1:6379 (DB=$REDIS_DB)"
    echo "MinIO Console:   http://127.0.0.1:9001"
  fi
  echo "=========================================="
}

main() {
  require_command java
  require_command curl
  if is_true "$BUILD_BACKEND"; then
    require_command mvn
  fi
  if is_true "$START_INFRA"; then
    require_command docker
  fi

  info "使用配置文件：$ENV_FILE"
  start_infra_if_needed

  if is_true "$BUILD_BACKEND"; then
    build_backend
  else
    info "已跳过后端构建（BUILD_BACKEND=$BUILD_BACKEND）"
  fi

  stop_old_process_if_exists
  local jar_file
  jar_file="$(resolve_jar_file)"
  start_backend "$jar_file"
  print_summary
}

main "$@"
