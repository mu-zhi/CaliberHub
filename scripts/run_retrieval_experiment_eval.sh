#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"

DRY_RUN=false
SNAPSHOT_ID=""
ADAPTER_NAME=""
GRAY_SCOPE=""

usage() {
  cat <<'EOF'
Usage: bash scripts/run_retrieval_experiment_eval.sh [options]

Options:
  --dry-run                 Print the command without executing it
  --snapshot-id <value>     Override retrieval index version / snapshot id
  --adapter <value>         Override retrieval adapter name
  --gray-scope <value>      Override gray release scope
  --help                    Show this message
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --snapshot-id)
      SNAPSHOT_ID="${2:-}"
      shift 2
      ;;
    --adapter)
      ADAPTER_NAME="${2:-}"
      shift 2
      ;;
    --gray-scope)
      GRAY_SCOPE="${2:-}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "${JAVA_HOME:-}" ]] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
  JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
  if [[ -n "$JAVA_HOME" ]]; then
    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

find_maven() {
  if [[ -n "${MVN_BIN:-}" ]]; then
    printf '%s\n' "$MVN_BIN"
    return 0
  fi
  if [[ -f "$BACKEND_DIR/mvnw" ]]; then
    printf '%s\n' "$BACKEND_DIR/mvnw"
    return 0
  fi
  if command -v mvn >/dev/null 2>&1; then
    command -v mvn
    return 0
  fi
  local jetbrains_dir="/Users/rlc/Library/Application Support/JetBrains"
  if [[ -d "$jetbrains_dir" ]]; then
    local jetbrains_mvn
    while IFS= read -r jetbrains_mvn; do
      if [[ -f "$jetbrains_mvn" ]]; then
        printf '%s\n' "$jetbrains_mvn"
        return 0
      fi
    done < <(find "$jetbrains_dir" -path "*plugins/maven/lib/maven3/bin/mvn" 2>/dev/null | sort -Vr)
  fi
  for candidate in /opt/homebrew/bin/mvn /usr/local/bin/mvn; do
    if [[ -x "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

build_maven_launcher() {
  local candidate="$1"
  if [[ -x "$candidate" ]]; then
    printf '%s\n' "$candidate"
    return 0
  fi
  if [[ -f "$candidate" ]]; then
    printf 'sh\n%s\n' "$candidate"
    return 0
  fi
  return 1
}

MVN_CMD="$(find_maven || true)"
if [[ -z "$MVN_CMD" ]]; then
  echo "Unable to locate Maven. Set MVN_BIN or install Maven first." >&2
  exit 1
fi

MVN_LAUNCHER=()
while IFS= read -r launcher_part; do
  MVN_LAUNCHER+=("$launcher_part")
done < <(build_maven_launcher "$MVN_CMD")
if [[ ${#MVN_LAUNCHER[@]} -eq 0 ]]; then
  echo "Unable to launch Maven from: $MVN_CMD" >&2
  exit 1
fi

EXEC_ARGS=()
if [[ -n "$SNAPSHOT_ID" ]]; then
  EXEC_ARGS+=("--snapshot-id" "$SNAPSHOT_ID")
fi
if [[ -n "$ADAPTER_NAME" ]]; then
  EXEC_ARGS+=("--adapter" "$ADAPTER_NAME")
fi
if [[ -n "$GRAY_SCOPE" ]]; then
  EXEC_ARGS+=("--gray-scope" "$GRAY_SCOPE")
fi

EXEC_ARGS_STRING=""
if [[ ${#EXEC_ARGS[@]} -gt 0 ]]; then
  EXEC_ARGS_STRING="${EXEC_ARGS[*]}"
fi

COMMAND=(
  "${MVN_LAUNCHER[@]}"
  -q
  -DskipTests
  exec:java
  -Dexec.mainClass=com.cmbchina.datadirect.caliber.application.service.query.graphrag.RetrievalExperimentEvaluationService
)

if [[ -n "$EXEC_ARGS_STRING" ]]; then
  COMMAND+=("-Dexec.args=$EXEC_ARGS_STRING")
fi

if [[ "$DRY_RUN" == true ]]; then
  printf 'cd %q &&' "$BACKEND_DIR"
  printf ' %q' "${COMMAND[@]}"
  printf '\n'
  exit 0
fi

cd "$BACKEND_DIR"
"${COMMAND[@]}"
