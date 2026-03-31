#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
PROVIDER_DIR="${CLAUDE_PROVIDER_DIR:-$PROJECT_ROOT/tooling/claude-providers}"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/claude_mux.sh <provider> <model> [claude args...]

Supported providers:
  bailian
  wenwen
EOF
}

if ! command -v claude >/dev/null 2>&1; then
  echo "claude command not found. Install Claude Code first." >&2
  exit 1
fi

PROVIDER="${1:-}"
MODEL="${2:-}"

if [ -z "$PROVIDER" ] || [ -z "$MODEL" ]; then
  usage >&2
  exit 1
fi

case "$PROVIDER" in
  bailian|wenwen) ;;
  *)
    echo "Unsupported provider: $PROVIDER" >&2
    usage >&2
    exit 1
    ;;
esac

shift 2

ENV_FILE="$PROVIDER_DIR/$PROVIDER.env.local"
if [ ! -f "$ENV_FILE" ]; then
  echo "Provider config not found: $ENV_FILE" >&2
  echo "Copy $PROVIDER_DIR/$PROVIDER.env.example to $ENV_FILE and fill in the provider credentials." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

: "${CLAUDE_PROVIDER_BASE_URL:?CLAUDE_PROVIDER_BASE_URL is required}"

if [ -n "${CLAUDE_PROVIDER_API_KEY:-}" ]; then
  export ANTHROPIC_API_KEY="$CLAUDE_PROVIDER_API_KEY"
  unset ANTHROPIC_AUTH_TOKEN
elif [ -n "${CLAUDE_PROVIDER_AUTH_TOKEN:-}" ]; then
  export ANTHROPIC_AUTH_TOKEN="$CLAUDE_PROVIDER_AUTH_TOKEN"
  unset ANTHROPIC_API_KEY
else
  echo "Provider config must define CLAUDE_PROVIDER_API_KEY or CLAUDE_PROVIDER_AUTH_TOKEN." >&2
  exit 1
fi

export ANTHROPIC_BASE_URL="$CLAUDE_PROVIDER_BASE_URL"
export ANTHROPIC_MODEL="$MODEL"

exec claude \
  --setting-sources "project,local" \
  --model "$MODEL" \
  "$@"
