#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
STATIC_DIR="$ROOT_DIR/src/main/resources/static"
INDEX_FILE="$STATIC_DIR/index.html"

if [ ! -f "$INDEX_FILE" ]; then
  echo "[FAIL] static index not found: $INDEX_FILE"
  exit 1
fi

errors=0

while IFS= read -r line; do
  src="$(echo "$line" | sed -nE 's/.*src="([^"]+)".*/\1/p')"
  type_attr="$(echo "$line" | sed -nE 's/.*type="([^"]+)".*/\1/p')"
  if [[ -z "$src" || "$src" == http* ]]; then
    continue
  fi
  local_file="$STATIC_DIR/${src#/}"
  if [[ "$src" == /assets/* ]]; then
    local_file="$STATIC_DIR/${src#/}"
  fi
  if [ ! -f "$local_file" ]; then
    continue
  fi
  if [[ "$type_attr" != "module" ]] && rg -n "^[[:space:]]*export[[:space:]]" "$local_file" >/dev/null 2>&1; then
    echo "[FAIL] non-module script includes ESM export: $src"
    errors=$((errors + 1))
  fi
done < <(rg -n "<script[^>]+src=\"" "$INDEX_FILE" | cut -d: -f3-)

if [ "$errors" -gt 0 ]; then
  echo "static js mode guard failed with $errors issue(s)."
  exit 1
fi

echo "static js mode guard passed."
