# Nexus quality matrix

Run only checks that apply to the change. Keep the exact command, result, evidence path or key output, and notes in the task record.

| Check | Command / evidence | Guidance |
|---|---|---|
| Formatting, static analysis, tests, package | `./mvnw -B -ntp clean spotless:check pmd:pmd verify -Dspotless.ratchetFrom=origin/main` | Matches the main CI verification job. Use the correct baseline for a non-PR context. |
| Focused tests | `./mvnw -B -ntp -Dtest='...Test' test` | Prefer focused tests while iterating, then run the full applicable gate. |
| Migration safety | CI `migration-verify` job, or an equivalent disposable MySQL 8.4 validation | On a PR, apply migrations from the base revision first and validate the upgrade. Use Hibernate schema validation and run the scoped integration tests on the migrated schema. Record `BLOCKED` if Docker/MySQL is unavailable. |
| Security/dependencies | CI dependency-review, Trivy, and CodeQL artifacts | Do not mark local security coverage as `PASS` when only CI can run it. |
| API compatibility | Existing integration/contract tests and the `nexus-openapi-<commit-sha>` CI artifact | Record changed endpoints, schemas, auth behavior, and backward-compatibility evidence. Use `BASE_URL=http://localhost:8080 ./test-api.sh` for read-only smoke checks when an API instance is running. |
| Build artifact | `./mvnw -B -ntp clean package -DskipTests` when packaging is affected | Preserve the generated artifact path and environment details. |

For Skill changes, run `python3 <skill-creator>/scripts/quick_validate.py .agents/skills/engineering-governance`, check each script with `bash -n`, and verify internal links and referenced files. Record the PR checks alongside these local results.

`PASS` means the command actually ran and succeeded. Use `FAIL`, `BLOCKED`, or `N/A` with reasons for everything else.
