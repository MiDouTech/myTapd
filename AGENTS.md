# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview

This is **米多内部工单系统 (Miduo Internal Ticket Platform)** — a full-stack project with a Spring Boot backend and Vue 3 frontend. See `ticket-platform/README.md` and `miduo-frontend/README.md` for detailed documentation.

### Services

| Service | Port | How to start |
|---------|------|-------------|
| MySQL 8.0 | 3306 | `sudo docker compose -f /workspace/ticket-platform/deployment/docker/docker-compose.yml up -d` |
| Redis 7 | 6379 | (started with docker-compose above) |
| Backend (Spring Boot) | 8080 | `cd /workspace/ticket-platform && JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn spring-boot:run -pl ticket-bootstrap -Dspring-boot.run.profiles=dev -Djdk.tls.client.protocols=TLSv1.2` |
| Frontend (Vite dev) | 5173 | `cd /workspace/miduo-frontend && npm run dev` |

### Important Caveats

- **JDK 8 required**: The backend requires JDK 8 (installed at `/opt/jdk8`). Always set `JAVA_HOME=/opt/jdk8` and prepend `$JAVA_HOME/bin` to `PATH` before running Maven or Java commands. The system default is JDK 21 which will NOT work.
- **MySQL password**: Docker Compose creates MySQL with root password `root123456`. The `application-dev.yml` reads the password from Nacos or `DATASOURCE_PASSWORD` env var. After starting MySQL, set the password to match your Nacos config: `sudo docker exec ticket-mysql mysql -u root -proot123456 -e "ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY '<YOUR_DB_PASSWORD>'; ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '<YOUR_DB_PASSWORD>'; FLUSH PRIVILEGES;"`
- **Docker daemon**: Must be started manually: `sudo dockerd &>/tmp/dockerd.log &` — wait a few seconds before running docker commands.
- **Qiniu TLS fix**: Backend startup must include `-Djdk.tls.client.protocols=TLSv1.2` JVM arg (already included in the startup command above). JDK 8u341+ enables TLS 1.3 by default, but OkHttp 3.14.4 (bundled in Qiniu SDK 7.14.0) has a cipher suite compatibility issue with JDK 8's TLS 1.3 implementation when connecting to `up.qiniup.com`. Forcing TLSv1.2 resolves the `SSLHandshakeException: handshake_failure`.
- **Flyway migrations**: Run automatically on backend startup. 9 migration scripts (V1–V9) create all tables.
- **Authentication**: Uses JWT with Enterprise WeChat OAuth. For API testing without WeChat, generate a JWT token using the secret configured in Nacos (key: `jwt.secret`). Sensitive configs are no longer stored in application yml files - see `miduo-md/nacos-config/README.md`.

### Lint / Test / Build

| Task | Command |
|------|---------|
| Frontend lint | `cd /workspace/miduo-frontend && npm run lint` |
| Frontend format check | `cd /workspace/miduo-frontend && npm run format` |
| Frontend build | `cd /workspace/miduo-frontend && npm run build` |
| Backend build | `cd /workspace/ticket-platform && JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn clean install -DskipTests` |
| Backend test | `cd /workspace/ticket-platform && JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn test` |
