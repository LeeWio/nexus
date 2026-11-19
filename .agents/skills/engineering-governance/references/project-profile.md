# Nexus project profile

- **Repository:** `LeeWio/nexus`, target branch currently `main` in the local checkout. Confirm protection and default branch through GitHub before relying on them.
- **Repository docs:** no root `README.md` or `CONTRIBUTING.md` was found in the inspected checkout.
- **Instructions:** read `AGENTS.md`, `GEMINI.md`, `ARCHITECTURE.md`, `.github/RELEASE_ENGINEERING.md`, relevant docs, and module-local instructions before editing. `test-api.sh` is a read-only smoke test against a running Nexus instance.
- **Stack:** Java 21/Spring Boot 4.1, Maven Wrapper, JPA, Redis, Elasticsearch, Flyway, REST, security, and Docker image delivery.
- **Formatting and analysis:** Spotless and PMD are part of CI. Preserve the existing Maven configuration and use the wrapper.
- **Release constraints:** `.github/RELEASE_ENGINEERING.md` requires production environment approval, immutable image digest/revision checks, expand/contract migrations, application/image rollback without reversing applied schema, and does not provide a real canary in the current single-slot Compose topology.
- **CI evidence:** `.github/workflows/ci-cd.yml` runs API verification, publishes a revisioned OpenAPI artifact, and tests the application against the Flyway-migrated MySQL schema. `security.yml` runs dependency review and Trivy. `codeql.yml` runs Java/Kotlin CodeQL. These workflows run on all PRs to `main`.
- **Protection:** inspect the current `Enterprise Main Protection` ruleset before reporting required CI, approval, bypass, or merge settings. `.github/RELEASE_ENGINEERING.md` names the expected check contexts and environment protections.
- **Repository templates:** use `.github/pull_request_template.md` for GitHub PRs and record additional evidence from the bundled template when relevant. Confirm branch protection and applicable owners before claiming a required reviewer.
- **Git convention:** recent history uses Conventional Commit-style subjects. The local `AGENTS.md` requires commit and push operations on the production checkout at `/srv/blog/apps/api`, so do not commit or push from this local copy unless the user explicitly changes that instruction.
- **Secrets:** `.env` and credentials are sensitive. Never include them in diffs or reports.

The profile describes observed repository facts. Recheck them when the checkout or branch changes.
