# Thin runtime image. Build the Boot jars on the host first:
#   mvn -DskipTests package
# Then: docker compose up --build
#
# curl is installed so Compose healthchecks can hit /actuator/health.
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
