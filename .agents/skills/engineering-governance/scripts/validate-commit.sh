#!/usr/bin/env bash
set -euo pipefail

range=${1:-HEAD~1..HEAD}
if ! git rev-parse --verify "${range%%..*}" >/dev/null 2>&1 || ! git rev-parse --verify "${range##*..}" >/dev/null 2>&1; then
  echo "BLOCKED: commit range '$range' does not resolve" >&2
  exit 2
fi
bad=0
while IFS= read -r message; do
  if [[ ! "$message" =~ ^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9._/-]+\))?:\ [^[:space:]].*$ ]]; then
    echo "FAIL: $message" >&2
    bad=1
  else
    echo "PASS: $message"
  fi
done < <(git log --format=%s "$range")
if [[ "$bad" -ne 0 ]]; then exit 1; fi
