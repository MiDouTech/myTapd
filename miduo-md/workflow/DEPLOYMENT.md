# 部署指南 — 米多工单系统

> 本文档覆盖：GitHub Actions CI/CD 流水线配置 → GHCR 制品库 → 生产环境 Docker Compose 部署。

---

## 目录

1. [整体流程概览](#1-整体流程概览)
2. [前置条件](#2-前置条件)
3. [GitHub 仓库配置](#3-github-仓库配置)
4. [触发构建](#4-触发构建)
5. [查看制品](#5-查看制品)
6. [生产服务器部署](#6-生产服务器部署)
7. [更新版本](#7-更新版本)
8. [常见问题排查](#8-常见问题排查)

---

## 1. 整体流程概览

```
┌─────────────┐  git push/tag   ┌──────────────────────────────────┐
│  开发者本地  │ ──────────────► │       GitHub Actions              │
└─────────────┘                 │                                   │
                                │  Job 1: build-backend             │
                                │    ① mvn clean package            │
                                │    ② docker build (JDK 8)         │
                                │    ③ push → ghcr.io/*/backend     │
                                │                                   │
                                │  Job 2: build-frontend            │
                                │    ① npm ci + npm run build       │
                                │    ② docker build (nginx)         │
                                │    ③ push → ghcr.io/*/frontend    │
                                └──────────────┬───────────────────┘
                                               │ image ready
                                               ▼
                                ┌──────────────────────────────────┐
                                │       生产服务器                  │
                                │  docker compose pull             │
                                │  docker compose up -d            │
                                │                                  │
                                │  [mysql] [redis] [minio]         │
                                │  [backend :8080] → Flyway 自动迁移│
                                │  [frontend :80]  → nginx 反代    │
                                └──────────────────────────────────┘
```

---

## 2. 前置条件

| 条件 | 说明 |
|---|---|
| GitHub 账号 | 仓库必须托管在 GitHub |
| 生产服务器 | Linux，已安装 Docker Engine 24+ 和 Docker Compose v2 |
| 域名（可选）| 如需 HTTPS，需配置 Nginx/Traefik 反代 + SSL 证书 |

---

## 3. GitHub 仓库配置

### 3.1 开启 GHCR 写入权限

Actions 默认已有 `GITHUB_TOKEN`，但需要确认仓库的 Package 写入权限已开启：

1. 进入 GitHub 仓库页面
2. **Settings → Actions → General**
3. 找到 **Workflow permissions**，选择 **Read and write permissions**
4. 点击 **Save**

### 3.2 配置 Repository Secrets

进入 **Settings → Secrets and variables → Actions → Secrets**，添加以下 Secret：

| Secret 名称 | 说明 | 是否必填 |
|---|---|---|
| `VITE_WECOM_OAUTH_URL` | 企微 OAuth 登录跳转 URL | 否（不配置则不显示企微登录） |

> **注意**：`GITHUB_TOKEN` 是 Actions 内置的，**不需要手动创建**。

---

## 4. 触发构建

### 自动触发（推荐）

```bash
# 推送到 main 分支 → 构建 latest 镜像
git push origin main

# 打版本标签 → 构建版本化镜像（如 v1.2.0）
git tag v1.2.0
git push origin v1.2.0
```

### 生成的镜像 Tag 规则

| 触发条件 | 生成的 Tag |
|---|---|
| 推送到 `main` | `latest`、`sha-abc1234` |
| 打标签 `v1.2.0` | `1.2.0`、`1.2`、`latest`、`sha-abc1234` |
| Pull Request | `pr-42`（**仅构建，不推送**） |

### 查看构建进度

GitHub 仓库 → **Actions** 标签页 → 选择最新的 workflow run。

---

## 5. 查看制品

构建成功后，镜像发布到 GHCR：

```
ghcr.io/<你的GitHub用户名>/ticket-platform-backend:latest
ghcr.io/<你的GitHub用户名>/ticket-platform-frontend:latest
```

在 GitHub 个人主页 → **Packages** 可以看到所有镜像和版本历史。

---

## 6. 生产服务器部署

### 6.1 准备工作（仅首次）

```bash
# 1. SSH 登录服务器
ssh user@your-server-ip

# 2. 安装 Docker（如未安装）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 重新登录生效

# 3. 创建部署目录
mkdir -p ~/ticket-platform && cd ~/ticket-platform

# 4. 下载 docker-compose.prod.yml（从仓库获取）
# 方式A：直接从 GitHub Raw 下载
curl -fsSL https://raw.githubusercontent.com/<你的用户名>/<仓库名>/main/ticket-platform/deployment/docker/docker-compose.prod.yml \
  -o docker-compose.prod.yml

# 方式B：用 git clone 整个仓库，然后 cd 到对应目录
```

### 6.2 创建环境变量文件

```bash
# 复制模板并编辑
curl -fsSL https://raw.githubusercontent.com/<你的用户名>/<仓库名>/main/ticket-platform/deployment/docker/.env.prod.example \
  -o .env.prod

vim .env.prod   # 填入所有必要的值
```

**必须修改的关键字段：**

```dotenv
GHCR_OWNER=你的GitHub用户名（小写）
IMAGE_TAG=latest

DB_ROOT_PASSWORD=强密码_至少16位
DB_PASSWORD=强密码_至少16位
REDIS_PASSWORD=强密码
JWT_SECRET=至少64位随机字符串
MINIO_ROOT_PASSWORD=强密码
```

### 6.3 登录 GHCR

```bash
# 使用 GitHub 个人访问令牌（PAT）登录
# PAT 需要 read:packages 权限
# 生成地址：GitHub → Settings → Developer settings → Personal access tokens

echo "<你的PAT>" | docker login ghcr.io -u <你的GitHub用户名> --password-stdin
```

### 6.4 启动所有服务

```bash
cd ~/ticket-platform

# 拉取最新镜像
docker compose -f docker-compose.prod.yml --env-file .env.prod pull

# 后台启动所有服务
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

**首次启动流程（自动执行）：**
1. MySQL 容器启动并初始化数据库
2. Backend 容器启动，Flyway 自动执行 V1~V12 数据库迁移脚本
3. Frontend 容器启动，Nginx 开始提供服务
4. 访问 `http://服务器IP` 即可

### 6.5 验证服务状态

```bash
# 查看所有容器状态
docker compose -f docker-compose.prod.yml --env-file .env.prod ps

# 查看后端健康状态
curl http://localhost:8080/actuator/health

# 查看后端日志
docker logs ticket-backend -f --tail 100

# 查看前端日志
docker logs ticket-frontend -f --tail 50
```

---

## 7. 更新版本

```bash
cd ~/ticket-platform

# 拉取新版镜像
docker compose -f docker-compose.prod.yml --env-file .env.prod pull backend frontend

# 滚动重启（零停机）
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-deps backend frontend
```

如果是数据库迁移版本，后端重启时 Flyway 会自动执行新的 Migration SQL，无需手动操作。

---

## 8. 常见问题排查

### 构建失败：Maven 编译错误

```bash
# 在 Actions 日志中查看具体报错，本地复现：
cd ticket-platform
mvn clean package -DskipTests -pl ticket-bootstrap -am
```

### 镜像推送失败：403 Forbidden

- 检查 Actions 设置中 **Workflow permissions** 是否设置为 **Read and write**
- 如果仓库是 Private，需确认 GHCR 包可见性

### 容器启动后后端无法连接 MySQL

```bash
docker logs ticket-backend | grep -i "error\|exception"
# 常见原因：DB_PASSWORD 填写错误，或 MySQL 还未 healthy
```

### Flyway 迁移失败

```bash
docker logs ticket-backend | grep -i "flyway\|migration"
# 迁移脚本有语法错误时，修复 SQL 后重新 push 触发构建
```

### 前端页面空白 / API 请求 404

- 确认 nginx.conf 中 `proxy_pass http://backend:8080/api/;` 配置正确
- 在前端容器内测试连通性：
  ```bash
  docker exec ticket-frontend wget -qO- http://backend:8080/actuator/health
  ```

### HTTPS 配置（可选）

推荐在服务器上额外部署 **Nginx + Certbot** 作为外层反代：

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:80;  # 转发到 ticket-frontend 容器
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 文件清单

| 文件 | 说明 |
|---|---|
| `.github/workflows/build-and-push.yml` | GitHub Actions 流水线 |
| `miduo-frontend/Dockerfile` | 前端多阶段构建（Node 20 → Nginx） |
| `miduo-frontend/nginx.conf` | Nginx 配置（SPA路由 + API代理 + WS代理） |
| `ticket-platform/deployment/docker/docker-compose.prod.yml` | 生产环境编排文件 |
| `ticket-platform/deployment/docker/.env.prod.example` | 环境变量模板 |
