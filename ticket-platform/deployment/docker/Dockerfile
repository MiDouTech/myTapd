FROM eclipse-temurin:8-jre-jammy

LABEL maintainer="miduo-team"

WORKDIR /app

COPY ticket-bootstrap/target/ticket-bootstrap-*.jar app.jar

# 动态页数据源：生产容器通常没有 Git 元数据，构建时把更新日志和周报打进镜像。
COPY deployment/docker/repo-data/ /data/repo/

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djdk.tls.client.protocols=TLSv1.2"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV UPDATE_CENTER_REPO_ROOT="/data/repo"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
