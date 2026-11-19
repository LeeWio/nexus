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
json=$(gh pr view "${1:-}" --json number,url,state,isDraft,baseRefName,headRefName,mergeStateStatus,reviewDecision,statusCheckRollup,reviews,author,body 2>"$err_file")
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
if pr.get("mergeStateStatus") in {"DIRTY", "UNKNOWN", "BEHIND"}:
    errors.append(f"merge state is {pr.get('mergeStateStatus')}")

checks = pr.get("statusCheckRollup") or []
failed = [c for c in checks if (c.get("conclusion") or c.get("state")) not in {"SUCCESS", "SKIPPED", "NEUTRAL"}]
if failed:
    errors.append(f"{len(failed)} status check(s) are not passing")

reviews = pr.get("reviews") or []
human_approvals = [r for r in reviews if r.get("state") == "APPROVED" and not (r.get("author") or {}).get("is_bot", False)]
if pr.get("reviewDecision") != "APPROVED" or not human_approvals:
    errors.append("an authorized human approval is not visible")

body = pr.get("body") or ""
if not re.search(r"(?i)(requirement|issue|related|task)[:#\\s-]", body):
    errors.append("PR body does not visibly link a requirement, issue, or task")
if errors:
    print("BLOCKED: " + "; ".join(errors), file=sys.stderr)
    sys.exit(1)
print(f"PASS: PR #{pr['number']} {pr['url']} {pr['headRefName']} -> {pr['baseRefName']}")
print("Human approval and passing status checks are visible. Confirm resolved conversations on the platform before merge.")
PY
