---
name: engineering-governance
description: Own Nexus engineering delivery end to end, from requirements, design and implementation to AI self-review, verification, commits, PR, merge, release, production deployment and live verification. Use when asked to develop, fix, review, release, deploy or change engineering governance in Nexus.
---

# Engineering Governance for Nexus

Use this workflow for every change, including one-line documentation and governance edits. The goal is a reviewable, reversible change with evidence that another engineer can audit.

## AI owns delivery

Treat an implementation request as end-to-end delegation within the agreed scope. Make routine decisions, run the workflow, fix recoverable failures, and proceed without asking the user to perform routine steps or approve each stage. Own requirements, design, implementation, self-review, testing, Git delivery, CI, PR review and resolution, merging, release, production deployment, smoke checks, and recovery when applicable. Honor explicit scope limits such as review-only, local-only, or a request to skip monitoring a long non-blocking suite.

## Non-negotiable rules

- Every change uses a requirement or task identifier and a dedicated pull request into the protected target branch. If no issue tracker is available, create a local requirement record in the task workspace and link it from the PR.
- Inspect the repository, applicable `AGENTS.md`, branch, remotes, working tree, relevant code, and tests before editing. Preserve user changes. Never use `git reset --hard`, `git clean`, an unapproved overwrite, or an unapproved stash.
- Do not develop on `main` or another protected branch. Follow existing branch naming. If no naming rule exists, use `feature/`, `fix/`, `refactor/`, `perf/`, `docs/`, `test/`, `build/`, `ci/`, `chore/`, or `hotfix/` plus a short description.
- Keep commits atomic and use the repository's existing style. Nexus history uses Conventional Commit-style subjects.
- Do not skip, weaken, delete, or fake tests, hooks, CI, branch protection, or review requirements. Do not use `--no-verify` or force-push shared branches.
- Use the authenticated identity and repository-supported automation for authorized platform actions. GitHub rules, environment policies, and the identity's current permissions determine approval, merge, and deployment eligibility. Record AI review as AI review and let GitHub enforce reviewer eligibility.
- Report `PASS`, `FAIL`, `BLOCKED`, and `N/A` from evidence. Close an implementation task only at its requested endpoint, with a verified production revision and smoke result when deployment applies.

## Workflow and evidence

Move through these states and record the current state in the task notes:

`PROPOSED → DISCOVERED → ANALYZED → DESIGNED → PLANNED → IMPLEMENTING → QUALITY_CHECK → READY_FOR_REVIEW → PR_OPEN → AI_REVIEW → READY_TO_MERGE → MERGED → RELEASING → PRODUCTION_VERIFIED → DELIVERED`

Use `BLOCKED`, `CHANGES_REQUESTED`, `CI_FAILED`, `CONFLICTED`, or `CANCELLED` with a reason, impact, and exit condition when needed. After a requested change or CI failure, rerun affected checks and a separate self-review pass.

1. **Discover.** Run `git status --short --branch`, inspect `HEAD`, remotes, target branch, `AGENTS.md`, README and relevant project docs. Read the project profile and produce a discovery summary before editing. Identify Java/Spring Boot/Maven modules, CI, PR templates, CODEOWNERS, and any existing issue or branch. Do not claim branch protection or GitHub state without querying it.
2. **Analyze.** Capture background, problem, objective, scope, exclusions, acceptance criteria, constraints, dependencies, risks, and open questions. Record exact domain-specific behavior in the requirement and verify it against current source, tests, or logs before implementation.
3. **Design and plan.** For non-trivial work record current architecture, proposed solution, alternatives, affected files, interfaces, failure recovery, security, performance, observability, tests, rollback, and migration. Break implementation into tasks with dependencies, expected files, result, and validation.
4. **Implement.** Reuse existing Spring Boot patterns, DTO and validation conventions, Flyway migration practices, and logging/error handling. Keep unrelated changes out of the branch. Edit and test locally, then transfer a reviewed, scoped patch to an isolated branch or worktree of `liwei@47.122.123.12:/srv/blog/apps/api`. Run `git apply --check` before applying. Commit and push there, then use the published head branch when opening the GitHub PR.
5. **Quality check.** Use the Nexus matrix in [references/quality-matrix.md](references/quality-matrix.md). Run applicable commands and keep their real output or artifact path in the quality record. Treat CI-only checks as `BLOCKED` or `N/A` with an explanation, never as `PASS`.
6. **Open and review the PR.** Use [templates/pull-request.md](templates/pull-request.md), include requirement and acceptance criteria, exact commands and outcomes, risk, rollback, and known limitations. Query the platform to confirm the PR exists. Re-read the final diff in a separate self-review pass using [references/review-and-delivery.md](references/review-and-delivery.md), then record findings by severity and file/line.
7. **Complete the PR.** Track required CI and review feedback for the reviewed head. Resolve actionable feedback, rerun affected checks, and review the updated diff. Check approval eligibility and exercise authorized review and merge actions when GitHub permits them. Use a normal merge or configured repository automation after all required gates pass. If the platform requires an eligible approval the available identity cannot provide, record the exact gate as `BLOCKED` and continue any unaffected work.
8. **Release, deploy, and verify.** Follow the versioning, release, and production workflows. Trigger or advance them when needed and authorized, verify the deployed revision or digest and applicable smoke checks, and use the established recovery path for failures. Resume from the gate when it clears. Record evidence for the merge, release, deployment, and live verification. For a change with no release or runtime impact, record why production verification is `N/A`.

## Decisions and blockers

Resolve routine ambiguity from repository evidence and state material assumptions. Isolate work to protect existing modifications. Escalate only when a consequential decision, missing permission, required platform approval, or irreversible action cannot be resolved within the user's authorization. Identify the blocked step, evidence, impact, and minimum action needed while completing the rest of the work.

## Load project-specific guidance as needed

- Read [references/project-profile.md](references/project-profile.md) before discovery and produce [templates/discovery-summary.md](templates/discovery-summary.md). Read [references/quality-matrix.md](references/quality-matrix.md) before verification.
- Use [references/governance-policy.md](references/governance-policy.md) for state transitions and safety boundaries.
- Use [references/review-and-delivery.md](references/review-and-delivery.md) for PR, AI review, platform gates, and production evidence.
- Start records from [templates/](templates/). Run the deterministic checks in [scripts/](scripts/) before reporting repository state.
