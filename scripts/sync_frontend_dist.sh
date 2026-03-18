#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_STATIC_DIR="$ROOT_DIR/backend/src/main/resources/static"

echo "[sync] build frontend dist..."
cd "$FRONTEND_DIR"
npm run build

echo "[sync] replace backend static with frontend dist..."
mkdir -p "$BACKEND_STATIC_DIR"
find "$BACKEND_STATIC_DIR" -mindepth 1 -maxdepth 1 ! -name "business_categories.json" -exec rm -rf {} +
cp -R "$FRONTEND_DIR/dist/." "$BACKEND_STATIC_DIR/"

echo "[sync] done -> $BACKEND_STATIC_DIR"
