#!/usr/bin/env bash
# Package Boot jars, then start the full Docker Compose stack.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed or not on PATH." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker Desktop (or another engine) is not running." >&2
  exit 1
fi

if docker compose -f docker-compose.kafka.yml ps --status running 2>/dev/null | grep -q kafka; then
  echo "docker-compose.kafka.yml is already using port 9092."
  echo "Stop it first: docker compose -f docker-compose.kafka.yml down"
  exit 1
fi

echo "Packaging jars (skip tests)..."
mvn -DskipTests package

echo "Starting Compose stack..."
docker compose up --build -d

echo
docker compose ps
echo
echo "Gateway: http://localhost:8080"
echo "Eureka:  http://localhost:8761"
echo "MySQL:   localhost:${MYSQL_PUBLISH_PORT:-3307} (container port 3306)"
echo
echo "Logs:    docker compose logs -f"
echo "Stop:    docker compose down"
echo "Wipe DB: docker compose down -v"
