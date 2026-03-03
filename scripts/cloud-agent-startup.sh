#!/usr/bin/env bash

set -euo pipefail

MAVEN_VERSION="${MAVEN_VERSION:-3.9.11}"
MAVEN_ARCHIVE="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_ARCHIVE}"
MAVEN_HOME="/opt/apache-maven-${MAVEN_VERSION}"

JDK8_RELEASE_TAG="${JDK8_RELEASE_TAG:-jdk8u442-b06}"
JDK8_ARCHIVE="${JDK8_ARCHIVE:-OpenJDK8U-jdk_x64_linux_hotspot_8u442b06.tar.gz}"
JDK8_URL="https://github.com/adoptium/temurin8-binaries/releases/download/${JDK8_RELEASE_TAG}/${JDK8_ARCHIVE}"
JDK8_HOME="/opt/jdk8"

FRONTEND_DIR="${FRONTEND_DIR:-/workspace/miduo-frontend}"

log() {
  echo "[cloud-agent-startup] $*"
}

run_as_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
    return
  fi
  if command -v sudo >/dev/null 2>&1; then
    sudo "$@"
    return
  fi
  log "需要 root 权限执行命令，但当前环境没有 sudo：$*"
  exit 1
}

version_ge() {
  local current="$1"
  local required="$2"
  [ "$(printf '%s\n%s\n' "${required}" "${current}" | sort -V | awk 'NR==1 { print; exit }')" = "${required}" ]
}

get_java_version() {
  java -version 2>&1 | awk 'NR==1 { gsub(/"/, "", $3); print $3; exit }'
}

install_maven() {
  local current_version=""
  if command -v mvn >/dev/null 2>&1; then
    current_version="$(mvn -v 2>/dev/null | awk 'NR==1 { print $3; exit }')"
  fi

  if [ -n "${current_version}" ] && version_ge "${current_version}" "3.8.0"; then
    log "Maven 已满足要求：${current_version}"
    return
  fi

  log "安装 Maven ${MAVEN_VERSION} ..."
  local tmp_dir
  tmp_dir="$(mktemp -d)"

  curl -fsSL "${MAVEN_URL}" -o "${tmp_dir}/${MAVEN_ARCHIVE}"
  run_as_root rm -rf "${MAVEN_HOME}"
  run_as_root tar -xzf "${tmp_dir}/${MAVEN_ARCHIVE}" -C /opt
  run_as_root ln -sf "${MAVEN_HOME}/bin/mvn" /usr/local/bin/mvn

  rm -rf "${tmp_dir}"
  log "Maven 安装完成：$(mvn -v 2>/dev/null | awk 'NR==1 { print; exit }')"
}

install_jdk8() {
  local java_version=""
  if command -v java >/dev/null 2>&1; then
    java_version="$(get_java_version || true)"
  fi

  if [[ "${java_version}" == 1.8.* || "${java_version}" == 8* ]]; then
    log "JDK8 已满足要求：${java_version}"
    return
  fi

  log "安装 Temurin JDK8 ..."
  local tmp_dir
  local extracted_dir
  tmp_dir="$(mktemp -d)"

  curl -fsSL "${JDK8_URL}" -o "${tmp_dir}/${JDK8_ARCHIVE}"
  extracted_dir="$(tar -tzf "${tmp_dir}/${JDK8_ARCHIVE}" | awk -F/ 'NR==1 { print $1; exit }')"

  run_as_root tar -xzf "${tmp_dir}/${JDK8_ARCHIVE}" -C /opt
  run_as_root rm -rf "${JDK8_HOME}"
  run_as_root mv "/opt/${extracted_dir}" "${JDK8_HOME}"
  run_as_root ln -sf "${JDK8_HOME}/bin/java" /usr/local/bin/java
  run_as_root ln -sf "${JDK8_HOME}/bin/javac" /usr/local/bin/javac
  run_as_root tee /etc/profile.d/jdk8.sh >/dev/null <<EOF
export JAVA_HOME=${JDK8_HOME}
export PATH=\$JAVA_HOME/bin:\$PATH
EOF

  rm -rf "${tmp_dir}"
  log "JDK8 安装完成：$(java -version 2>&1 | awk 'NR==1 { print; exit }')"
}

install_frontend_dependencies() {
  if [ ! -d "${FRONTEND_DIR}" ]; then
    log "前端目录不存在，跳过 npm install：${FRONTEND_DIR}"
    return
  fi

  log "执行前端依赖安装：${FRONTEND_DIR} -> npm install"
  (
    cd "${FRONTEND_DIR}"
    npm install
  )
}

print_summary() {
  local maven_line
  local java_line
  local node_line
  local npm_line

  maven_line="$(mvn -v 2>/dev/null | awk 'NR==1 { print; exit }')"
  java_line="$(java -version 2>&1 | awk 'NR==1 { print; exit }')"
  node_line="$(node -v 2>/dev/null || echo 'node: 未安装')"
  npm_line="$(npm -v 2>/dev/null || echo 'npm: 未安装')"

  log "环境就绪："
  log "  ${java_line}"
  log "  ${maven_line}"
  log "  node ${node_line}"
  log "  npm ${npm_line}"
}

main() {
  install_maven
  install_jdk8
  install_frontend_dependencies
  print_summary
}

main "$@"
