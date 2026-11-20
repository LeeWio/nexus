# ENG-20261119 — Reading, editing and notification journeys

State: READY_FOR_REVIEW

## Discovery

Clean base: `d0a4772`; target: `main`; branch: `feature/reading-editor-notification`. Odyssey base: `7618f1e`. User explicitly authorizes local commits/pushes and production delivery, superseding server-only Git instructions. All backend checks use Docker. Existing CI, PR review and production environment rules remain enforced.

## Requirements and design

- P1: serialize per-user history writes, including first creation. Separate current progress from furthest progress. Order resume positions by a required client observation timestamp; stale requests cannot regress the resume position. Preserve completion once reached. Validate timestamps and anchor bounds. Add a forward-only Flyway migration; preserve existing rows.
- P2: database editor drafts are the only autosave source. Remove Redis autosave endpoints, fallback and obsolete DTOs. Extract publication transitions and public query behavior into focused services while preserving authorization, transactions, caching, revisions and event behavior.
- P3: add real persisted journey evidence and export actual response fixtures and OpenAPI so Odyssey validates payload schemas. Cover notification category preferences, permitted destinations, and audited delivery retries without sending production mail.

## Verification and recovery

Use focused Docker Maven tests followed by Spotless, PMD, full verify and disposable MySQL migration validation. Review changes independently and publish a linked PR. Deploy this backend before Odyssey; verify immutable revision and smoke endpoints. Use existing image rollback if necessary, preserving applied schema. No production data cleanup or Flyway history repair is in scope.

## Evidence

- Backend Docker `mvn -B -ntp clean spotless:check pmd:pmd verify -Dnexus.openapi.output=/app/target/nexus-openapi.json` passed. Results were 459 tests, 0 failures, 0 errors, and 5 skipped.
- Disposable MySQL 8.4 validation applied 125 baseline migrations through V198, applied V199, and passed Flyway validation with 126 migrations.
- Docker image `nexus-api:local` built from the implementation commit. The recreated local container applied V199, became healthy, and passed `./test-api.sh`.
- Live OpenAPI contains the new observed reading timestamp and database draft endpoint, with no autosave paths. The generated document is `target/nexus-openapi.json`.
- AI self-review covered reading concurrency and ordering, draft ownership and revision conflicts, service boundaries, notification destinations, migration ordering, and secret scanning. No blocking findings remain.
