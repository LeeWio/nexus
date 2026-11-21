#!/usr/bin/env bash

deployment_storage_warning() {
  local message=$*
  printf '::warning title=Production storage::%s\n' "${message//%/%25}"
}

deployment_storage_snapshot() {
  local rows
  rows=$(LC_ALL=C df -Pk -- "$@" | awk '
    NR > 1 && $4 ~ /^[0-9]+$/ && $5 ~ /^[0-9]+%$/ && !seen[$1]++ {
      print $1, $4, int($5)
    }
  ') || return 1
  [[ -n $rows ]] || return 1
  printf '%s\n' "$rows"
}

deployment_prune_images() {
  local mirror=$1 candidate_digest=$2 cutoff containers protected inventory candidates image_ref
  local removed=0
  cutoff=$(date -d '7 days ago' +%F) || return 1
  containers=$(timeout 15s docker ps --all --quiet --no-trunc) || return 1
  protected=
  if [[ -n $containers ]]; then
    local -a container_ids
    mapfile -t container_ids <<<"$containers"
    protected=$(timeout 20s docker inspect --format '{{.Image}}' "${container_ids[@]}") || return 1
  fi
  inventory=$(timeout 90s docker image ls --digests --no-trunc \
    --format '{{.CreatedAt}}|{{.Repository}}|{{.Tag}}|{{.Digest}}|{{.ID}}' |
    LC_ALL=C sort -r) || return 1

  candidates=$(awk -F'|' -v mirror="$mirror" -v cutoff="$cutoff" -v pinned="$protected" \
    -v candidate="$candidate_digest" '
    BEGIN {
      split(pinned, image_ids, "\n")
      for (position in image_ids) keep[image_ids[position]] = 1
    }
    function is_commit(tag) { return length(tag) == 40 && tag ~ /^[0-9a-f]+$/ }
    function is_digest(digest) { return length(digest) == 71 && digest ~ /^sha256:[0-9a-f]+$/ }
    {
      if (!is_digest($5)) next
      if (($2 != mirror && $2 != "<none>") || ($2 == mirror && $3 != "<none>" && !is_commit($3)) ||
          $4 == candidate || $5 == candidate) keep[$5] = 1
      if ($2 != mirror) next
      if (!seen[$5]++ && ++recent <= 20) keep[$5] = 1
      if (substr($1, 1, 10) >= cutoff) keep[$5] = 1
      if (substr($1, 1, 10) !~ /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$/ ||
          !is_digest($4) || ($3 != "<none>" && !is_commit($3))) next
      ids[NR] = $5
      refs[NR] = ($3 == "<none>" ? mirror "@" $4 : mirror ":" $3)
    }
    END {
      for (row = NR; row > 0 && count < 25; row--) {
        if (!(row in ids) || keep[ids[row]] || emitted[refs[row]]++) continue
        print refs[row]
        count++
      }
    }
  ' <<<"$inventory") || return 1

  if [[ -n $candidates ]]; then
    while IFS= read -r image_ref; do
      if timeout 15s docker image rm --no-prune "$image_ref" >/dev/null; then
        printf 'Removed unused local image: %s\n' "$image_ref"
        removed=$((removed + 1))
      else
        deployment_storage_warning "Docker refused image removal; stopping cleanup."
        break
      fi
    done <<<"$candidates"
  fi
  printf 'Removed local image references: %s\n' "$removed"
}

deployment_check_storage() {
  local mirror=$1 digest=$2 project_dir=$3 docker_root storage report device available used
  local needs_cleanup=false too_low=false
  local -a paths

  docker_root=$(timeout 15s docker info --format '{{.DockerRootDir}}') || return 1
  [[ $docker_root == /* ]] || return 1
  paths=("$docker_root")
  if [[ -d /var/lib/containerd ]]; then
    paths+=(/var/lib/containerd)
  fi
  report=$(deployment_storage_snapshot "${paths[@]}" "${TMPDIR:-/tmp}" "$project_dir") || return 1
  storage=$(deployment_storage_snapshot "${paths[@]}") || return 1

  while read -r device available used; do
    if (( used >= 80 )); then
      deployment_storage_warning "$device is ${used}% full before deployment."
    fi
  done <<<"$report"

  while read -r device available used; do
    if (( used >= 80 || available < 1048576 )); then
      needs_cleanup=true
    fi
  done <<<"$storage"

  if [[ $needs_cleanup == true ]]; then
    deployment_prune_images "$mirror" "$digest" || deployment_storage_warning "Unable to inspect old local images; cleanup skipped."
    report=$(deployment_storage_snapshot "${paths[@]}" "${TMPDIR:-/tmp}" "$project_dir") || return 1
    storage=$(deployment_storage_snapshot "${paths[@]}") || return 1
  fi

  while read -r device available used; do
    printf 'Production filesystem %s: %s%% used, %s KiB available\n' "$device" "$used" "$available"
    if (( available == 0 )); then
      too_low=true
    fi
  done <<<"$report"
  while read -r device available used; do
    if (( available < 1048576 )); then
      too_low=true
    fi
  done <<<"$storage"

  if [[ $too_low == true ]]; then
    printf 'Insufficient production disk space; deployment stopped before pulling the image.\n' >&2
    return 1
  fi
}
