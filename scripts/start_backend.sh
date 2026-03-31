#!/usr/bin/env bash
# 启动后端服务（自动加载 .env.local 中的环境变量）
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env.local"

# 加载 .env.local（跳过注释和空行）
if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line; do
    [[ -z "$line" || "$line" =~ ^# ]] && continue
    export "$line"
  done < "$ENV_FILE"
  echo "[env] 已加载 $ENV_FILE"
else
  echo "[env] 未找到 $ENV_FILE，使用 application.yml 默认值"
fi

cd "$PROJECT_ROOT/backend"
echo "[boot] 启动后端服务 ..."
exec mvn -q spring-boot:run -Dmaven.repo.local=.m2/repository
