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
- The `production` GitHub Environment should require approval from a maintainer.

## Quality and security gates

- CI runs Maven Enforcer, Spotless verification, PMD reporting, tests, and a
  JaCoCo report in one Maven invocation.
- Flyway migrations are applied and validated against an empty MySQL 8.4 schema.
- Pull requests block newly introduced critical dependency vulnerabilities.
- Source/configuration audits are retained for review, while deployable images
  block on fixed critical vulnerabilities.

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
