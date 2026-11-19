# PR, review, and delivery policy

## PR gate

Before opening a PR, inspect the final diff and file list, scan for secrets and unrelated files, verify the target branch, and confirm commit subjects across the full source-to-target range. Use the repository's existing template when one exists. Include the requirement ID or local record path, acceptance criteria, implementation summary, exact checks, risk, rollback, and known limitations.

## AI self-review

Re-read the final diff in a separate review pass after implementation. The same agent can perform this pass. Record the base/head SHA, reviewer context or agent, files and checks in scope, and untested areas. Check correctness, security, reliability, performance, maintainability, compatibility, tests, regressions, scope, and documentation. Record each finding with an ID, severity (`P0` to `P3`), file and line, evidence, impact, recommendation, and blocking status. Do not invent issues. If none are found, state the reviewed scope and untested areas.

Record the result as AI review. Perform a formal GitHub review action only through an explicitly authorized, eligible identity where the active repository rules permit it. Author and last-pusher restrictions and any required separation of duties still apply.

## GitHub gates and merge

Query the current PR head SHA, target branch, rulesets, required checks, reviews, conversations, and merge state. Treat blocked or missing required checks as pending or blocked and fix actionable failures. If the branch needs updating, preserve other contributors' commits and verify the resulting head before merging. The bundled script checks GitHub’s aggregate merge state. In an unstable state, verify that only optional checks are outstanding. Re-read the platform state before merging and pin the operation to the reviewed head, for example with `gh pr merge --match-head-commit <sha>` where supported. Use permitted merge methods or the configured merge queue. If GitHub requires an approval the current identity cannot supply, record the exact gate and continue work that can proceed. Keep PR, release, and production environment gates distinct.

If the user authorizes the repository's existing PR-only administrator merge route, verify that it grants the current identity an exception to review only. Confirm that required checks, conversation resolution, and all other rules are satisfied. Use the platform's permitted merge action and record the administrator merge separately from any formal approval.

## Release and production verification

Confirm the platform's merge result and record the source and target branches, reviewed head, merge SHA, required CI, and review evidence. Track any applicable release PR, version or tag, image digest, and deployment run. Use the existing release and deploy workflows and their approval mechanisms. Verify the revision or digest actually served by production and run scoped smoke checks. Diagnose failures, apply established safe recovery, and verify the recovered state. Record the current blocker and resumption condition whenever a required gate is unavailable. Use `DELIVERED` only when the requested endpoint is evidenced.
