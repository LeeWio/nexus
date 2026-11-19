#!/usr/bin/env bash
set -euo pipefail

branch=${1:-$(git symbolic-ref --quiet --short HEAD || true)}
target=${2:-main}
if [[ -z "$branch" ]]; then echo "BLOCKED: detached HEAD" >&2; exit 2; fi
if [[ "$target" == main || "$target" == master ]]; then :; else echo "BLOCKED: target '$target' is not an expected protected branch" >&2; exit 2; fi
if [[ "$branch" == main || "$branch" == master ]]; then echo "FAIL: '$branch' is protected" >&2; exit 1; fi
if [[ ! "$branch" =~ ^(feature|fix|refactor|perf|docs|test|build|ci|chore|hotfix|codex|dependabot)/[a-z0-9][a-z0-9._/-]*$ ]]; then
  echo "FAIL: '$branch' does not follow the supported project or platform prefixes" >&2
  exit 1
fi
printf 'PASS: head=%s target=%s\n' "$branch" "$target"
