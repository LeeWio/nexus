# Release Engineering Guardrails

## Required production configuration

- `SERVER_SSH_FINGERPRINT` is required by the deployment workflow. Store the
  server's expected SSH host-key fingerprint as a production environment secret.
- Production deployments use the immutable image digest emitted by the build job
  and verify the `org.opencontainers.image.revision` label before accepting the
  container as the requested revision.
- If the replacement container cannot start, fails its health check, or reports
  an unexpected revision, deployment restores the image that backed the prior
  API container. This is an availability safeguard for the current single-slot
  Compose topology, not a zero-downtime rollout.
- Before pulling, the deploy job warns at 80% filesystem usage. Under Docker
  storage pressure it removes at most 25 old Nexus mirror references on the
  host. The newest 20 images, the last seven calendar days, and current, pinned,
  rollback, or incoming images are retained. Other repositories, volumes, and
  registry copies are not affected. A successful replacement leaves the prior
  image tagged as `ghcr.io/leewio/nexus-api:rollback`.
- If Docker storage still has less than 1 GiB available, or a required filesystem
  has no available space, the job stops before pulling. These are deployment
  annotations rather than continuous monitoring.
- Protect the `production` GitHub Environment with a required maintainer review
  and restrict deployment branches to `main`. Repository maintainers configure
  these settings in GitHub. The workflow binds the deploy job to this environment.

## Quality and security gates

- CI runs Maven Enforcer, Spotless verification, PMD reporting, tests, and a
  JaCoCo report in one Maven invocation.
- Every PR into `main` runs `Verify API`, `Verify Flyway Migrations on MySQL 8.4`,
  `Review New Dependencies`, and `CodeQL Security Scan (Java)`, including
  documentation and governance changes. Require these check contexts in the
  main ruleset after verifying them on a PR.
- On PRs, apply Flyway migrations from the target revision to disposable
  MySQL 8.4, then migrate and validate with the candidate revision. On `main`,
  apply and validate the full chain against an empty schema before image
  publication. For both paths, Spring Boot uses Hibernate schema validation
  and runs the auth and analytics integration tests on the migrated schema.
- The verify job exports the runtime-generated OpenAPI document as
  `nexus-openapi-<commit-sha>`. Use this evidence or the generated document
  from an isolated test instance when updating the frontend contract.
- Pull requests block newly introduced critical dependency vulnerabilities.
- Source/configuration audits are retained for review, while deployable images
  block on fixed critical vulnerabilities. Pin external GitHub Actions to
  a full upstream commit SHA and annotate the corresponding version for review.

## Database rollout policy

Production migrations must follow expand/contract compatibility rules. Additive
changes ship before code starts relying on them; destructive cleanup occurs only
after every running application revision no longer needs the old schema. Roll
back application traffic or image digests, never an already applied schema
migration.

## Next infrastructure phase

The current Compose topology has one API container, so it cannot provide a real
canary. Blue/green rollout requires separate `api-blue` and `api-green` services,
weighted Caddy upstreams, Prometheus-based error/latency thresholds, and a
rollback that switches ingress back to the healthy slot without reversing schema
migrations. That infrastructure change must be deployed as a separate phase;
claiming canary behavior before ingress can split traffic would be misleading.

## Restore production Flyway validation

For new changes, keep applied migrations immutable and add a higher version for
each schema change. PR CI validates the upgrade from the current main branch.
Production additionally needs reconciliation of its historical database state.
On 2026-11-19, a read-only comparison of 120 applied migration rows found 28
checksum or script differences and a recorded V28 absent from the current Git
tree. Commit `c9416c2` moved V28 to V26.1 before V27's seed data. The production
profile keeps the existing compatibility setting until that history is verified.

1. Capture a protected backup and confirm it can be restored. On an isolated
   restore, run Flyway `info` and `validate` with the same Flyway version,
   migration locations and options as the deployable JAR. Compare each reported
   version with the migration file from its originally deployed revision.
2. For each mismatch, document whether the difference is cosmetic or changes
   the schema or data. Use a new forward migration for any material difference.
   Investigate V28 and its current V26.1 counterpart before treating either as
   equivalent. Test a fresh install alongside the upgraded restore.
3. If metadata reconciliation is still required, review the exact expected
   history changes and rehearse them on the restored copy. A Flyway `repair`
   can rewrite checksums and mark missing migrations as deleted. Keep it outside
   the deployment workflow and use it only with an approved, backed-up
   reconciliation plan and a reviewed before/after history diff.
4. Enable `SPRING_FLYWAY_VALIDATE_ON_MIGRATE=true` on the restored instance
   and validate application startup, migration history and the API smoke test.
   Enable it in production through a separately approved rollout after these
   checks pass with the actual release artifact.

The read-only validation command is available at
`scripts/validate-flyway-history.sh`. Run it against the restored copy with
`MYSQL_URL`, `MYSQL_USER`, and `MYSQL_PASSWORD`; it performs `info` and
`validate` through the pinned Flyway Docker image and never runs `repair` or
changes the database. The command is evidence for the reconciliation review,
not a substitute for the protected backup and restore rehearsal.
