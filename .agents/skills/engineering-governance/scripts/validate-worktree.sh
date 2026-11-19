#!/usr/bin/env bash
set -euo pipefail

branch=$(git symbolic-ref --quiet --short HEAD || true)
if [[ -z "$branch" ]]; then
  echo "BLOCKED: detached HEAD" >&2
  exit 2
fi
printf 'Branch: %s\n' "$branch"
printf 'HEAD: %s\n' "$(git rev-parse HEAD)"
status=$(git status --porcelain)
if [[ -n "$status" ]]; then
  echo "$status"
  echo "BLOCKED: classify pre-existing changes before editing; this check never approves a dirty worktree" >&2
  exit 2
fi
if [[ "$branch" == main || "$branch" == master ]]; then
  echo "BLOCKED: work is on protected branch '$branch'; create an isolated branch" >&2
  exit 2
fi
echo "PASS: clean isolated worktree"
