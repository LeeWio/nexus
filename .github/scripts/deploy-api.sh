#!/usr/bin/env bash
set -euo pipefail

image_repository="${API_IMAGE%@*}"
image_digest="${API_IMAGE##*@}"
if [[ $image_repository == "$API_IMAGE" || ! $image_digest =~ ^sha256:[0-9a-f]{64}$ ||
  ! $EXPECTED_REVISION =~ ^[0-9a-f]{40}$ ]]; then
  echo "Invalid build digest or revision" >&2
  exit 1
fi

cleanup() {
  docker logout "${ALIYUN_REGISTRY}" >/dev/null 2>&1 || true
}

case "${SERVER_PROJECT_PATH}" in
  /*) ;;
  *)
    echo "SERVER_PROJECT_PATH must be an absolute path"
    exit 1
    ;;
esac

cd -- "${SERVER_PROJECT_PATH}"
deployment_check_storage "$image_repository" "$image_digest" "$SERVER_PROJECT_PATH"
docker compose -f compose.yaml config --quiet

echo "Logging in to Aliyun Container Registry..."
echo "${ALIYUN_REGISTRY_PASSWORD}" | docker login "${ALIYUN_REGISTRY}" -u "${ALIYUN_REGISTRY_USER}" --password-stdin
trap cleanup EXIT

echo "Pulling immutable API image for revision ${EXPECTED_REVISION}..."
# For VPC servers, use the VPC registry endpoint (optional manual override in target server if needed)
docker pull "${API_IMAGE}"

PULLED_REVISION="$(docker image inspect --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "${API_IMAGE}")"
if [ "${PULLED_REVISION}" != "${EXPECTED_REVISION}" ]; then
  echo "Pulled image revision '${PULLED_REVISION}' does not match '${EXPECTED_REVISION}'"
  exit 1
fi

PREVIOUS_CONTAINER_ID="$(docker compose -f compose.yaml ps -q api || true)"
PREVIOUS_IMAGE_ID=""
PREVIOUS_REVISION=""
if [ -n "${PREVIOUS_CONTAINER_ID}" ]; then
  PREVIOUS_IMAGE_ID="$(docker inspect --format '{{.Image}}' "${PREVIOUS_CONTAINER_ID}" 2>/dev/null || true)"
  PREVIOUS_REVISION="$(docker image inspect --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "${PREVIOUS_IMAGE_ID}" 2>/dev/null || true)"
fi

rollback() {
  echo "Deployment failed. Recent API logs:"
  docker compose -f compose.yaml logs --tail=200 api || true

  if [ -z "${PREVIOUS_IMAGE_ID}" ]; then
    echo "No previous API container was available for rollback"
    return
  fi

  echo "Restoring previous API revision ${PREVIOUS_REVISION:-unknown}..."
  if ! docker tag "${PREVIOUS_IMAGE_ID}" "${API_LATEST_IMAGE}"; then
    echo "Unable to retag the previous API image for rollback"
    return
  fi

  if ! docker compose -f compose.yaml up -d --no-deps --force-recreate --pull never api; then
    echo "Unable to recreate the previous API container"
    return
  fi

  ROLLED_BACK_CONTAINER_ID="$(docker compose -f compose.yaml ps -q api || true)"
  ROLLED_BACK_IMAGE_ID="$(docker inspect --format '{{.Image}}' "${ROLLED_BACK_CONTAINER_ID}" 2>/dev/null || true)"
  if [ "${ROLLED_BACK_IMAGE_ID}" = "${PREVIOUS_IMAGE_ID}" ]; then
    echo "Rollback container was recreated from the previous image"
  else
    echo "Rollback image verification failed"
  fi
}

fail_deploy() {
  echo "$1"
  rollback
  exit 1
}

# compose.yaml references :latest. Repoint that local tag to the exact
# SHA image before recreating the service, rather than pulling a moving tag.
PULLED_IMAGE_ID="$(docker image inspect --format '{{.Id}}' "${API_IMAGE}")"
if [ -n "${PREVIOUS_IMAGE_ID}" ] && [ "${PREVIOUS_IMAGE_ID}" != "${PULLED_IMAGE_ID}" ]; then
  docker tag "${PREVIOUS_IMAGE_ID}" "${API_LATEST_IMAGE%:*}:rollback"
fi
docker tag "${PULLED_IMAGE_ID}" "${API_LATEST_IMAGE}"
if ! docker compose -f compose.yaml up -d --no-deps --force-recreate --pull never api; then
  fail_deploy "Unable to recreate the API container"
fi

CONTAINER_ID="$(docker compose -f compose.yaml ps -q api)"
if [ -z "${CONTAINER_ID}" ]; then
  fail_deploy "API container ID could not be resolved"
fi

DEPLOYED_IMAGE_ID="$(docker inspect --format '{{.Image}}' "${CONTAINER_ID}" 2>/dev/null || true)"
DEPLOYED_REVISION="$(docker image inspect --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "${DEPLOYED_IMAGE_ID}" 2>/dev/null || true)"
if [ "${DEPLOYED_REVISION}" != "${EXPECTED_REVISION}" ]; then
  fail_deploy "Container revision '${DEPLOYED_REVISION}' does not match '${EXPECTED_REVISION}'"
fi

READY=false
MAX_WAIT_SECONDS=240
POLL_INTERVAL_SECONDS=2
MAX_ATTEMPTS=$((MAX_WAIT_SECONDS / POLL_INTERVAL_SECONDS))

for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
  RUNNING="$(docker inspect --format '{{.State.Running}}' "${CONTAINER_ID}" 2>/dev/null || true)"
  HEALTH="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${CONTAINER_ID}" 2>/dev/null || true)"

  echo "Check ${attempt}/${MAX_ATTEMPTS}: running=${RUNNING}, health=${HEALTH}"

  if [ "${RUNNING}" != "true" ]; then
    fail_deploy "API container stopped unexpectedly"
  fi

  case "${HEALTH}" in
    healthy)
      READY=true
      break
      ;;
    unhealthy)
      echo "API container is temporarily unhealthy; waiting for cold start to complete"
      ;;
    none)
      fail_deploy "API image does not define a Docker health check"
      ;;
  esac

  sleep "${POLL_INTERVAL_SECONDS}"
done

if [ "${READY}" != "true" ]; then
  fail_deploy "Container did not become ready in time"
fi

echo "Deployment completed for revision ${EXPECTED_REVISION}"
