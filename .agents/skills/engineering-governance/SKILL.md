---
name: engineering-governance
description: Govern every Nexus repository change from traceable requirement through isolated branch, implementation, real quality evidence, pull request, independent AI review, human approval, and verified delivery. Use this skill whenever the user asks to change code, tests, documentation, configuration, dependencies, CI, infrastructure, repository metadata, or this skill itself.
---

# Engineering Governance for Nexus

Use this workflow for every change, including one-line documentation and governance edits. The goal is a reviewable, reversible change with evidence that another engineer can audit.

## Non-negotiable rules

- Every change uses a requirement or task identifier and a dedicated pull request into the protected target branch. If no issue tracker is available, create a local requirement record in the task workspace and link it from the PR.
- Inspect the repository, applicable `AGENTS.md`, branch, remotes, working tree, relevant code, and tests before editing. Preserve user changes. Never use `git reset --hard`, `git clean`, an unapproved overwrite, or an unapproved stash.
- Do not develop on `main` or another protected branch. Follow existing branch naming. If no naming rule exists, use `feature/`, `fix/`, `refactor/`, `perf/`, `docs/`, `test/`, `build/`, `ci/`, `chore/`, or `hotfix/` plus a short description.
- Keep commits atomic and use the repository's existing style. Nexus history uses Conventional Commit-style subjects.
- Do not skip, weaken, delete, or fake tests, hooks, CI, branch protection, or review requirements. Do not use `--no-verify` or force-push shared branches.
- AI review is advisory. Only a human with repository permission can approve and merge. The AI must not approve its own PR, enable auto-merge, merge, or change governance settings to make a PR pass.
- Report `PASS`, `FAIL`, `BLOCKED`, and `N/A` honestly. A local change, pushed branch, or open PR is not delivered until the platform shows a human-approved, merged PR.

## Workflow and evidence

Move through these states and record the current state in the task notes:

`PROPOSED → DISCOVERED → ANALYZED → DESIGNED → PLANNED → IMPLEMENTING → QUALITY_CHECK → READY_FOR_REVIEW → PR_OPEN → AI_REVIEW → WAITING_FOR_HUMAN → APPROVED → MERGED → DELIVERED`

Use `BLOCKED`, `CHANGES_REQUESTED`, `CI_FAILED`, `CONFLICTED`, or `CANCELLED` with a reason, impact, and exit condition when needed. After a requested change or CI failure, rerun affected checks and the independent review.

1. **Discover.** Run `git status --short --branch`, inspect `HEAD`, remotes, target branch, `AGENTS.md`, README and relevant project docs. Read the project profile and produce a discovery summary before editing. Identify Java/Spring Boot/Maven modules, CI, PR templates, CODEOWNERS, and any existing issue or branch. Do not claim branch protection or GitHub state without querying it.
2. **Analyze.** Capture background, problem, objective, scope, exclusions, acceptance criteria, constraints, dependencies, risks, and open questions. Record exact domain-specific behavior in the requirement and verify it against current source, tests, or logs before implementation.
3. **Design and plan.** For non-trivial work record current architecture, proposed solution, alternatives, affected files, interfaces, failure recovery, security, performance, observability, tests, rollback, and migration. Break implementation into tasks with dependencies, expected files, result, and validation.
4. **Implement.** Reuse existing Spring Boot patterns, DTO and validation conventions, Flyway migration practices, and logging/error handling. Keep unrelated changes out of the branch. Edit and test locally, then transfer a reviewed, scoped patch to an isolated branch or worktree of `liwei@47.122.123.12:/srv/blog/apps/api`. Run `git apply --check` before applying. Commit and push there, then use the published head branch when opening the GitHub PR.
5. **Quality check.** Use the Nexus matrix in [references/quality-matrix.md](references/quality-matrix.md). Run applicable commands and keep their real output or artifact path in the quality record. Treat CI-only checks as `BLOCKED` or `N/A` with an explanation, never as `PASS`.
6. **Open and review the PR.** Use [templates/pull-request.md](templates/pull-request.md), include requirement and acceptance criteria, exact commands and outcomes, risk, rollback, and known limitations. Query the platform to confirm the PR exists. Re-read the final diff as an independent reviewer using [references/review-and-delivery.md](references/review-and-delivery.md), then record findings by severity and file/line.
7. **Wait for the human gate.** Track CI and review comments when access permits. Resolve concrete feedback, rerun affected checks, and repeat AI review. Stop at `WAITING_FOR_HUMAN` until a human approval is visible. Never represent AI review as human approval.
8. **Deliver.** Only after the platform confirms the human-approved PR is merged, record PR URL/number, branches, commits, merge SHA, AI review, human approval, CI, delivered scope, limitations, and follow-ups.

## Autonomous boundary

Perform routine, reversible repository inspection, editing, testing, and review preparation autonomously. Follow `AGENTS.md` for server-side Git operations. Pause when a proposed action could lose user work or when a material scope, permission, production, or irreversible change still needs authorization. Continue independent work when credentials, services, or decisions block one part of a task.

## Load project-specific guidance as needed

- Read [references/project-profile.md](references/project-profile.md) before discovery and produce [templates/discovery-summary.md](templates/discovery-summary.md). Read [references/quality-matrix.md](references/quality-matrix.md) before verification.
- Use [references/governance-policy.md](references/governance-policy.md) for state transitions and safety boundaries.
- Use [references/review-and-delivery.md](references/review-and-delivery.md) for PR, AI review, human approval, and delivery evidence.
- Start records from [templates/](templates/). Run the deterministic checks in [scripts/](scripts/) before reporting repository state.
