#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHANGELOG_FILE="${ROOT_DIR}/CHANGELOG.md"
FRAGMENT_DIR="${ROOT_DIR}/changelogs"
RELEASE_DATE="${RELEASE_DATE:-$(TZ="${TZ:-Asia/Shanghai}" date +%F)}"
RELEASE_VERSION="${1:-${RELEASE_VERSION:-}}"

if [[ -z "${RELEASE_VERSION}" ]]; then
  if [[ -n "${GITHUB_REF_NAME:-}" && "${GITHUB_REF_NAME}" == v* ]]; then
    RELEASE_VERSION="${GITHUB_REF_NAME}"
  elif [[ -n "${GITHUB_SHA:-}" ]]; then
    RELEASE_VERSION="production-${RELEASE_DATE}-sha-${GITHUB_SHA:0:7}"
  else
    RELEASE_VERSION="production-${RELEASE_DATE}-manual"
  fi
fi

if [[ ! -d "${FRAGMENT_DIR}" ]]; then
  echo "No changelogs directory found, skip archive."
  exit 0
fi

shopt -s nullglob
fragments=("${FRAGMENT_DIR}"/*.md)
shopt -u nullglob

if [[ ${#fragments[@]} -eq 0 ]]; then
  echo "No changelog fragments found, skip archive."
  exit 0
fi

tmp_section="$(mktemp)"
{
  echo "## [${RELEASE_VERSION}] - ${RELEASE_DATE}"
  echo
  for fragment in "${fragments[@]}"; do
    file_name="$(basename "${fragment}")"
    if [[ "${file_name}" =~ ^([0-9]{4}-[0-9]{2}-[0-9]{2}) ]]; then
      fragment_date="${BASH_REMATCH[1]}"
    else
      fragment_date="${RELEASE_DATE}"
    fi

    echo "### ${fragment_date}"
    echo
    echo "| 类型 | 模块 | 描述 |"
    echo "|---|---|---|"
    awk '
      function trim(value) {
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        return value
      }
      /^\|/ {
        line = $0
        gsub(/\r$/, "", line)
        n = split(line, parts, "|")
        if (n < 5) next
        first = trim(parts[2])
        if (first == "类型" || first == "type" || first ~ /^-+$/) next
        print line
      }
    ' "${fragment}"
    echo
  done
} > "${tmp_section}"

tmp_changelog="$(mktemp)"
if [[ -s "${CHANGELOG_FILE}" ]]; then
  first_line="$(sed -n '1p' "${CHANGELOG_FILE}")"
  {
    echo "${first_line}"
    echo
    cat "${tmp_section}"
    sed '1d' "${CHANGELOG_FILE}" | sed '/./,$!d'
  } > "${tmp_changelog}"
else
  {
    echo "# 更新日志"
    echo
    cat "${tmp_section}"
  } > "${tmp_changelog}"
fi

mv "${tmp_changelog}" "${CHANGELOG_FILE}"
rm -f "${tmp_section}"
rm -f "${fragments[@]}"

echo "Archived ${#fragments[@]} changelog fragment(s) into ${CHANGELOG_FILE} as ${RELEASE_VERSION}."
