#!/usr/bin/env bash

set -euo pipefail

if ! command -v jq >/dev/null 2>&1; then
  echo "missing dependency: jq"
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "missing dependency: curl"
  exit 1
fi

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
API_URL="${BASE_URL%/}/api/import/preprocess"
CURL_MAX_TIME="${CURL_MAX_TIME:-120}"
VERIFY_MODE="${VERIFY_MODE:-strict}"
ELASTIC_MAX_FAIL="${ELASTIC_MAX_FAIL:-1}"
ELASTIC_RETRY_PER_FAIL="${ELASTIC_RETRY_PER_FAIL:-1}"
RUNS_PER_GROUP="${RUNS_PER_GROUP:-4}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DOC_DIR="${PROJECT_ROOT}/research/source-materials/sql-samples"

DOC09="${DOC_DIR}/03-口径文档现状-零售客户信息查询.sql"
DOC10="${DOC_DIR}/04-口径文档现状-零售客户信息变更.sql"
DOC11="${DOC_DIR}/05-口径文档现状-代发明细查询.sql"

for required_file in "$DOC09" "$DOC10" "$DOC11"; do
  if [[ ! -f "$required_file" ]]; then
    echo "missing required file: $required_file"
    exit 1
  fi
done

if [[ "$VERIFY_MODE" != "strict" && "$VERIFY_MODE" != "elastic" && "$VERIFY_MODE" != "fallback_observe" ]]; then
  echo "invalid VERIFY_MODE=${VERIFY_MODE}, fallback to strict"
  VERIFY_MODE="strict"
fi

RESULT_STORE="$(mktemp)"
trap 'rm -f "$RESULT_STORE" "$RESULT_STORE.tmp" 2>/dev/null || true' EXIT

declare -a LOG_LINES=()
declare -a RETRY_LOG_LINES=()
declare -a FAILED_KEYS=()

TOTAL_RUNS=0
LLM_EFFECTIVE_COUNT=0
FALLBACK_COUNT=0
HTTP_FAILURE_COUNT=0
DOC11_MIN_SCENE_COUNT=999999
P95_MS=0
INITIAL_FAILED_COUNT=0
FINAL_FAILED_COUNT=0

call_preprocess() {
  local raw_text="$1"
  local source_type="$2"
  local source_name="$3"
  local response_file
  response_file="$(mktemp)"
  local payload
  payload="$(jq -n --arg rt "$raw_text" --arg st "$source_type" --arg sn "$source_name" '{rawText:$rt, sourceType:$st, sourceName:$sn}')"

  local curl_output
  if [[ -n "${AUTH_TOKEN:-}" ]]; then
    curl_output="$(curl -sS --max-time "$CURL_MAX_TIME" \
      -o "$response_file" \
      -w "%{http_code} %{time_total}" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer ${AUTH_TOKEN}" \
      -X POST "$API_URL" \
      -d "$payload" || true)"
  else
    curl_output="$(curl -sS --max-time "$CURL_MAX_TIME" \
      -o "$response_file" \
      -w "%{http_code} %{time_total}" \
      -H "Content-Type: application/json" \
      -X POST "$API_URL" \
      -d "$payload" || true)"
  fi

  if [[ -z "$curl_output" || "$curl_output" != *" "* ]]; then
    curl_output="000 0"
  fi
  echo "$response_file $curl_output"
}

build_case_payload() {
  local group="$1"
  local run_id="$2"
  case "$group" in
    A)
      CASE_SOURCE_TYPE="PASTE_MD"
      CASE_SOURCE_NAME="A-short-${run_id}.md"
      CASE_RAW_TEXT="$SHORT_SQL"
      ;;
    B)
      CASE_SOURCE_TYPE="FILE_SQL"
      CASE_SOURCE_NAME="$(basename "$DOC09")"
      CASE_RAW_TEXT="$DOC09_TEXT"
      ;;
    C)
      CASE_SOURCE_TYPE="FILE_SQL"
      CASE_SOURCE_NAME="$(basename "$DOC10")"
      CASE_RAW_TEXT="$DOC10_TEXT"
      ;;
    D)
      CASE_SOURCE_TYPE="FILE_SQL"
      CASE_SOURCE_NAME="$(basename "$DOC11")"
      CASE_RAW_TEXT="$DOC11_TEXT"
      ;;
    E)
      CASE_SOURCE_TYPE="PASTE_MD"
      CASE_SOURCE_NAME="E-plain-${run_id}.md"
      CASE_RAW_TEXT="$PLAIN_TEXT"
      ;;
    *)
      echo "unsupported group=${group}"
      exit 1
      ;;
  esac
}

upsert_case_result() {
  local key="$1"
  local group="$2"
  local http_code="$3"
  local latency_ms="$4"
  local mode="$5"
  local fallback="$6"
  local scene_count="$7"
  local warnings_json="$8"
  warnings_json="${warnings_json//$'\t'/ }"
  local line
  line="$(printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$key" "$group" "$http_code" "$latency_ms" "$mode" "$fallback" "$scene_count" "$warnings_json")"
  awk -F $'\t' -v OFS=$'\t' -v key="$key" -v line="$line" '
    BEGIN { replaced = 0 }
    $1 == key { print line; replaced = 1; next }
    { print $0 }
    END { if (replaced == 0) print line }
  ' "$RESULT_STORE" > "$RESULT_STORE.tmp"
  mv "$RESULT_STORE.tmp" "$RESULT_STORE"
}

run_case() {
  local group="$1"
  local run_id="$2"
  local attempt_label="${3:-}"
  local key="${group}#${run_id}"

  build_case_payload "$group" "$run_id"

  local call_result
  call_result="$(call_preprocess "$CASE_RAW_TEXT" "$CASE_SOURCE_TYPE" "$CASE_SOURCE_NAME")"
  local response_file http_code total_time
  response_file="$(awk '{print $1}' <<<"$call_result")"
  http_code="$(awk '{print $2}' <<<"$call_result")"
  total_time="$(awk '{print $3}' <<<"$call_result")"
  local latency_ms
  latency_ms="$(awk -v t="${total_time:-0}" 'BEGIN {printf "%.0f", t * 1000}')"

  local mode warnings_json scene_count fallback_warning
  if [[ "$http_code" != "200" ]]; then
    mode="HTTP_ERROR"
    warnings_json="[]"
    scene_count=0
    fallback_warning="true"
  else
    mode="$(jq -r '.mode // ""' "$response_file" 2>/dev/null || echo "")"
    warnings_json="$(jq -c '.warnings // []' "$response_file" 2>/dev/null || echo "[]")"
    scene_count="$(jq -r '(.scenes // []) | length' "$response_file" 2>/dev/null || echo "0")"
    fallback_warning="$(jq -r '((.warnings // []) | any(tostring | contains("llm_preprocess_fallback_to_rule")))' "$response_file" 2>/dev/null || echo "false")"
    if [[ "$fallback_warning" != "true" && "$fallback_warning" != "false" ]]; then
      fallback_warning="false"
    fi
  fi

  upsert_case_result "$key" "$group" "$http_code" "$latency_ms" "$mode" "$fallback_warning" "$scene_count" "$warnings_json"

  if [[ -n "$attempt_label" ]]; then
    RETRY_LOG_LINES+=("retry=${attempt_label} key=${key} http=${http_code} latencyMs=${latency_ms} mode=${mode} fallback=${fallback_warning} scenes=${scene_count} warnings=${warnings_json}")
  fi

  rm -f "$response_file"
}

is_case_failed() {
  local group="$1"
  local http_code="$2"
  local mode="$3"
  local fallback_warning="$4"
  local scene_count="$5"
  local failed=0

  if [[ "$http_code" != "200" ]]; then
    failed=1
  fi
  if [[ "$group" == "D" && "$scene_count" -lt 6 ]]; then
    failed=1
  fi

  if [[ "$VERIFY_MODE" == "fallback_observe" ]]; then
    if [[ "$fallback_warning" != "true" && "$mode" != "rule_generated" ]]; then
      failed=1
    fi
  else
    if [[ "$mode" != "llm_enhanced" ]]; then
      failed=1
    fi
    if [[ "$fallback_warning" == "true" ]]; then
      failed=1
    fi
  fi

  if [[ "$failed" -eq 1 ]]; then
    return 0
  fi
  return 1
}

evaluate_results() {
  LOG_LINES=()
  FAILED_KEYS=()
  TOTAL_RUNS=0
  LLM_EFFECTIVE_COUNT=0
  FALLBACK_COUNT=0
  HTTP_FAILURE_COUNT=0
  DOC11_MIN_SCENE_COUNT=999999
  P95_MS=0
  local latencies_file
  latencies_file="$(mktemp)"

  while IFS=$'\t' read -r key group http_code latency_ms mode fallback_warning scene_count warnings_json; do
    [[ -z "${key:-}" ]] && continue
    TOTAL_RUNS=$((TOTAL_RUNS + 1))
    echo "$latency_ms" >> "$latencies_file"

    if [[ "$mode" == "llm_enhanced" ]]; then
      LLM_EFFECTIVE_COUNT=$((LLM_EFFECTIVE_COUNT + 1))
    fi
    if [[ "$fallback_warning" == "true" || "$mode" != "llm_enhanced" ]]; then
      FALLBACK_COUNT=$((FALLBACK_COUNT + 1))
    fi
    if [[ "$http_code" != "200" ]]; then
      HTTP_FAILURE_COUNT=$((HTTP_FAILURE_COUNT + 1))
    fi
    if [[ "$group" == "D" && "$scene_count" -lt "$DOC11_MIN_SCENE_COUNT" ]]; then
      DOC11_MIN_SCENE_COUNT="$scene_count"
    fi

    if is_case_failed "$group" "$http_code" "$mode" "$fallback_warning" "$scene_count"; then
      FAILED_KEYS+=("$key")
    fi

    LOG_LINES+=("key=${key} group=${group} http=${http_code} latencyMs=${latency_ms} mode=${mode} fallback=${fallback_warning} scenes=${scene_count} warnings=${warnings_json}")
  done < <(sort -t $'\t' -k1,1 "$RESULT_STORE")

  if [[ "$TOTAL_RUNS" -gt 0 ]]; then
    sorted_latencies=()
    while IFS= read -r latency_line; do
      sorted_latencies+=("$latency_line")
    done < <(sort -n "$latencies_file")
    local p95_rank p95_index
    p95_rank=$(( (95 * TOTAL_RUNS + 99) / 100 ))
    p95_index=$(( p95_rank - 1 ))
    P95_MS="${sorted_latencies[$p95_index]}"
  fi
  rm -f "$latencies_file"

  if [[ "$DOC11_MIN_SCENE_COUNT" == "999999" ]]; then
    DOC11_MIN_SCENE_COUNT=0
  fi
}

SHORT_SQL=$'## 冒烟验证\nSELECT 1 AS smoke_col;'
PLAIN_TEXT=$'业务背景：查询零售客户变更。\n限制字段：客户号、机构号。\n无SQL片段。'
DOC09_TEXT="$(cat "$DOC09")"
DOC10_TEXT="$(cat "$DOC10")"
DOC11_TEXT="$(cat "$DOC11")"

for i in $(seq 1 "$RUNS_PER_GROUP"); do
  run_case "A" "$i"
  run_case "B" "$i"
  run_case "C" "$i"
  run_case "D" "$i"
  run_case "E" "$i"
done

evaluate_results
INITIAL_FAILED_COUNT="${#FAILED_KEYS[@]}"

if [[ "$VERIFY_MODE" == "elastic" && "$INITIAL_FAILED_COUNT" -gt 0 && "$INITIAL_FAILED_COUNT" -le "$ELASTIC_MAX_FAIL" ]]; then
  for key in "${FAILED_KEYS[@]}"; do
    local_group="${key%%#*}"
    local_run="${key##*#}"
    for retry_round in $(seq 1 "$ELASTIC_RETRY_PER_FAIL"); do
      run_case "$local_group" "$local_run" "${key}/retry-${retry_round}"
    done
  done
  evaluate_results
fi

FINAL_FAILED_COUNT="${#FAILED_KEYS[@]}"

echo "===== preprocess batch detail ====="
printf '%s\n' "${LOG_LINES[@]}"
if [[ "${#RETRY_LOG_LINES[@]}" -gt 0 ]]; then
  echo "===== preprocess elastic retry detail ====="
  printf '%s\n' "${RETRY_LOG_LINES[@]}"
fi
echo "===== preprocess batch summary ====="
echo "verify_mode=${VERIFY_MODE}"
echo "total_runs=${TOTAL_RUNS}"
echo "llm_effective=${LLM_EFFECTIVE_COUNT}"
echo "fallback_count=${FALLBACK_COUNT}"
echo "http_failures=${HTTP_FAILURE_COUNT}"
echo "p95_latency_ms=${P95_MS}"
echo "doc11_min_scene_count=${DOC11_MIN_SCENE_COUNT}"
echo "initial_failed_cases=${INITIAL_FAILED_COUNT}"
echo "final_failed_cases=${FINAL_FAILED_COUNT}"

EXIT_CODE=0
if [[ "$HTTP_FAILURE_COUNT" -ne 0 ]]; then
  echo "gate_failed: http_failures must be 0"
  EXIT_CODE=1
fi
if [[ "$VERIFY_MODE" == "fallback_observe" ]]; then
  if [[ "$FALLBACK_COUNT" -ne "$TOTAL_RUNS" ]]; then
    echo "gate_failed: fallback_count must equal total_runs in fallback_observe"
    EXIT_CODE=1
  fi
  if [[ "$P95_MS" -gt 45000 ]]; then
    echo "gate_failed: p95_latency_ms must be <= 45000 in fallback_observe"
    EXIT_CODE=1
  fi
else
  if [[ "$LLM_EFFECTIVE_COUNT" -ne "$TOTAL_RUNS" ]]; then
    echo "gate_failed: llm_effective must equal total_runs"
    EXIT_CODE=1
  fi
  if [[ "$FALLBACK_COUNT" -ne 0 ]]; then
    echo "gate_failed: fallback_count must be 0"
    EXIT_CODE=1
  fi
  if [[ "$P95_MS" -gt 35000 ]]; then
    echo "gate_failed: p95_latency_ms must be <= 35000"
    EXIT_CODE=1
  fi
fi
if [[ "$DOC11_MIN_SCENE_COUNT" -lt 6 ]]; then
  echo "gate_failed: doc11_min_scene_count must be >= 6"
  EXIT_CODE=1
fi
if [[ "$VERIFY_MODE" == "elastic" && "$INITIAL_FAILED_COUNT" -gt "$ELASTIC_MAX_FAIL" ]]; then
  echo "gate_failed: initial_failed_cases exceeds ELASTIC_MAX_FAIL"
  EXIT_CODE=1
fi

exit "$EXIT_CODE"
