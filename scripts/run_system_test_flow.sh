#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="/Users/rlc/Code/数据直通车"
FRONTEND_DIR="${ROOT_DIR}/frontend"
BACKEND_DIR="${ROOT_DIR}/backend"

echo "[1/5] 前端 lint"
cd "${FRONTEND_DIR}"
npm run lint

echo "[2/5] 前端 test"
npm run test

echo "[3/5] 前端 build + 产物同步"
npm run build
bash "${ROOT_DIR}/scripts/sync_frontend_dist.sh"

echo "[4/5] 后端 test"
cd "${BACKEND_DIR}"
mvn -q test

echo "[5/5] 轻量健康检查（如服务在线）"
if curl -fsS "http://127.0.0.1:8080/api/system/llm-preprocess-config" >/dev/null 2>&1; then
  echo "  - backend 8080 可访问"
else
  echo "  - backend 8080 未在线（非阻断，按需先启动）"
fi

if curl -fsS -I "http://127.0.0.1:5173" >/dev/null 2>&1; then
  echo "  - frontend 5173 可访问"
else
  echo "  - frontend 5173 未在线（非阻断，按需先启动）"
fi

echo "系统化测试流程执行完成。"
