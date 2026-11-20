# ENG-20261120 — Notification, reading and release hardening

State: READY_FOR_REVIEW

## Objective

Close the next cross-repository quality gap after the reading/editor/
notification release without changing production data or bypassing protected
delivery gates.

## Scope

- Pin Odyssey's OpenAPI operation snapshot to the merged Nexus revision after
  comparing the complete operation projection with the Nexus CI artifact.
- Make category-publication notifications target Odyssey's canonical
  `/single/{slug}` reader route and cover the boundary in both repositories.
- Provide a read-only Flyway history validation command for the protected
  production-history reconciliation rehearsal.
- Keep performance auditing focused on the public reading surfaces rather than
  adding a new product module.

## Acceptance

- The pinned operation projection matches the merged Nexus artifact byte-for-
  byte after removing only the revision metadata.
- Category-publication notifications produce `/single/{slug}` and frontend
  presentation tests accept the persisted destination.
- Flyway validation runs in Docker, reports `info` and `validate`, and cannot
  perform `repair` or a migration.
- Production history reconciliation remains explicitly gated by a protected
  backup and restored database; no live data cleanup is performed here.

## Verification

- Docker Maven quality gate `clean spotless:check pmd:pmd verify` passes with
  459 tests, 0 failures, 0 errors, and 5 skipped.
- `docker build --tag nexus-api:eng-20261120 .`: PASS — the deployable image
  builds from the current source.
- `BASE_URL=http://127.0.0.1:8080 ./test-api.sh`: PASS — the local Docker API
  smoke endpoints are healthy.
- Notification service coverage verifies `/single/{slug}` for category
  publication notifications.
- Odyssey notification presentation tests, preflight, build, contract coverage
  and focused browser checks pass.
- `scripts/validate-flyway-history.sh` passes `bash -n` and runs Docker Flyway
  `info` against local schema version 199. Its read-only `validate` step
  reports the existing V181 and V182 checksum mismatches and exits non-zero;
  no `repair` or migration is executed. Production reconciliation remains
  blocked on a protected backup and isolated restore rehearsal.
