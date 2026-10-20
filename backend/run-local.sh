#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

set -a
source ../.env
set +a

export QUEUE_HOST=localhost
export STORAGE_ENDPOINT="http://localhost:${MINIO_API_PORT:-9000}"
export STORAGE_PUBLIC_ENDPOINT="$STORAGE_ENDPOINT"
export SPRING_PROFILES_ACTIVE=local
export DB_URL="jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}"

exec mvn spring-boot:run

exit 0