#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="${ROOT_DIR}/frontend"
BACKEND_DIR="${ROOT_DIR}/backend"

echo "[1/3] 分层越界检查"
bash "${ROOT_DIR}/scripts/check_canonical_entity_boundaries.sh"

echo "[2/3] 契约快照检查"
cd "${FRONTEND_DIR}"
npm test -- src/api/contracts.test.js

echo "[3/3] Golden Path 烟测"
cd "${BACKEND_DIR}"
echo "  - clean compile to avoid stale generated classes"
mvn -q -DskipTests clean compile
mvn -q -Dtest=CanonicalEntityPersistenceTest,SceneGraphAssetSyncCanonicalTest,CanonicalEntityResolutionServiceTest test

echo "canonical_entity_gate passed."
