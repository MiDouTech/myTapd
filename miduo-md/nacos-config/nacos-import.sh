#!/bin/bash
# ============================================================
# Nacos 配置一键导入脚本
# 用法: bash nacos-import.sh [环境] [nacos地址] [用户名] [密码]
#
# 参数说明:
#   环境     - dev / test / prod (必填)
#   nacos地址 - 如 http://127.0.0.1:8848 (可选，默认 http://127.0.0.1:8848)
#   用户名   - Nacos 用户名 (可选，默认 nacos)
#   密码     - Nacos 密码 (可选，默认 nacos)
#
# 示例:
#   bash nacos-import.sh dev
#   bash nacos-import.sh prod http://nacos.company.com:8848 admin SecretPass
# ============================================================

set -euo pipefail

ENV="${1:?请指定环境: dev / test / prod}"
NACOS_ADDR="${2:-http://127.0.0.1:8848}"
NACOS_USER="${3:-nacos}"
NACOS_PASS="${4:-nacos}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/ticket-platform-secrets-${ENV}.yml"
DATA_ID="ticket-platform-secrets.yml"
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
echo " 用户:       $NACOS_USER"
echo " Data ID:    $DATA_ID"
echo " Group:      $GROUP"
echo " 配置文件:   $CONFIG_FILE"
echo "============================================"

# 检查配置中是否还有未替换的占位符
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

# Step 1: 获取 Nacos accessToken
echo ""
echo "[1/3] 正在登录 Nacos..."
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

# Step 2: 检查或创建 Namespace
echo ""
echo "[2/3] 检查命名空间 '${ENV}'..."

# 获取已有命名空间列表
NS_LIST=$(curl -s "${NACOS_ADDR}/nacos/v1/console/namespaces?${AUTH_PARAM#&}")
NS_ID=$(echo "$NS_LIST" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    namespaces = data.get('data', [])
    for ns in namespaces:
        if ns.get('namespace') == '${ENV}' or ns.get('namespaceShowName') == '${ENV}':
            print(ns.get('namespace', ''))
            break
except:
    pass
" 2>/dev/null || echo "")

if [ -z "$NS_ID" ]; then
    echo "命名空间 '${ENV}' 不存在，正在创建..."
    CREATE_NS_RESP=$(curl -s -X POST "${NACOS_ADDR}/nacos/v1/console/namespaces" \
        -d "customNamespaceId=${ENV}&namespaceName=${ENV}&namespaceDesc=ticket-platform ${ENV} environment${AUTH_PARAM}")
    if echo "$CREATE_NS_RESP" | grep -qi "true"; then
        echo "[OK] 命名空间 '${ENV}' 创建成功"
        NS_ID="${ENV}"
    else
        echo "[WARNING] 创建命名空间失败: $CREATE_NS_RESP"
        echo "将使用默认命名空间 (public)"
        NS_ID=""
    fi
else
    echo "[OK] 命名空间 '${ENV}' 已存在 (ID: ${NS_ID})"
fi

# Step 3: 发布配置
echo ""
echo "[3/3] 正在发布配置..."

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
echo "启动应用时请设置以下环境变量:"
echo ""
echo "  export NACOS_SERVER_ADDR=${NACOS_ADDR#http://}"
echo "  export NACOS_NAMESPACE=${NS_ID}"
echo "  export NACOS_USERNAME=${NACOS_USER}"
echo "  export NACOS_PASSWORD=${NACOS_PASS}"
echo ""
echo "或者在启动命令中传入:"
echo ""
echo "  java -jar ticket-platform.jar \\"
echo "    -Dnacos.config.server-addr=${NACOS_ADDR#http://} \\"
echo "    -Dnacos.config.namespace=${NS_ID} \\"
echo "    -Dnacos.config.username=${NACOS_USER} \\"
echo "    -Dnacos.config.password=${NACOS_PASS} \\"
echo "    --spring.profiles.active=${ENV}"
echo ""
