#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
odyssey_root="${ODYSSEY_ROOT:-${repo_root}/../odyssey}"
compose=(docker compose -p nexus-notification-reading-e2e -f "$repo_root/docker-compose.e2e.yml")

cleanup() {
	set +e
	"${compose[@]}" down --volumes --remove-orphans
}
trap cleanup EXIT

"${compose[@]}" up --build --detach
for _ in {1..60}; do
	if curl --fail --silent --max-time 3 http://127.0.0.1:18080/readyz >/dev/null; then
		break
	fi
	sleep 2
done
curl --fail --silent --max-time 5 http://127.0.0.1:18080/readyz >/dev/null
"${compose[@]}" exec -T nexus-db mysql -uroot -pnexus_e2e_password nexus_e2e <"$repo_root/scripts/e2e-seed.sql"

(
	cd "$odyssey_root"
	PLAYWRIGHT_REAL_BACKEND=1 \
	PLAYWRIGHT_API_URL=http://127.0.0.1:18080 \
	bunx playwright test tests/e2e/notification-reading-real.spec.ts --project=chromium
)
