#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
dump_path="${MYSQL_DUMP:-}"
mysql_image="${MYSQL_IMAGE:-mysql:8.4.9}"
flyway_network="flyway-history-rehearsal-$RANDOM"
mysql_container="flyway-history-mysql-$RANDOM"
database="${MYSQL_DATABASE:-nexus_history_rehearsal}"
password="${MYSQL_PASSWORD:-rehearsal-only-password}"
app_image="${NEXUS_IMAGE:-}"
app_container="flyway-history-app-$RANDOM"

if [[ -z "$dump_path" || ! -f "$dump_path" ]]; then
	echo "Set MYSQL_DUMP to a protected SQL dump copied for isolated review." >&2
	exit 2
fi

cleanup() {
	set +e
	docker rm -f "$app_container" "$mysql_container" >/dev/null 2>&1
	docker network rm "$flyway_network" >/dev/null 2>&1
}
trap cleanup EXIT

docker network create "$flyway_network" >/dev/null
docker run -d --rm --name "$mysql_container" --network "$flyway_network" \
	-e MYSQL_DATABASE="$database" -e MYSQL_ROOT_PASSWORD="$password" "$mysql_image" >/dev/null

for _ in {1..60}; do
	if docker exec "$mysql_container" mysqladmin ping -h 127.0.0.1 -uroot -p"$password" --silent >/dev/null 2>&1; then
		break
	fi
	sleep 2
done

if ! docker exec "$mysql_container" mysqladmin ping -h 127.0.0.1 -uroot -p"$password" --silent >/dev/null 2>&1; then
	echo "Isolated MySQL did not become ready." >&2
	exit 1
fi

docker exec -i "$mysql_container" mysql -uroot -p"$password" "$database" <"$dump_path"

MYSQL_URL="jdbc:mysql://${mysql_container}:3306/${database}?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC&useSSL=false" \
	MYSQL_USER=root MYSQL_PASSWORD="$password" FLYWAY_DOCKER_NETWORK="$flyway_network" \
	"$repo_root/scripts/validate-flyway-history.sh"

if [[ -n "$app_image" ]]; then
	docker run -d --rm --name "$app_container" --network "$flyway_network" \
		-e SPRING_PROFILES_ACTIVE=prod \
		-e SPRING_FLYWAY_ENABLED=false \
		-e DB_HOST="$mysql_container" -e DB_PORT=3306 -e DB_NAME="$database" \
		-e DB_USERNAME=root -e DB_PASSWORD="$password" \
		-e JWT_SECRET=rehearsal-only-jwt-secret-that-is-long-enough \
		-e ANALYTICS_HASH_SALT=rehearsal-only-analytics-salt \
		"$app_image" >/dev/null
	for _ in {1..60}; do
		if docker exec "$app_container" wget -q --spider http://127.0.0.1:8080/readyz; then
			echo "Application smoke passed against the isolated restored schema."
			exit 0
		fi
		sleep 2
	done
	echo "Application smoke failed against the isolated restored schema." >&2
	docker logs --tail 120 "$app_container" >&2 || true
	exit 1
fi

echo "Flyway history rehearsal passed in an isolated MySQL container."
