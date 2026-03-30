#!/bin/bash
# ============================================================
# Nacos 配置一键导入脚本
# 用法: bash nacos-import.sh [环境] [命名空间ID] [nacos地址] [用户名] [密码]
#
# 参数说明:
#   环境       - dev / test / prod (必填)
#   命名空间ID - Nacos namespace ID (可选，默认 e4479836-f77e-4b46-9e96-56179bdd6875)
#   nacos地址  - 如 http://10.0.4.4:8848 (可选，默认 http://10.0.4.4:8848)
#   用户名     - Nacos 用户名 (可选，默认 nacos)
#   密码       - Nacos 密码 (可选，默认 nacos)
#
# 示例:
#   bash nacos-import.sh test
#   bash nacos-import.sh prod e4479836-f77e-4b46-9e96-56179bdd6875 http://10.0.4.4:8848 nacos nacos
# ============================================================

set -euo pipefail

ENV="${1:?请指定环境: dev / test / prod}"
NS_ID="${2:-e4479836-f77e-4b46-9e96-56179bdd6875}"
NACOS_ADDR="${3:-http://10.0.4.4:8848}"
NACOS_USER="${4:-nacos}"
NACOS_PASS="${5:-nacos}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/ticket-platform-secrets-${ENV}.yml"
DATA_ID="ticket-platform-secrets.yaml"
GROUP="DEFAULT_GROUP"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "[ERROR] 配置文件不存在: $CONFIG_FILE"
    echo "请先基于模板文件填写真实配置值"
    exit 1
fi

echo "============================================"
echo " Nacos 配置导入工具"
echo "============================================"
echo " 环境:       $ENV"
echo " Nacos地址:  $NACOS_ADDR"
echo " 命名空间:   $NS_ID"
echo " 用户:       $NACOS_USER"
echo " Data ID:    $DATA_ID"
echo " Group:      $GROUP"
echo " 配置文件:   $CONFIG_FILE"
echo "============================================"

if grep -q '<YOUR_' "$CONFIG_FILE"; then
    echo ""
    echo "[WARNING] 配置文件中仍存在未替换的占位符:"
    grep '<YOUR_' "$CONFIG_FILE" | head -20
    echo ""
    read -p "是否继续导入? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "已取消导入"
        exit 0
    fi
fi

echo ""
echo "[1/2] 正在登录 Nacos..."
LOGIN_RESP=$(curl -s -X POST "${NACOS_ADDR}/nacos/v1/auth/login" \
    -d "username=${NACOS_USER}&password=${NACOS_PASS}")

ACCESS_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(data.get('accessToken', ''))
except:
    print('')
" 2>/dev/null || echo "")

if [ -z "$ACCESS_TOKEN" ]; then
    echo "[WARNING] 无法获取 accessToken，可能 Nacos 未开启鉴权，将尝试无鉴权导入"
    AUTH_PARAM=""
else
    echo "[OK] 登录成功"
    AUTH_PARAM="&accessToken=${ACCESS_TOKEN}"
fi

echo ""
echo "[2/2] 正在发布配置到命名空间 ${NS_ID}..."

CONTENT=$(cat "$CONFIG_FILE")

PUBLISH_RESP=$(curl -s -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${DATA_ID}" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "content=${CONTENT}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "tenant=${NS_ID}" \
    -d "${AUTH_PARAM#&}")

if echo "$PUBLISH_RESP" | grep -qi "true"; then
    echo "[OK] 配置发布成功!"
else
    echo "[ERROR] 配置发布失败: $PUBLISH_RESP"
    exit 1
fi

echo ""
echo "============================================"
echo " 导入完成!"
echo "============================================"
echo ""
echo "bootstrap.yml 中已配置 Nacos 地址和命名空间，直接启动即可。"
echo ""
