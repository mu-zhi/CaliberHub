#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="${ROOT_DIR}/frontend"
BACKEND_DIR="${ROOT_DIR}/backend"
BASE_URL="${BASE_URL:-http://127.0.0.1:8082}"
FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:5174}"
CI_SKIP_BACKEND_TESTS="${CI_SKIP_BACKEND_TESTS:-false}"

echo "[1/5] 前端 lint"
cd "${FRONTEND_DIR}"
npm run lint

echo "[2/5] 前端 test"
npm run test

echo "[3/5] 前端 build + 产物同步"
npm run build
bash "${ROOT_DIR}/scripts/sync_frontend_dist.sh"

echo "[4/5] 后端 test / compile"
cd "${BACKEND_DIR}"
if [[ "${CI_SKIP_BACKEND_TESTS}" == "true" ]]; then
  echo "  - 跳过后端测试（CI_SKIP_BACKEND_TESTS=true），执行 test-compile"
  mvn -q -DskipTests test-compile
else
  mvn -q test
fi

echo "[5/5] 轻量健康检查（如服务在线）"
if curl -fsS -X POST "${BASE_URL%/}/api/system/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' >/dev/null 2>&1; then
  echo "  - backend 8082 可访问"
else
  echo "  - backend 8082 未在线（非阻断，按需先启动）"
fi

if curl -fsS -I "${FRONTEND_URL%/}" >/dev/null 2>&1; then
  echo "  - frontend 5174 可访问"
else
  echo "  - frontend 5174 未在线（非阻断，按需先启动）"
fi

echo "系统化测试流程执行完成。"
