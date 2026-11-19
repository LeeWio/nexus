#!/usr/bin/env bash

# Read-only API smoke test for a running Nexus instance.
# Usage: BASE_URL=http://localhost:8080 ./test-api.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"
SLEEP_SECONDS="${SLEEP_SECONDS:-2}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required" >&2
  exit 127
fi

BASE_URL="${BASE_URL%/}"
if [[ ! "$MAX_ATTEMPTS" =~ ^[1-9][0-9]*$ ]]; then
  echo "MAX_ATTEMPTS must be a positive integer" >&2
  exit 2
fi

expect_ok() {
  local url="$1"
  local timeout="$2"
  local status_code
  status_code=$(curl --silent --show-error --connect-timeout 3 --max-time "$timeout" \
    -H 'Accept: application/json' --output /dev/null --write-out '%{http_code}' "$url") || return 1
  if [[ "$status_code" != 200 ]]; then
    echo "$url returned HTTP $status_code" >&2
    return 1
  fi
}

request() {
  local path="$1"
  local url="${BASE_URL}${path}"
  echo "GET ${url}"
  expect_ok "$url" 20
}

echo "Waiting for Nexus at ${BASE_URL}"
for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
  if expect_ok "${BASE_URL}/readyz" 5; then
    break
  fi
  if [[ "$attempt" == "$MAX_ATTEMPTS" ]]; then
    echo "Nexus did not become ready after ${MAX_ATTEMPTS} attempts" >&2
    exit 1
  fi
  sleep "$SLEEP_SECONDS"
done

request /livez
request /readyz
request /api/v1/public/categories
request /api/v1/public/tags
request /api/v1/public/blog/discovery

echo "Nexus API smoke test passed"
