#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

scan_forbidden_terms() {
  local scope="$1"
  local root="$2"
  shift 2

  if [[ ! -d "${root}" ]]; then
    return 0
  fi

  local -a patterns=("$@")
  local -a rg_args=(
    -n
    --hidden
    --glob '!**/node_modules/**'
    --glob '!**/target/**'
    --glob '!**/dist/**'
    --glob '!**/.git/**'
  )

  local pattern
  for pattern in "${patterns[@]}"; do
    rg_args+=(-e "${pattern}")
  done

  local matches
  matches="$(rg "${rg_args[@]}" "${root}" || true)"
  if [[ -n "${matches}" ]]; then
    echo "boundary_failed: ${scope} contains forbidden canonical access patterns"
    echo "${matches}"
    exit 1
  fi
}

scan_forbidden_terms \
  "frontend canonical boundary" \
  "${ROOT_DIR}/frontend/src" \
  "caliber_canonical_" \
  "canonical_entity_" \
  "canonicalEntity" \
  "CanonicalEntity" \
  "CanonicalSnapshot"

scan_forbidden_terms \
  "backend query/web canonical boundary" \
  "${ROOT_DIR}/backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query" \
  "caliber_canonical_" \
  "canonical_entity_" \
  "CanonicalEntityMapper" \
  "CanonicalEntityPO" \
  "CanonicalEntityMembershipMapper" \
  "CanonicalEntityRelationPO" \
  "CanonicalResolutionAuditPO" \
  "CanonicalSnapshotMembershipPO"

scan_forbidden_terms \
  "backend adapter/web canonical boundary" \
  "${ROOT_DIR}/backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web" \
  "caliber_canonical_" \
  "canonical_entity_" \
  "CanonicalEntityMapper" \
  "CanonicalEntityPO" \
  "CanonicalEntityMembershipMapper" \
  "CanonicalEntityRelationPO" \
  "CanonicalResolutionAuditPO" \
  "CanonicalSnapshotMembershipPO"

echo "canonical_entity_boundary_check passed."
