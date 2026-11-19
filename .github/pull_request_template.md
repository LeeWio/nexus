## Summary

<!-- What changed and why? Link the issue or design note when one exists. -->

## Scope and rollout

- [ ] This change is scoped to the stated problem.
- [ ] Database changes follow expand/contract compatibility rules.
- [ ] Production configuration or deployment impact is documented.
- [ ] No secrets, credentials, or production data are included.

## Verification

- [ ] `./mvnw -B -ntp spotless:check verify`
- [ ] `./test-api.sh` (when a running API is available)
- [ ] Flyway migration validation was run for migration changes.
- [ ] I recorded any checks that were not run and why.

## Review notes

<!-- Call out compatibility, migration ordering, security, or rollback concerns. -->
