#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
maven_image="${MAVEN_IMAGE:-maven:3.9.15-amazoncorretto-21}"
ratchet_from="${SPOTLESS_RATCHET_FROM:-origin/main}"

docker run --rm \
	-v "${repo_root}:/app" \
	-v nexus-maven-cache:/root/.m2 \
	-w /app \
	-e MAVEN_OPTS="${MAVEN_OPTS:--Xmx512m -XX:+UseSerialGC}" \
	"${maven_image}" \
	mvn -B -ntp clean spotless:check pmd:pmd verify \
		-Dnexus.openapi.output=target/nexus-openapi.json \
		-Dspotless.ratchetFrom="${ratchet_from}"
