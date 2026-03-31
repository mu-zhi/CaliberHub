#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
OPERATOR="${OPERATOR:-support}"
SUFFIX="${1:-$(date +%Y%m%d%H%M%S)}"

SCENE_TITLE="代发明细查询-样板-${SUFFIX}"
SCENE_DOMAIN="零售金融"
SOURCE_DOC="research/source-materials/sql-samples/05-口径文档现状-代发明细查询.sql"

CURRENT_SQL=$(cat <<'SQL'
SELECT DISTINCT
  EVT_ID,
  AGN_BCH_SEQ,
  DTL_SEQ_NBR,
  EAC_NBR,
  EAC_NM,
  CUST_ID,
  TRX_AMT,
  TRX_DT,
  TRX_TM,
  AGN_STS_CD
FROM PDM_VHIS.T05_AGN_DTL
WHERE AUTO_PAY_ARG_ID IN ('99999')
  AND TRX_DT BETWEEN '2020-01-01' AND '2023-02-12'
ORDER BY TRX_DT;
SQL
)

HISTORY_SQL=$(cat <<'SQL'
SELECT
  TRX_DAT AS 交易日期,
  CNV_NBR AS 合作方协议号,
  EAC_NBR AS 客户户口号,
  TRX_AMT AS 交易金额,
  TRX_STS AS 交易状态
FROM LGC_EAM.UNICORE_EPHISTRXP_YEAR
WHERE TRIM(CNV_NBR) IN ('199829')
  AND TRX_STS = 'S'
  AND TRX_DAT BETWEEN '2011-01-01' AND '2012-12-31'
ORDER BY 1;
SQL
)

api() {
  local method="$1"
  local path="$2"
  local data="${3-}"
  if [[ -n "${data}" ]]; then
    curl -fsS -X "${method}" "${BASE_URL}${path}" \
      -H 'Content-Type: application/json' \
      -d "${data}"
  else
    curl -fsS -X "${method}" "${BASE_URL}${path}"
  fi
}

assert_json() {
  local payload="$1"
  local jq_expr="$2"
  local message="$3"
  if ! jq -e "${jq_expr}" >/dev/null <<<"${payload}"; then
    echo "ASSERT FAILED: ${message}" >&2
    echo "${payload}" | jq . >&2
    exit 1
  fi
}

echo "[1/8] 创建样板 Scene..."
scene_resp=$(api POST /api/scenes "$(jq -n \
  --arg title "${SCENE_TITLE}" \
  --arg domain "${SCENE_DOMAIN}" \
  --arg raw "代发明细样板：支持协议号/客户号双入口，覆盖 2014+ 当前明细、2004-2013 历史明细，以及 2004 前覆盖缺口。" \
  --arg operator "${OPERATOR}" \
  '{
    sceneTitle: $title,
    domain: $domain,
    sceneType: "FACT_DETAIL",
    rawInput: $raw,
    operator: $operator
  }')")

scene_id=$(jq -r '.id' <<<"${scene_resp}")
scene_code=$(jq -r '.sceneCode' <<<"${scene_resp}")

if [[ -z "${scene_id}" || "${scene_id}" == "null" ]]; then
  echo "创建 Scene 失败" >&2
  echo "${scene_resp}" >&2
  exit 1
fi

echo "[2/8] 更新自动生成的 Output Contract / Source Intake Contract..."
output_contract=$(api GET "/api/output-contracts?sceneId=${scene_id}" | jq '.[0]')
output_contract_id=$(jq -r '.id' <<<"${output_contract}")
output_contract_ver=$(jq -r '.rowVersion' <<<"${output_contract}")

api PUT "/api/output-contracts/${output_contract_id}" "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg contractCode "${scene_code}-OUT-01" \
  --arg contractName "代发明细输出契约" \
  --arg summaryText "代发明细样板输出契约，覆盖当前明细与历史明细查询。" \
  --arg fieldsJson '["协议号","交易日期","金额","收款账号","身份证号","银行卡号"]' \
  --arg maskingRulesJson '[{"field":"收款账号","rule":"MASK_TAIL_4"}]' \
  --arg usageConstraints "仅用于样板验证与审批演示，不直接作为执行 SQL 输出。" \
  --arg timeCaliberNote "默认时间语义为交易日期。" \
  --argjson expectedVersion "${output_contract_ver}" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    contractCode: $contractCode,
    contractName: $contractName,
    summaryText: $summaryText,
    fieldsJson: $fieldsJson,
    maskingRulesJson: $maskingRulesJson,
    usageConstraints: $usageConstraints,
    timeCaliberNote: $timeCaliberNote,
    expectedVersion: $expectedVersion,
    operator: $operator
  }')" >/dev/null

source_intake=$(api GET "/api/source-intake-contracts?sceneId=${scene_id}" | jq '.[0]')
source_intake_id=$(jq -r '.id' <<<"${source_intake}")
source_intake_ver=$(jq -r '.rowVersion' <<<"${source_intake}")

api PUT "/api/source-intake-contracts/${source_intake_id}" "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg intakeCode "${scene_code}-SRC-01" \
  --arg intakeName "代发明细来源接入契约" \
  --arg sourceType "SQL_DOC" \
  --arg requiredFieldsJson '["业务场景描述","来源表说明","覆盖说明","敏感级别","SQL样例"]' \
  --arg completenessRule "发布前需确认协议号入口、客户号入口、当前明细来源、历史明细来源、覆盖缺口与敏感字段策略。" \
  --arg gapTaskHint "若覆盖说明或来源表不能验证，请进入 Gap Task 并阻断发布。" \
  --arg sourceTableHintsJson '["NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T","PDM_VHIS.T03_SF_COOP_AGR_INF_S","PDM_VHIS.T05_AGN_DTL","LGC_EAM.EPHISTRXP1","LGC_EAM.UNICORE_EPHISTRXP_YEAR","LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR"]' \
  --arg knownCoverageJson '[{"segment":"2014+","status":"FULL"},{"segment":"2004-2013","status":"PARTIAL"},{"segment":"2004前","status":"GAP"}]' \
  --arg sensitivityLevel "S3" \
  --arg defaultTimeSemantic "交易日期" \
  --arg materialSourceNote "${SOURCE_DOC}" \
  --argjson expectedVersion "${source_intake_ver}" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    intakeCode: $intakeCode,
    intakeName: $intakeName,
    sourceType: $sourceType,
    requiredFieldsJson: $requiredFieldsJson,
    completenessRule: $completenessRule,
    gapTaskHint: $gapTaskHint,
    sourceTableHintsJson: $sourceTableHintsJson,
    knownCoverageJson: $knownCoverageJson,
    sensitivityLevel: $sensitivityLevel,
    defaultTimeSemantic: $defaultTimeSemantic,
    materialSourceNote: $materialSourceNote,
    expectedVersion: $expectedVersion,
    operator: $operator
  }')" >/dev/null

echo "[3/8] 创建 Input Slot..."
protocol_slot=$(api POST /api/input-slot-schemas "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg slotCode "${scene_code}-SLOT-PROTOCOL" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    slotCode: $slotCode,
    slotName: "协议号",
    slotType: "STRING",
    requiredFlag: true,
    identifierCandidatesJson: "[\"PROTOCOL_NBR\"]",
    normalizationRule: "trim + upper",
    clarificationHint: "请输入代发协议号",
    operator: $operator
  }')")

customer_slot=$(api POST /api/input-slot-schemas "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg slotCode "${scene_code}-SLOT-CUST" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    slotCode: $slotCode,
    slotName: "客户号",
    slotType: "STRING",
    requiredFlag: true,
    identifierCandidatesJson: "[\"CUST_ID\"]",
    normalizationRule: "trim + upper",
    clarificationHint: "请输入公司客户号",
    operator: $operator
  }')")

echo "[4/8] 创建 Plan / Coverage / Policy / Evidence / Contract View / Source Contract..."
current_plan=$(api POST /api/plans "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg planCode "${scene_code}-PLN-CURRENT" \
  --arg planName "代发当前明细（2014+）" \
  --arg sourceTablesJson '["PDM_VHIS.T05_AGN_DTL"]' \
  --arg sqlText "${CURRENT_SQL}" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planCode: $planCode,
    planName: $planName,
    applicablePeriod: "2014+",
    defaultTimeSemantic: "交易日期",
    sourceTablesJson: $sourceTablesJson,
    notes: "2014年及以后从统一明细表查询。",
    sqlText: $sqlText,
    confidenceScore: 0.92,
    operator: $operator
  }')")

history_plan=$(api POST /api/plans "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg planCode "${scene_code}-PLN-HISTORY" \
  --arg planName "代发历史明细（2004-2013）" \
  --arg sourceTablesJson '["LGC_EAM.EPHISTRXP1","LGC_EAM.UNICORE_EPHISTRXP_YEAR","LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR"]' \
  --arg sqlText "${HISTORY_SQL}" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planCode: $planCode,
    planName: $planName,
    applicablePeriod: "2004-2013",
    defaultTimeSemantic: "交易日期",
    sourceTablesJson: $sourceTablesJson,
    notes: "历史明细跨多张历史表，需人工确认完整性。",
    sqlText: $sqlText,
    confidenceScore: 0.88,
    operator: $operator
  }')")

current_plan_id=$(jq -r '.id' <<<"${current_plan}")
history_plan_id=$(jq -r '.id' <<<"${history_plan}")

api POST /api/coverage-declarations "$(jq -n \
  --argjson planId "${current_plan_id}" \
  --arg coverageCode "${scene_code}-COV-CURRENT" \
  --arg operator "${OPERATOR}" \
  '{
    planId: $planId,
    coverageCode: $coverageCode,
    coverageTitle: "当前明细覆盖",
    coverageType: "PERIOD_TABLE",
    coverageStatus: "FULL",
    statementText: "2014年及以后代发明细可从当前统一明细表完整覆盖。",
    applicablePeriod: "2014+",
    timeSemantic: "交易日期",
    sourceSystem: "PDM_VHIS",
    sourceTablesJson: "[\"PDM_VHIS.T05_AGN_DTL\"]",
    gapText: "",
    active: true,
    startDate: "2014-01-01",
    endDate: null,
    operator: $operator
  }')" >/dev/null

api POST /api/coverage-declarations "$(jq -n \
  --argjson planId "${history_plan_id}" \
  --arg coverageCode "${scene_code}-COV-HISTORY" \
  --arg operator "${OPERATOR}" \
  '{
    planId: $planId,
    coverageCode: $coverageCode,
    coverageTitle: "历史明细覆盖",
    coverageType: "PERIOD_TABLE",
    coverageStatus: "PARTIAL",
    statementText: "2004-2013历史代发明细可查，但跨年覆盖需结合多表复核。",
    applicablePeriod: "2004-2013",
    timeSemantic: "交易日期",
    sourceSystem: "LGC_EAM/LGC_EDW",
    sourceTablesJson: "[\"LGC_EAM.EPHISTRXP1\",\"LGC_EAM.UNICORE_EPHISTRXP_YEAR\",\"LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR\"]",
    gapText: "2004-2013历史明细存在分段覆盖与缺数风险，默认进入审批。",
    active: true,
    startDate: "2004-01-01",
    endDate: "2013-12-31",
    operator: $operator
  }')" >/dev/null

api POST /api/coverage-declarations "$(jq -n \
  --argjson planId "${history_plan_id}" \
  --arg coverageCode "${scene_code}-COV-GAP" \
  --arg operator "${OPERATOR}" \
  '{
    planId: $planId,
    coverageCode: $coverageCode,
    coverageTitle: "2004年前覆盖缺口",
    coverageType: "PERIOD_TABLE",
    coverageStatus: "GAP",
    statementText: "2004年前代发明细不在当前样板可运行覆盖范围内。",
    applicablePeriod: "2004前",
    timeSemantic: "交易日期",
    sourceSystem: "LEGACY_BRANCH",
    sourceTablesJson: "[]",
    gapText: "2004年前暂无代发明细覆盖",
    active: true,
    startDate: null,
    endDate: "2003-12-31",
    operator: $operator
  }')" >/dev/null

allow_policy=$(api POST /api/policies "$(jq -n \
  --arg policyCode "${scene_code}-PLC-ALLOW" \
  --arg policyName "当前明细普通查询放行" \
  --argjson scopeRefId "${current_plan_id}" \
  --argjson planIds "[${current_plan_id}]" \
  --arg operator "${OPERATOR}" \
  '{
    policyCode: $policyCode,
    policyName: $policyName,
    scopeType: "PLAN",
    scopeRefId: $scopeRefId,
    effectType: "ALLOW",
    conditionText: "普通字段 + FULL 覆盖可直接放行",
    sourceType: "RULE",
    sensitivityLevel: "S1",
    maskingRule: "按 Contract View 输出",
    planIds: $planIds,
    operator: $operator
  }')")

approval_policy=$(api POST /api/policies "$(jq -n \
  --arg policyCode "${scene_code}-PLC-APPROVAL" \
  --arg policyName "历史明细审批策略" \
  --argjson scopeRefId "${history_plan_id}" \
  --argjson planIds "[${history_plan_id}]" \
  --arg operator "${OPERATOR}" \
  '{
    policyCode: $policyCode,
    policyName: $policyName,
    scopeType: "PLAN",
    scopeRefId: $scopeRefId,
    effectType: "REQUIRE_APPROVAL",
    conditionText: "历史明细默认 need_approval",
    sourceType: "RULE",
    sensitivityLevel: "S3",
    maskingRule: "审批通过后按 Contract View 输出",
    planIds: $planIds,
    operator: $operator
  }')")

allow_policy_id=$(jq -r '.id' <<<"${allow_policy}")
approval_policy_id=$(jq -r '.id' <<<"${approval_policy}")

current_evidence=$(api POST /api/evidence-fragments "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg evidenceCode "${scene_code}-EVD-CURRENT" \
  --arg title "当前明细 SQL 样例" \
  --arg fragmentText "${CURRENT_SQL}" \
  --arg sourceAnchor "${SOURCE_DOC}:2014-至今代发明细" \
  --arg sourceRef "${SOURCE_DOC}" \
  --argjson planIds "[${current_plan_id}]" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    evidenceCode: $evidenceCode,
    title: $title,
    fragmentText: $fragmentText,
    sourceAnchor: $sourceAnchor,
    sourceType: "SQL_FRAGMENT",
    sourceRef: $sourceRef,
    confidenceScore: 0.92,
    planIds: $planIds,
    operator: $operator
  }')")

history_evidence=$(api POST /api/evidence-fragments "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg evidenceCode "${scene_code}-EVD-HISTORY" \
  --arg title "历史明细 SQL 样例" \
  --arg fragmentText "${HISTORY_SQL}" \
  --arg sourceAnchor "${SOURCE_DOC}:2009-2012历史明细" \
  --arg sourceRef "${SOURCE_DOC}" \
  --argjson planIds "[${history_plan_id}]" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    evidenceCode: $evidenceCode,
    title: $title,
    fragmentText: $fragmentText,
    sourceAnchor: $sourceAnchor,
    sourceType: "SQL_FRAGMENT",
    sourceRef: $sourceRef,
    confidenceScore: 0.88,
    planIds: $planIds,
    operator: $operator
  }')")

gap_evidence=$(api POST /api/evidence-fragments "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg evidenceCode "${scene_code}-EVD-GAP" \
  --arg title "历史覆盖缺口说明" \
  --arg fragmentText "04年前柜面代发不可查，2004之前的代发明细数据需回到交易流水与纸质代发凭证核验。" \
  --arg sourceAnchor "${SOURCE_DOC}:业务场景描述" \
  --arg sourceRef "${SOURCE_DOC}" \
  --argjson planIds "[${history_plan_id}]" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    evidenceCode: $evidenceCode,
    title: $title,
    fragmentText: $fragmentText,
    sourceAnchor: $sourceAnchor,
    sourceType: "DOC_FRAGMENT",
    sourceRef: $sourceRef,
    confidenceScore: 0.95,
    planIds: $planIds,
    operator: $operator
  }')")

current_evidence_id=$(jq -r '.id' <<<"${current_evidence}")
history_evidence_id=$(jq -r '.id' <<<"${history_evidence}")
gap_evidence_id=$(jq -r '.id' <<<"${gap_evidence}")

api POST /api/contract-views "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --argjson outputContractId "${output_contract_id}" \
  --arg viewCode "${scene_code}-VIEW-GENERAL" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planId: null,
    outputContractId: $outputContractId,
    viewCode: $viewCode,
    viewName: "普通角色视图",
    roleScope: "GENERAL",
    visibleFieldsJson: "[\"协议号\",\"交易日期\",\"金额\",\"收款账号\"]",
    maskedFieldsJson: "[\"收款账号\"]",
    restrictedFieldsJson: "[\"身份证号\"]",
    forbiddenFieldsJson: "[\"银行卡号\"]",
    approvalTemplate: "PAYROLL_HISTORY_APPROVAL",
    operator: $operator
  }')" >/dev/null

api POST /api/source-contracts "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --argjson intakeContractId "${source_intake_id}" \
  --arg code "${scene_code}-SRC-CON-ID-PROTOCOL" \
  --arg intakeRef "Source Intake Contract#${source_intake_id}" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planId: null,
    intakeContractId: $intakeContractId,
    sourceContractCode: $code,
    sourceName: "公司户口号转协议号",
    physicalTable: "NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T",
    sourceRole: "IDENTIFIER_MAPPING",
    identifierType: "PROTOCOL_NBR",
    outputIdentifierType: "PROTOCOL_NBR",
    sourceSystem: "NDS_VHIS",
    timeSemantic: "全量",
    completenessLevel: "FULL",
    sensitivityLevel: "S2",
    startDate: null,
    endDate: null,
    materialSourceNote: $intakeRef,
    notes: "公司户口号可解析代发协议号",
    operator: $operator
  }')" >/dev/null

api POST /api/source-contracts "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --argjson intakeContractId "${source_intake_id}" \
  --arg code "${scene_code}-SRC-CON-ID-CUST" \
  --arg intakeRef "Source Intake Contract#${source_intake_id}" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planId: null,
    intakeContractId: $intakeContractId,
    sourceContractCode: $code,
    sourceName: "客户号转协议号",
    physicalTable: "PDM_VHIS.T03_SF_COOP_AGR_INF_S",
    sourceRole: "IDENTIFIER_MAPPING",
    identifierType: "CUST_ID",
    outputIdentifierType: "PROTOCOL_NBR",
    sourceSystem: "PDM_VHIS",
    timeSemantic: "快照日期",
    completenessLevel: "FULL",
    sensitivityLevel: "S2",
    startDate: null,
    endDate: null,
    materialSourceNote: $intakeRef,
    notes: "客户号先解析为协议号，再进入明细方案",
    operator: $operator
  }')" >/dev/null

api POST /api/source-contracts "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --argjson planId "${current_plan_id}" \
  --argjson intakeContractId "${source_intake_id}" \
  --arg code "${scene_code}-SRC-CON-CURRENT" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planId: $planId,
    intakeContractId: $intakeContractId,
    sourceContractCode: $code,
    sourceName: "当前代发明细来源",
    physicalTable: "PDM_VHIS.T05_AGN_DTL",
    sourceRole: "FACT_CURRENT",
    identifierType: "PROTOCOL_NBR",
    outputIdentifierType: "PROTOCOL_NBR",
    sourceSystem: "PDM_VHIS",
    timeSemantic: "交易日期",
    completenessLevel: "FULL",
    sensitivityLevel: "S2",
    startDate: "2014-01-01",
    endDate: null,
    materialSourceNote: "2014+ 当前明细来源",
    notes: "2014年及以后当前明细主来源",
    operator: $operator
  }')" >/dev/null

api POST /api/source-contracts "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --argjson planId "${history_plan_id}" \
  --argjson intakeContractId "${source_intake_id}" \
  --arg code "${scene_code}-SRC-CON-HIS-1" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planId: $planId,
    intakeContractId: $intakeContractId,
    sourceContractCode: $code,
    sourceName: "历史代发明细来源-2004至2008",
    physicalTable: "LGC_EAM.EPHISTRXP1",
    sourceRole: "FACT_HISTORY",
    identifierType: "PROTOCOL_NBR",
    outputIdentifierType: "PROTOCOL_NBR",
    sourceSystem: "LGC_EAM",
    timeSemantic: "交易日期",
    completenessLevel: "PARTIAL",
    sensitivityLevel: "S2",
    startDate: "2004-07-07",
    endDate: "2008-12-31",
    materialSourceNote: "历史表 2004-2008",
    notes: "历史表存在数据不连续风险",
    operator: $operator
  }')" >/dev/null

api POST /api/source-contracts "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --argjson planId "${history_plan_id}" \
  --argjson intakeContractId "${source_intake_id}" \
  --arg code "${scene_code}-SRC-CON-HIS-2" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planId: $planId,
    intakeContractId: $intakeContractId,
    sourceContractCode: $code,
    sourceName: "历史代发明细来源-2009至2012",
    physicalTable: "LGC_EAM.UNICORE_EPHISTRXP_YEAR",
    sourceRole: "FACT_HISTORY",
    identifierType: "PROTOCOL_NBR",
    outputIdentifierType: "PROTOCOL_NBR",
    sourceSystem: "LGC_EAM",
    timeSemantic: "交易日期",
    completenessLevel: "PARTIAL",
    sensitivityLevel: "S2",
    startDate: "2009-01-01",
    endDate: "2012-12-31",
    materialSourceNote: "历史表 2009-2012",
    notes: "历史表需配合审批使用",
    operator: $operator
  }')" >/dev/null

api POST /api/source-contracts "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --argjson planId "${history_plan_id}" \
  --argjson intakeContractId "${source_intake_id}" \
  --arg code "${scene_code}-SRC-CON-HIS-3" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planId: $planId,
    intakeContractId: $intakeContractId,
    sourceContractCode: $code,
    sourceName: "历史代发明细来源-2013",
    physicalTable: "LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR",
    sourceRole: "FACT_HISTORY",
    identifierType: "PROTOCOL_NBR",
    outputIdentifierType: "PROTOCOL_NBR",
    sourceSystem: "LGC_EDW",
    timeSemantic: "交易日期",
    completenessLevel: "PARTIAL",
    sensitivityLevel: "S2",
    startDate: "2013-01-01",
    endDate: "2013-12-31",
    materialSourceNote: "历史表 2013",
    notes: "2013年覆盖可能缺数",
    operator: $operator
  }')" >/dev/null

current_plan_ver=$(jq -r '.rowVersion' <<<"${current_plan}")
history_plan_ver=$(jq -r '.rowVersion' <<<"${history_plan}")

api PUT "/api/plans/${current_plan_id}" "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg planCode "${scene_code}-PLN-CURRENT" \
  --arg planName "代发当前明细（2014+）" \
  --arg sourceTablesJson '["PDM_VHIS.T05_AGN_DTL"]' \
  --arg sqlText "${CURRENT_SQL}" \
  --argjson expectedVersion "${current_plan_ver}" \
  --argjson evidenceIds "[${current_evidence_id}]" \
  --argjson policyIds "[${allow_policy_id}]" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planCode: $planCode,
    planName: $planName,
    applicablePeriod: "2014+",
    defaultTimeSemantic: "交易日期",
    sourceTablesJson: $sourceTablesJson,
    notes: "2014年及以后从统一明细表查询。",
    sqlText: $sqlText,
    confidenceScore: 0.92,
    expectedVersion: $expectedVersion,
    evidenceIds: $evidenceIds,
    policyIds: $policyIds,
    operator: $operator
  }')" >/dev/null

api PUT "/api/plans/${history_plan_id}" "$(jq -n \
  --argjson sceneId "${scene_id}" \
  --arg planCode "${scene_code}-PLN-HISTORY" \
  --arg planName "代发历史明细（2004-2013）" \
  --arg sourceTablesJson '["LGC_EAM.EPHISTRXP1","LGC_EAM.UNICORE_EPHISTRXP_YEAR","LGC_EDW.ODS_C3A3_A2DPADTLP_YEAR"]' \
  --arg sqlText "${HISTORY_SQL}" \
  --argjson expectedVersion "${history_plan_ver}" \
  --argjson evidenceIds "[${history_evidence_id},${gap_evidence_id}]" \
  --argjson policyIds "[${approval_policy_id}]" \
  --arg operator "${OPERATOR}" \
  '{
    sceneId: $sceneId,
    planCode: $planCode,
    planName: $planName,
    applicablePeriod: "2004-2013",
    defaultTimeSemantic: "交易日期",
    sourceTablesJson: $sourceTablesJson,
    notes: "历史明细跨多张历史表，需人工确认完整性。",
    sqlText: $sqlText,
    confidenceScore: 0.88,
    expectedVersion: $expectedVersion,
    evidenceIds: $evidenceIds,
    policyIds: $policyIds,
    operator: $operator
  }')" >/dev/null

echo "[5/8] 发布前检查..."
publish_check=$(api GET "/api/publish-checks/${scene_id}")
assert_json "${publish_check}" '.publishReady == true' "发布门禁未通过"

echo "[6/8] 发布样板 Scene..."
verified_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
publish_resp=$(api POST "/api/scenes/${scene_id}/publish" "$(jq -n \
  --arg verifiedAt "${verified_at}" \
  --arg changeSummary "首刀代发明细样板发布" \
  --arg operator "${OPERATOR}" \
  '{
    verifiedAt: $verifiedAt,
    changeSummary: $changeSummary,
    operator: $operator
  }')")

assert_json "${publish_resp}" '.status == "PUBLISHED"' "Scene 发布失败"

echo "[7/8] 回放验证 Knowledge Package..."
allow_result=$(api POST /api/graphrag/query "$(jq -n \
  --arg operator "${OPERATOR}" \
  '{
    identifierType: "PROTOCOL_NBR",
    identifierValue: "AGR-2021-0001",
    dateFrom: "2021-01-01",
    dateTo: "2021-12-31",
    requestedFields: ["协议号","交易日期","金额"],
    purpose: "代发样板回放",
    operator: $operator
  }')")
assert_json "${allow_result}" '.decision == "allow" and .coverage.status == "FULL"' "当前明细放行回放失败"

approval_result=$(api POST /api/graphrag/query "$(jq -n \
  --arg operator "${OPERATOR}" \
  '{
    identifierType: "CUST_ID",
    identifierValue: "CUST-2008-0009",
    dateFrom: "2011-01-01",
    dateTo: "2011-12-31",
    requestedFields: ["协议号","身份证号"],
    purpose: "代发样板回放",
    operator: $operator
  }')")
assert_json "${approval_result}" '.decision == "need_approval" and .coverage.status == "PARTIAL"' "历史明细审批回放失败"

gap_result=$(api POST /api/graphrag/query "$(jq -n \
  --arg operator "${OPERATOR}" \
  '{
    identifierType: "PROTOCOL_NBR",
    identifierValue: "AGR-2003-0001",
    dateFrom: "2003-01-01",
    dateTo: "2003-12-31",
    requestedFields: ["协议号","金额"],
    purpose: "代发样板回放",
    operator: $operator
  }')")
assert_json "${gap_result}" '.decision == "deny" and .reasonCode == "COVERAGE_GAP" and .coverage.status == "GAP"' "覆盖缺口回放失败"

restricted_result=$(api POST /api/graphrag/query "$(jq -n \
  --arg operator "${OPERATOR}" \
  '{
    identifierType: "PROTOCOL_NBR",
    identifierValue: "AGR-2021-0002",
    dateFrom: "2021-01-01",
    dateTo: "2021-12-31",
    requestedFields: ["协议号","身份证号"],
    purpose: "代发样板回放",
    operator: $operator
  }')")
assert_json "${restricted_result}" '.decision == "need_approval" and .reasonCode == "FIELD_RESTRICTED"' "受限字段回放失败"

forbidden_result=$(api POST /api/graphrag/query "$(jq -n \
  --arg operator "${OPERATOR}" \
  '{
    identifierType: "PROTOCOL_NBR",
    identifierValue: "AGR-2021-0003",
    dateFrom: "2021-01-01",
    dateTo: "2021-12-31",
    requestedFields: ["协议号","银行卡号"],
    purpose: "代发样板回放",
    operator: $operator
  }')")
assert_json "${forbidden_result}" '.decision == "deny" and .reasonCode == "FIELD_FORBIDDEN"' "禁止字段回放失败"

cross_plan_result=$(api POST /api/graphrag/query "$(jq -n \
  --arg operator "${OPERATOR}" \
  '{
    identifierType: "PROTOCOL_NBR",
    identifierValue: "AGR-2012-2021",
    dateFrom: "2012-01-01",
    dateTo: "2021-12-31",
    requestedFields: ["协议号","金额"],
    purpose: "代发样板回放",
    operator: $operator
  }')")
assert_json "${cross_plan_result}" '.decision == "deny" and .reasonCode == "CROSS_PLAN_RANGE_UNSUPPORTED"' "跨方案时段回放失败"

echo "[8/8] 样板录入完成。"
jq -n \
  --arg sceneId "${scene_id}" \
  --arg sceneCode "${scene_code}" \
  --arg sceneTitle "${SCENE_TITLE}" \
  --arg workbench "${BASE_URL/8080/5173}/assets/knowledge-package" \
  --arg snapshotId "$(jq -r '.trace.snapshotId' <<<"${allow_result}")" \
  --arg versionTag "$(jq -r '.trace.versionTag' <<<"${allow_result}")" \
  '{
    sceneId: ($sceneId | tonumber),
    sceneCode: $sceneCode,
    sceneTitle: $sceneTitle,
    snapshotId: ($snapshotId | tonumber),
    versionTag: $versionTag,
    workbench: $workbench
  }'
