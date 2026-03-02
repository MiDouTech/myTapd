#!/usr/bin/env bash

set -euo pipefail

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" || $# -lt 3 ]]; then
  cat <<'HELP'
用法：
  ./gen-wecom-oauth-url.sh <CorpID> <AgentID> <RedirectURI> [State]

示例：
  ./gen-wecom-oauth-url.sh ww1234567890 1000002 "https://test-ticket.company.com/login" "test-login"
HELP
  exit 0
fi

CORP_ID="$1"
AGENT_ID="$2"
REDIRECT_URI="$3"
STATE="${4:-ticket_state}"

urlencode() {
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"
}

ENCODED_REDIRECT="$(urlencode "$REDIRECT_URI")"

OAUTH_URL="https://open.work.weixin.qq.com/wwopen/sso/qrConnect?appid=${CORP_ID}&agentid=${AGENT_ID}&redirect_uri=${ENCODED_REDIRECT}&state=${STATE}"

echo "$OAUTH_URL"
