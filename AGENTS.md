# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview

This is **米多内部工单系统 (Miduo Internal Ticket Platform)** — a full-stack project with a Spring Boot backend and Vue 3 frontend. See `ticket-platform/README.md` and `miduo-frontend/README.md` for detailed documentation.

### Services

| Service | Port | How to start |
|---------|------|-------------|
| MySQL 8.0 | 3306 | `sudo docker compose -f /workspace/ticket-platform/deployment/docker/docker-compose.yml up -d` |
| Redis 7 | 6379 | (started with docker-compose above) |
| Backend (Spring Boot) | 8080 | `cd /workspace/ticket-platform && JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn spring-boot:run -pl ticket-bootstrap -Dspring-boot.run.profiles=dev` |
| Frontend (Vite dev) | 5173 | `cd /workspace/miduo-frontend && npm run dev` |

### Important Caveats

- **JDK 8 required**: The backend requires JDK 8 (installed at `/opt/jdk8`). Always set `JAVA_HOME=/opt/jdk8` and prepend `$JAVA_HOME/bin` to `PATH` before running Maven or Java commands. The system default is JDK 21 which will NOT work.
- **MySQL password mismatch**: Docker Compose creates MySQL with root password `root123456`, but `application-dev.yml` expects `miduo,./147`. After starting MySQL, run: `sudo docker exec ticket-mysql mysql -u root -proot123456 -e "ALTER USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'miduo,./147'; ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'miduo,./147'; FLUSH PRIVILEGES;"`
- **Docker daemon**: Must be started manually: `sudo dockerd &>/tmp/dockerd.log &` — wait a few seconds before running docker commands.
- **RabbitMQ not needed**: The health endpoint shows RabbitMQ as DOWN — this is expected. WeChat MQ is disabled by default (`wecom.mq.enabled=false`).
- **Flyway migrations**: Run automatically on backend startup. 9 migration scripts (V1–V9) create all tables.
- **Authentication**: Uses JWT with Enterprise WeChat OAuth. For API testing without WeChat, generate a JWT token using the secret from `application-dev.yml` (`miduo-ticket-platform-jwt-secret-key-2026-must-be-long-enough`).

### Lint / Test / Build

| Task | Command |
|------|---------|
| Frontend lint | `cd /workspace/miduo-frontend && npm run lint` |
| Frontend format check | `cd /workspace/miduo-frontend && npm run format` |
| Frontend build | `cd /workspace/miduo-frontend && npm run build` |
| Backend build | `cd /workspace/ticket-platform && JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn clean install -DskipTests` |
| Backend test | `cd /workspace/ticket-platform && JAVA_HOME=/opt/jdk8 PATH=$JAVA_HOME/bin:$PATH mvn test` |
