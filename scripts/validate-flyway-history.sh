#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
migration_dir="${MIGRATION_DIR:-${repo_root}/src/main/resources/db/migration}"
flyway_image="${FLYWAY_IMAGE:-flyway/flyway:11.14.1}"
connect_retries="${FLYWAY_CONNECT_RETRIES:-60}"
flyway_network_args=(--network host)

if [[ -n "${FLYWAY_DOCKER_NETWORK:-}" ]]; then
	flyway_network_args=(--network "${FLYWAY_DOCKER_NETWORK}")
fi

: "${MYSQL_URL:?Set MYSQL_URL to the database under review}"
: "${MYSQL_USER:?Set MYSQL_USER to the read-only validation user}"
: "${MYSQL_PASSWORD:?Set MYSQL_PASSWORD to the validation user password}"

if [[ ! -d "${migration_dir}" ]]; then
	echo "Migration directory does not exist: ${migration_dir}" >&2
	exit 1
fi

flyway() {
	docker run --rm "${flyway_network_args[@]}" -v "${migration_dir}:/flyway/sql:ro" "${flyway_image}" \
		-url="${MYSQL_URL}" \
		-user="${MYSQL_USER}" \
		-password="${MYSQL_PASSWORD}" \
		-locations=filesystem:/flyway/sql \
		-connectRetries="${connect_retries}" \
		-validateMigrationNaming=true "$@"
}

flyway info
flyway validate

echo "Flyway history validation passed without modifying the database."
