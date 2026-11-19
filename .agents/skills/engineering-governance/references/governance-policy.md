# Governance policy

## State contract

| State | Entry condition | Exit evidence |
|---|---|---|
| `PROPOSED` | User request is received | Requirement record exists or an ambiguity is recorded |
| `DISCOVERED` | Repository and working tree inspected | Discovery summary identifies target branch, instructions, tools, and existing work |
| `ANALYZED` | Scope and acceptance criteria are explicit | Requirement record is traceable and open questions are resolved or listed |
| `DESIGNED` | Change is non-trivial or risk-bearing | Design records alternatives, affected surfaces, failure recovery, and tests |
| `PLANNED` | Design is ready for implementation | Tasks have dependencies, expected files, results, and validation |
| `IMPLEMENTING` | Isolated branch is ready | Commits contain only scoped changes |
| `QUALITY_CHECK` | Implementation is complete enough to test | Quality matrix has command, result, evidence, and notes for each applicable check |
| `READY_FOR_REVIEW` | Local checks and diff review are complete | Clean PR payload, no secrets, correct target branch, and complete description |
| `PR_OPEN` | PR was created | Platform query returns the PR URL/number and branch pair |
| `AI_REVIEW` | Final PR diff is available | Separate AI self-review records findings or explicitly says none found |
| `READY_TO_MERGE` | Latest head self-review is complete and GitHub gates are satisfied | Authorized merge is confirmed by GitHub |
| `MERGED` | GitHub shows merged status | Merge SHA is recorded and applicable post-merge workflows are identified |
| `RELEASING` | The change requires release or deployment | Release, deployment, and live revision are verified |
| `PRODUCTION_VERIFIED` | Expected revision or digest is live | Health and scoped smoke results are recorded |
| `DELIVERED` | Requested endpoint is verified or inapplicable steps are explained | Delivery record names actual results, limitations, and follow-ups |

`BLOCKED`, `CHANGES_REQUESTED`, `CI_FAILED`, `CONFLICTED`, and `CANCELLED` are holding states. Each record must state the cause, impact, owner or dependency, and the condition that permits resumption or closure.

## User work protection

Before editing, preserve the output of `git status --short` and identify each pre-existing change. Work around unrelated changes instead of resetting, cleaning, overwriting, or stashing them without explicit approval. If ownership is unclear, use an isolated worktree. Ask only if the change cannot proceed safely in isolation.

## Governance changes

Document the rationale, validation, and impact of changes to this skill, required CI checks, CODEOWNERS, or approval policy. Keep independent governance changes in their own PR. Apply platform settings only when explicitly authorized and with evidence that the proposed checks and reviewers are available.

## Platform limits

If GitHub, `gh`, credentials, a required service, or a device is unavailable, continue checks that do not depend on it and mark the unavailable check `BLOCKED`. Never claim a PR, approval, merge, test, or deployment that was not observed.
