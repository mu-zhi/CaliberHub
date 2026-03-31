#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

FAKE_BIN_DIR="$TMP_DIR/bin"
CAPTURE_FILE="$TMP_DIR/capture.txt"
PROVIDER_DIR="$TMP_DIR/providers"

mkdir -p "$FAKE_BIN_DIR"
mkdir -p "$PROVIDER_DIR"

cat >"$FAKE_BIN_DIR/claude" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
{
  printf 'ANTHROPIC_API_KEY=%s\n' "${ANTHROPIC_API_KEY:-}"
  printf 'ANTHROPIC_BASE_URL=%s\n' "${ANTHROPIC_BASE_URL:-}"
  printf 'ANTHROPIC_MODEL=%s\n' "${ANTHROPIC_MODEL:-}"
  for arg in "$@"; do
    printf 'ARG=%s\n' "$arg"
  done
} >"$CLAUDE_CAPTURE_FILE"
EOF
chmod +x "$FAKE_BIN_DIR/claude"

cat >"$PROVIDER_DIR/bailian.env.local" <<'EOF'
CLAUDE_PROVIDER_API_KEY=test-bailian-key
CLAUDE_PROVIDER_BASE_URL=https://bailian.example.invalid
EOF

cat >"$PROVIDER_DIR/wenwen.env.local" <<'EOF'
CLAUDE_PROVIDER_API_KEY=test-wenwen-key
CLAUDE_PROVIDER_BASE_URL=https://wenwen.example.invalid
EOF

PATH="$FAKE_BIN_DIR:$PATH" \
CLAUDE_CAPTURE_FILE="$CAPTURE_FILE" \
CLAUDE_PROVIDER_DIR="$PROVIDER_DIR" \
"$ROOT_DIR/scripts/claude_review.sh" --print "review prompt"

grep -Fq 'ANTHROPIC_API_KEY=test-wenwen-key' "$CAPTURE_FILE"
grep -Fq 'ANTHROPIC_BASE_URL=https://wenwen.example.invalid' "$CAPTURE_FILE"
grep -Fq 'ANTHROPIC_MODEL=claude-sonnet-4-6-20260218' "$CAPTURE_FILE"
grep -Fq 'ARG=--setting-sources' "$CAPTURE_FILE"
grep -Fq 'ARG=project,local' "$CAPTURE_FILE"
grep -Fq 'ARG=--model' "$CAPTURE_FILE"
grep -Fq 'ARG=claude-sonnet-4-6-20260218' "$CAPTURE_FILE"
grep -Fq 'ARG=--print' "$CAPTURE_FILE"
grep -Fq 'ARG=review prompt' "$CAPTURE_FILE"

test -x "$ROOT_DIR/scripts/claude_mux.sh"

PATH="$FAKE_BIN_DIR:$PATH" \
CLAUDE_CAPTURE_FILE="$CAPTURE_FILE" \
CLAUDE_PROVIDER_DIR="$PROVIDER_DIR" \
"$ROOT_DIR/scripts/claude_mux.sh" bailian gpt-5.2-codex --print "dev prompt"

grep -Fq 'ANTHROPIC_API_KEY=test-bailian-key' "$CAPTURE_FILE"
grep -Fq 'ANTHROPIC_BASE_URL=https://bailian.example.invalid' "$CAPTURE_FILE"
grep -Fq 'ANTHROPIC_MODEL=gpt-5.2-codex' "$CAPTURE_FILE"
grep -Fq 'ARG=project,local' "$CAPTURE_FILE"
grep -Fq 'ARG=gpt-5.2-codex' "$CAPTURE_FILE"
grep -Fq 'ARG=dev prompt' "$CAPTURE_FILE"

PATH="$FAKE_BIN_DIR:$PATH" \
CLAUDE_CAPTURE_FILE="$CAPTURE_FILE" \
CLAUDE_PROVIDER_DIR="$PROVIDER_DIR" \
"$ROOT_DIR/scripts/claude_mux.sh" wenwen claude-sonnet-4-6-20260218 --print "review via mux"

grep -Fq 'ANTHROPIC_API_KEY=test-wenwen-key' "$CAPTURE_FILE"
grep -Fq 'ANTHROPIC_BASE_URL=https://wenwen.example.invalid' "$CAPTURE_FILE"
grep -Fq 'ANTHROPIC_MODEL=claude-sonnet-4-6-20260218' "$CAPTURE_FILE"
grep -Fq 'ARG=claude-sonnet-4-6-20260218' "$CAPTURE_FILE"
grep -Fq 'ARG=review via mux' "$CAPTURE_FILE"

for wrapper in \
  claude-dev.sh \
  claude-coder.sh \
  claude-plan.sh \
  claude_review.sh \
  claude-review-deep.sh
do
  test -x "$ROOT_DIR/scripts/$wrapper"
done
