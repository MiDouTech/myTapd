#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

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

INSTALL_DEPS="${INSTALL_DEPS:-true}"
BUILD_MODE="${BUILD_MODE:-production}"
DIST_DIR="${DIST_DIR:-dist}"
DEPLOY_DIR="${DEPLOY_DIR:-}"

VITE_APP_TITLE="${VITE_APP_TITLE:-米多工单系统（测试环境）}"
VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://127.0.0.1:8080/api}"
VITE_USE_PROXY="${VITE_USE_PROXY:-false}"
VITE_API_PROXY_TARGET="${VITE_API_PROXY_TARGET:-}"
VITE_WECOM_OAUTH_URL="${VITE_WECOM_OAUTH_URL:-}"

info() { echo "[INFO] $*"; }
err() { echo "[ERROR] $*"; }

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

install_dependencies() {
  if ! is_true "$INSTALL_DEPS"; then
    info "已跳过依赖安装（INSTALL_DEPS=$INSTALL_DEPS）"
    return
  fi

  info "安装前端依赖"
  if [[ -f "$PROJECT_DIR/package-lock.json" ]]; then
    (cd "$PROJECT_DIR" && npm ci)
  else
    (cd "$PROJECT_DIR" && npm install)
  fi
}

build_frontend() {
  info "开始构建前端（mode=$BUILD_MODE）"
  (
    cd "$PROJECT_DIR"
    VITE_APP_TITLE="$VITE_APP_TITLE" \
    VITE_API_BASE_URL="$VITE_API_BASE_URL" \
    VITE_USE_PROXY="$VITE_USE_PROXY" \
    VITE_API_PROXY_TARGET="$VITE_API_PROXY_TARGET" \
    VITE_WECOM_OAUTH_URL="$VITE_WECOM_OAUTH_URL" \
    npm run build -- --mode "$BUILD_MODE"
  )
  info "前端构建完成"
}

deploy_dist_if_needed() {
  local dist_path="$PROJECT_DIR/$DIST_DIR"
  if [[ ! -d "$dist_path" ]]; then
    err "未找到构建产物目录：$dist_path"
    exit 1
  fi

  if [[ -z "$DEPLOY_DIR" ]]; then
    info "未配置 DEPLOY_DIR，仅保留本地产物：$dist_path"
    return
  fi

  info "发布前端静态资源到：$DEPLOY_DIR"
  mkdir -p "$DEPLOY_DIR"
  rm -rf "$DEPLOY_DIR"/*
  cp -a "$dist_path"/. "$DEPLOY_DIR"/
}

print_summary() {
  local dist_path="$PROJECT_DIR/$DIST_DIR"
  echo ""
  echo "=========================================="
  echo "✅ 前端测试环境构建完成"
  echo "=========================================="
  echo "构建目录:       $dist_path"
  if [[ -n "$DEPLOY_DIR" ]]; then
    echo "发布目录:       $DEPLOY_DIR"
  fi
  echo "VITE_API_BASE_URL:   $VITE_API_BASE_URL"
  echo "VITE_WECOM_OAUTH_URL:${VITE_WECOM_OAUTH_URL:-<未配置>}"
  echo "=========================================="
}

main() {
  require_command node
  require_command npm

  info "使用配置文件：$ENV_FILE"
  install_dependencies
  build_frontend
  deploy_dist_if_needed
  print_summary
}

main "$@"
