# PR, review, and delivery policy

## PR gate

Before opening a PR, inspect the final diff and file list, scan for secrets and unrelated files, verify the target branch, and confirm commit subjects across the full source-to-target range. Use the repository's existing template when one exists. Include the requirement ID or local record path, acceptance criteria, implementation summary, exact checks, risk, rollback, and known limitations.

## Independent AI review

Review the final diff as a reviewer after implementation. Record the base/head SHA, reviewer context or agent, files and checks in scope, and untested areas. Check correctness, security, reliability, performance, maintainability, compatibility, tests, regressions, scope, and documentation. Record each finding with an ID, severity (`P0` to `P3`), file and line, evidence, impact, recommendation, and blocking status. Do not invent issues. If none are found, state the reviewed scope and untested areas.

AI review is not a platform approval. The AI must not submit an approval on its own PR, dismiss required reviews, enable auto-merge, merge, or change repository protection.

## Human gate and delivery

Wait for an authorized human approval, passing required status checks, resolved required conversations, a clean merge state, the correct target branch, and disabled auto-merge. The bundled PR script checks a deterministic subset and cannot prove conversation resolution or platform policy. A requested change returns the work to implementation and then to quality checks and AI review. After a human merges the PR, query the platform and record the PR number/URL, source and target branches, commit SHAs, merge SHA, CI results, AI review summary, human approval state, delivered scope, limitations, and follow-up tasks. Until this evidence exists, use `WAITING_FOR_HUMAN` or `BLOCKED`, not `DELIVERED`.
