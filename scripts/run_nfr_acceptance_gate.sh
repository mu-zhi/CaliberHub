#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
BASE_URL="${BASE_URL:-http://127.0.0.1:8082}"

AUTH_USER="${AUTH_USER:-admin}"
AUTH_PASS="${AUTH_PASS:-admin123}"
VERIFY_MODE="${VERIFY_MODE:-strict}"
ELASTIC_MAX_FAIL="${ELASTIC_MAX_FAIL:-1}"
ELASTIC_RETRY_PER_FAIL="${ELASTIC_RETRY_PER_FAIL:-1}"
NFR_PROFILE="${NFR_PROFILE:-llm_strict}"
RUNS_PER_GROUP="${RUNS_PER_GROUP:-4}"
CURL_MAX_TIME="${CURL_MAX_TIME:-120}"

if ! command -v jq >/dev/null 2>&1; then
  echo "missing dependency: jq"
  exit 1
fi

if [[ "${NFR_PROFILE}" != "llm_strict" && "${NFR_PROFILE}" != "fallback_observe" ]]; then
  echo "invalid NFR_PROFILE=${NFR_PROFILE}, fallback to llm_strict"
  NFR_PROFILE="llm_strict"
fi

echo "[1/4] 获取认证令牌"
token_response="$(curl -sS -X POST "${BASE_URL%/}/api/system/auth/token" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${AUTH_USER}\",\"password\":\"${AUTH_PASS}\"}")"
AUTH_TOKEN="$(printf '%s' "${token_response}" | jq -r '.accessToken // empty')"
if [[ -z "${AUTH_TOKEN}" ]]; then
  echo "failed to acquire accessToken from ${BASE_URL%/}/api/system/auth/token"
  echo "response=${token_response}"
  exit 1
fi
export AUTH_TOKEN

echo "[1.5/4] 应用 NFR 运行档位: ${NFR_PROFILE}"
if [[ "${NFR_PROFILE}" == "fallback_observe" ]]; then
  curl -sS -X PUT "${BASE_URL%/}/api/system/llm-preprocess-config" \
    -H "Authorization: Bearer ${AUTH_TOKEN}" \
    -H "Content-Type: application/json" \
    -d '{
      "enabled": true,
      "endpoint": "http://127.0.0.1:1/v1/chat/completions",
      "model": "qwen3-max",
      "timeoutSeconds": 5,
      "temperature": 0.0,
      "maxTokens": 2048,
      "enableThinking": false,
      "fallbackToRule": true,
      "operator": "nfr-gate"
    }' >/dev/null
  VERIFY_MODE="fallback_observe"
  if [[ -z "${RUNS_PER_GROUP:-}" || "${RUNS_PER_GROUP}" == "4" ]]; then
    RUNS_PER_GROUP="1"
  fi
  if [[ -z "${CURL_MAX_TIME:-}" || "${CURL_MAX_TIME}" == "120" ]]; then
    CURL_MAX_TIME="45"
  fi
fi

echo "[2/4] 执行导入链路 NFR 验证（P95/失败率/降级）"
nfr_output_file="$(mktemp)"
trap 'rm -f "${nfr_output_file}"' EXIT
(
  cd "${BACKEND_DIR}"
  CURL_MAX_TIME="${CURL_MAX_TIME}" \
  VERIFY_MODE="${VERIFY_MODE}" \
  ELASTIC_MAX_FAIL="${ELASTIC_MAX_FAIL}" \
  ELASTIC_RETRY_PER_FAIL="${ELASTIC_RETRY_PER_FAIL}" \
  RUNS_PER_GROUP="${RUNS_PER_GROUP}" \
  BASE_URL="${BASE_URL}" \
  ./scripts/preprocess_batch_verify.sh
) | tee "${nfr_output_file}"

echo "[3/4] 预热发布链路指标并校验关键指标端点可观测性"
domain_code="NFR_GATE_$(date +%s)"
domain_response="$(curl -sS -X POST "${BASE_URL%/}/api/domains" \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"domainCode\":\"${domain_code}\",\"domainName\":\"NFR门禁域\",\"domainOverview\":\"NFR gate warmup\",\"operator\":\"nfr-gate\"}")"
domain_id="$(printf '%s' "${domain_response}" | jq -r '.id // empty')"
if [[ -z "${domain_id}" ]]; then
  echo "failed to create domain for metric warmup: ${domain_response}"
  exit 1
fi

scene_create="$(curl -sS -X POST "${BASE_URL%/}/api/scenes" \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"sceneTitle":"NFR发布预热","rawInput":"raw","operator":"nfr-gate"}')"
scene_id="$(printf '%s' "${scene_create}" | jq -r '.id // empty')"
if [[ -z "${scene_id}" ]]; then
  echo "failed to create scene for metric warmup: ${scene_create}"
  exit 1
fi

curl -sS -X PUT "${BASE_URL%/}/api/scenes/${scene_id}" \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"sceneTitle\":\"NFR发布预热\",
    \"domainId\":${domain_id},
    \"sceneDescription\":\"指标预热\",
    \"sqlVariantsJson\":\"[{\\\"variant_name\\\":\\\"v1\\\",\\\"sql_text\\\":\\\"select 1\\\",\\\"applicable_period\\\":\\\"2026-至今\\\",\\\"source_tables\\\":[\\\"dm_customer_info\\\"]}]\",
    \"codeMappingsJson\":\"[{\\\"code\\\":\\\"AGN_STS_CD\\\",\\\"mappings\\\":[{\\\"value_code\\\":\\\"01\\\",\\\"value_name\\\":\\\"成功\\\"}]}]\",
    \"operator\":\"nfr-gate\"
  }" >/dev/null

publish_now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
curl -sS -X POST "${BASE_URL%/}/api/scenes/${scene_id}/publish" \
  -H "Authorization: Bearer ${AUTH_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"verifiedAt\":\"${publish_now}\",\"changeSummary\":\"NFR指标预热发布\",\"operator\":\"nfr-gate\"}" >/dev/null

for metric in \
  "caliber.import.preprocess.latency" \
  "caliber.scene.publish.total" \
  "caliber.scene.publish.latency"
do
  metric_response="$(curl -sS "${BASE_URL%/}/actuator/metrics/${metric}")"
  metric_name="$(printf '%s' "${metric_response}" | jq -r '.name // empty')"
  if [[ "${metric_name}" != "${metric}" ]]; then
    echo "metric missing: ${metric}"
    echo "response=${metric_response}"
    exit 1
  fi
done

soft_metric="caliber.scene.publish.soft_gate.total"
soft_metric_response="$(curl -sS "${BASE_URL%/}/actuator/metrics/${soft_metric}" || true)"
soft_metric_name="$(printf '%s' "${soft_metric_response}" | jq -r '.name // empty' 2>/dev/null || true)"
if [[ "${soft_metric_name}" != "${soft_metric}" ]]; then
  echo "warn: optional metric missing: ${soft_metric}"
fi

echo "[4/4] 汇总 NFR 结果"
p95_ms="$(rg -n '^p95_latency_ms=' "${nfr_output_file}" | tail -n 1 | sed -E 's/.*=([0-9]+).*/\1/' || true)"
failed_cases="$(rg -n '^final_failed_cases=' "${nfr_output_file}" | tail -n 1 | sed -E 's/.*=([0-9]+).*/\1/' || true)"
echo "nfr_summary: profile=${NFR_PROFILE}, verify_mode=${VERIFY_MODE}, runs_per_group=${RUNS_PER_GROUP}, p95_latency_ms=${p95_ms:-unknown}, final_failed_cases=${failed_cases:-unknown}"
echo "nfr_acceptance_gate passed."
