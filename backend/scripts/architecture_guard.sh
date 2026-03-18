#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$ROOT_DIR/src/main/java"

errors=0

check_suffix() {
  local target_dir="$1"
  local regex="$2"
  local message="$3"

  if [ ! -d "$target_dir" ]; then
    return
  fi

  while IFS= read -r file; do
    local name
    name="$(basename "$file")"
    if [[ ! "$name" =~ $regex ]]; then
      echo "[FAIL] $message: $file"
      errors=$((errors + 1))
    fi
  done < <(find "$target_dir" -type f -name "*.java")
}

check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/application/api/dto/request" '.*(Cmd|Query)\.java$' "request DTO naming"
check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/application/api/dto/response" '.*DTO\.java$' "response DTO naming"
check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/application/service" '.*AppService\.java$' "application service naming"
check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/application/assembler" '.*Assembler\.java$' "application assembler naming"
check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/infrastructure/module/converter" '.*Converter\.java$' "infrastructure converter naming"
check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/infrastructure/module/dao/po" '.*PO\.java$' "persistence object naming"
check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/domain/support" '.*Support\.java$' "domain support naming"
check_suffix "$SRC_DIR/com/cmbchina/datadirect/caliber/application/support" '.*Support\.java$' "application support naming"

if rg -n "import\\s+com\\.cmbchina\\.datadirect\\.caliber\\.infrastructure\\.module\\.dao\\.(po|mapper)\\." \
  "$SRC_DIR/com/cmbchina/datadirect/caliber/application" \
  "$SRC_DIR/com/cmbchina/datadirect/caliber/domain" >/dev/null 2>&1; then
  echo "[FAIL] application/domain layer must not import dao mapper/po"
  rg -n "import\\s+com\\.cmbchina\\.datadirect\\.caliber\\.infrastructure\\.module\\.dao\\.(po|mapper)\\." \
    "$SRC_DIR/com/cmbchina/datadirect/caliber/application" \
    "$SRC_DIR/com/cmbchina/datadirect/caliber/domain"
  errors=$((errors + 1))
fi

if rg -n "@Data|@Setter|@AllArgsConstructor" "$SRC_DIR/com/cmbchina/datadirect/caliber/domain" >/dev/null 2>&1; then
  echo "[FAIL] domain layer must not use @Data/@Setter/@AllArgsConstructor"
  rg -n "@Data|@Setter|@AllArgsConstructor" "$SRC_DIR/com/cmbchina/datadirect/caliber/domain"
  errors=$((errors + 1))
fi

if [ "$errors" -gt 0 ]; then
  echo "Architecture guard failed with $errors issue(s)."
  exit 1
fi

"$ROOT_DIR/scripts/verify_static_js_mode.sh"

echo "Architecture guard passed."
