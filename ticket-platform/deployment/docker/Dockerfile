FROM eclipse-temurin:8-jre-jammy

LABEL maintainer="miduo-team"

WORKDIR /app

COPY ticket-bootstrap/target/ticket-bootstrap-*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE="prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
