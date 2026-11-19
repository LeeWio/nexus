#!/usr/bin/env bash
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  echo "BLOCKED: GitHub CLI (gh) is not installed" >&2
  exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "BLOCKED: python3 is required to inspect GitHub JSON" >&2
  exit 2
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "BLOCKED: gh is not authenticated" >&2
  exit 2
fi

err_file=$(mktemp)
set +e
json=$(gh pr view "${1:-}" --json number,url,state,isDraft,baseRefName,headRefName,mergeStateStatus,reviewDecision,body 2>"$err_file")
status=$?
set -e
if [[ "$status" -ne 0 ]]; then
  echo "BLOCKED: could not resolve a PR for this checkout" >&2
  cat "$err_file" >&2 || true
  rm -f "$err_file"
  exit 2
fi
rm -f "$err_file"

python3 - "$json" <<'PY'
import json
import re
import sys

pr = json.loads(sys.argv[1])
errors = []
if pr.get("state") != "OPEN":
    errors.append(f"PR state is {pr.get('state')}")
if pr.get("isDraft"):
    errors.append("PR is still a draft")
if pr.get("baseRefName") not in {"main", "master"}:
    errors.append(f"unexpected base branch {pr.get('baseRefName')}")
if pr.get("headRefName") in {"main", "master"}:
    errors.append("head branch is protected")
if pr.get("mergeStateStatus") not in {"CLEAN", "UNSTABLE"}:
    errors.append(f"merge state is {pr.get('mergeStateStatus')}")

if pr.get("reviewDecision") in {"REVIEW_REQUIRED", "CHANGES_REQUESTED"}:
    errors.append(f"review state is {pr.get('reviewDecision')}")

body = pr.get("body") or ""
if not re.search(r"(?i)(requirement|issue|related|task)[:#\s-]", body):
    errors.append("PR body does not visibly link a requirement, issue, or task")
if errors:
    print("BLOCKED: " + "; ".join(errors), file=sys.stderr)
    sys.exit(1)
print(f"PASS: PR #{pr['number']} {pr['url']} {pr['headRefName']} -> {pr['baseRefName']}")
print("PR readiness checks passed. Confirm required contexts, conversations, and the reviewed head on GitHub before merge.")
PY
