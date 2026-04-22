import { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ClipboardList, Database, FileJson2, Route, ShieldCheck, TestTube2 } from "lucide-react";
import { apiRequest, parseJsonText } from "../api/client";
import { API_CONTRACTS, buildApiPath } from "../api/contracts";
import {
  UiBadge,
  UiButton,
  UiCard,
  UiEmptyState,
  UiInlineError,
  UiInput,
  UiTextarea,
  describeCoverageStatus,
  describeDecisionStatus,
  describeProjectionStatus,
} from "../components/ui";
import { buildWorkbenchHref, readValidatedWorkbenchContext } from "../navigation/workbenchContext";
import { resolveRuntimeContextState } from "../navigation/workbenchContextReceivers";
import { useAuthStore } from "../store/authStore";

const DEFAULT_FORM = {
  identifierType: "PROTOCOL_NBR",
  identifierValue: "",
  dateFrom: "2021-01-01",
  dateTo: "2021-12-31",
  requestedFields: "",
  purpose: "代发明细知识包验证",
};

function isPayrollScene(scene) {
  const text = [
    scene?.sceneTitle || "",
    scene?.sceneCode || "",
    scene?.sceneType || "",
    scene?.sceneDescription || "",
    scene?.rawInput || "",
  ].join(" ").toLowerCase();
  return ["代发", "工资", "薪资", "payroll"].some((keyword) => text.includes(keyword));
}

function isDetailScene(scene) {
  const text = [
    scene?.sceneTitle || "",
    scene?.sceneCode || "",
    scene?.sceneType || "",
    scene?.sceneDescription || "",
  ].join(" ").toLowerCase();
  return text.includes("明细") || `${scene?.sceneType || ""}`.toUpperCase() === "FACT_DETAIL";
}

function chooseSampleScene(rows) {
  if (!Array.isArray(rows) || rows.length === 0) {
    return null;
  }
  return rows.find((item) => isDetailScene(item)) || rows.find((item) => isPayrollScene(item)) || rows[0];
}

function parseRequestedFields(value) {
  return `${value || ""}`
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function toneByCheck(passed) {
  return passed ? "good" : "bad";
}

function formatDateTime(value) {
  if (!value) {
    return "未生成";
  }
  try {
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(value)).replace(/\//g, "-");
  } catch (_) {
    return `${value}`;
  }
}

function readStringList(value) {
  if (!value) {
    return [];
  }
  if (Array.isArray(value)) {
    return value.map((item) => `${item || ""}`.trim()).filter(Boolean);
  }
  const parsed = parseJsonText(value, []);
  if (Array.isArray(parsed)) {
    return parsed
      .map((item) => {
        if (typeof item === "string") {
          return item.trim();
        }
        if (item && typeof item === "object") {
          return `${item.fieldName || item.fieldCode || item.name || item.code || item.columnName || ""}`.trim();
        }
        return "";
      })
      .filter(Boolean);
  }
  return [];
}

function deriveIdentifierType(inputSlots) {
  for (const item of inputSlots || []) {
    const identifiers = readStringList(item?.identifierCandidatesJson);
    const hit = identifiers.find(Boolean);
    if (hit) {
      return hit;
    }
    const slotCode = `${item?.slotCode || ""}`.toUpperCase();
    const slotName = `${item?.slotName || ""}`.toUpperCase();
    if (slotCode.includes("CUST") || slotName.includes("客户")) {
      return "CUST_ID";
    }
    if (slotCode.includes("PROTOCOL") || slotName.includes("协议")) {
      return "PROTOCOL_NBR";
    }
  }
  return "PROTOCOL_NBR";
}

function deriveRequestedFields(contractViews, outputContracts) {
  const preferred = contractViews?.[0];
  const viewFields = readStringList(preferred?.visibleFieldsJson);
  if (viewFields.length > 0) {
    return viewFields.slice(0, 6);
  }
  const contractFields = readStringList(outputContracts?.[0]?.fieldsJson);
  if (contractFields.length > 0) {
    return contractFields.slice(0, 6);
  }
  return ["协议号", "交易日期", "金额"];
}

function buildIdentifierOptions(inputSlots, currentValue) {
  return Array.from(new Set([
    ...((inputSlots || []).flatMap((item) => readStringList(item.identifierCandidatesJson))),
    `${currentValue || ""}`.trim(),
  ].filter(Boolean)));
}

function normalizeErrorMessage(error, fallback) {
  const raw = `${error?.message || ""}`.trim();
  if (!raw) {
    return fallback;
  }
  if (/failed to fetch|networkerror|load failed/i.test(raw)) {
    return fallback;
  }
  return raw;
}

function formatExperimentScore(value) {
  const score = Number(value || 0);
  if (!Number.isFinite(score) || score <= 0) {
    return "--";
  }
  return score > 1 ? `${score.toFixed(2)}` : `${(score * 100).toFixed(0)}%`;
}

async function safeRequest(path, options = {}) {
  try {
    return await apiRequest(path, options);
  } catch (_) {
    return null;
  }
}

async function loadRuntimeScenes(token) {
  const published = await apiRequest(API_CONTRACTS.scenes, {
    token,
    query: { status: "PUBLISHED" },
  });
  return Array.isArray(published) ? published.filter((item) => isPayrollScene(item)) : [];
}

async function loadSceneBundle(sceneId, token) {
  const [
    plans,
    coverages,
    policies,
    contractViews,
    sourceContracts,
    publishCheck,
    versions,
    inputSlots,
    outputContracts,
    projection,
  ] = await Promise.all([
    safeRequest(API_CONTRACTS.plans, { token, query: { sceneId } }),
    safeRequest(API_CONTRACTS.coverageDeclarations, { token, query: { sceneId } }),
    safeRequest(API_CONTRACTS.policies, { token, query: { sceneId } }),
    safeRequest(API_CONTRACTS.contractViews, { token, query: { sceneId } }),
    safeRequest(API_CONTRACTS.sourceContracts, { token, query: { sceneId } }),
    safeRequest(buildApiPath("publishChecks", { sceneId }), { token }),
    safeRequest(buildApiPath("sceneVersions", { id: sceneId }), { token }),
    safeRequest(API_CONTRACTS.inputSlotSchemas, { token, query: { sceneId } }),
    safeRequest(API_CONTRACTS.outputContracts, { token, query: { sceneId } }),
    safeRequest(buildApiPath("graphProjection", { sceneId }), { token }),
  ]);
  return {
    plans: Array.isArray(plans) ? plans : [],
    coverages: Array.isArray(coverages) ? coverages : [],
    policies: Array.isArray(policies) ? policies : [],
    contractViews: Array.isArray(contractViews) ? contractViews : [],
    sourceContracts: Array.isArray(sourceContracts) ? sourceContracts : [],
    publishCheck,
    versions: Array.isArray(versions) ? versions : [],
    inputSlots: Array.isArray(inputSlots) ? inputSlots : [],
    outputContracts: Array.isArray(outputContracts) ? outputContracts : [],
    projection,
  };
}

function buildQueryText(scene, form) {
  const fields = parseRequestedFields(form.requestedFields).join("、");
  return [
    scene?.sceneTitle || "代发明细场景",
    `${form.identifierType}:${form.identifierValue}`,
    form.dateFrom && form.dateTo ? `${form.dateFrom} 至 ${form.dateTo}` : "",
    fields ? `请求字段 ${fields}` : "",
    form.purpose ? `用途 ${form.purpose}` : "",
  ].filter(Boolean).join("，");
}

export function KnowledgePackageWorkbenchPage() {
  const location = useLocation();
  const role = useAuthStore((state) => state.role) || "admin";
  const token = useAuthStore((state) => state.token);
  const [loading, setLoading] = useState(true);
  const [loadTimeout, setLoadTimeout] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [bundleError, setBundleError] = useState("");
  const [queryError, setQueryError] = useState("");
  const [scenes, setScenes] = useState([]);
  const [selectedSceneId, setSelectedSceneId] = useState("");
  const [sceneBundle, setSceneBundle] = useState({
    plans: [],
    coverages: [],
    policies: [],
    contractViews: [],
    sourceContracts: [],
    publishCheck: null,
    versions: [],
    inputSlots: [],
    outputContracts: [],
    projection: null,
  });
  const [form, setForm] = useState(DEFAULT_FORM);
  const [result, setResult] = useState(null);
  const [pipeline, setPipeline] = useState(null);
  const contextValidation = useMemo(
    () => readValidatedWorkbenchContext(location.search, "runtime"),
    [location.search],
  );
  const runtimeContextState = useMemo(
    () => resolveRuntimeContextState(contextValidation.ok ? contextValidation.context : null),
    [contextValidation],
  );
  const contextError = contextValidation.ok ? "" : contextValidation.message;

  const selectedScene = useMemo(
    () => scenes.find((item) => `${item.id}` === `${selectedSceneId}`) || null,
    [scenes, selectedSceneId],
  );

  async function refreshSceneBundle(sceneId, { silent = false } = {}) {
    if (!sceneId) {
      setSceneBundle({
        plans: [],
        coverages: [],
        policies: [],
        contractViews: [],
        sourceContracts: [],
        publishCheck: null,
        versions: [],
        inputSlots: [],
        outputContracts: [],
        projection: null,
      });
      return;
    }
    if (!silent) {
      setBundleError("");
    }
    try {
      const nextBundle = await loadSceneBundle(sceneId, token);
      setSceneBundle(nextBundle);
      const requestedFields = deriveRequestedFields(nextBundle.contractViews, nextBundle.outputContracts);
      setForm((prev) => ({
        ...prev,
        identifierType: deriveIdentifierType(nextBundle.inputSlots),
        requestedFields: requestedFields.join(", "),
        purpose: `${selectedScene?.sceneTitle || "代发明细"} MVP 运行验证`,
      }));
    } catch (error) {
      setBundleError(normalizeErrorMessage(error, "运行样板资产加载失败"));
    }
  }

  async function refreshScenes({ silent = false } = {}) {
    if (silent) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setLoadError("");
    try {
      const nextScenes = await loadRuntimeScenes(token);
      setScenes(nextScenes);
      const currentExists = nextScenes.some((item) => `${item.id}` === `${selectedSceneId}`);
      const nextScene = currentExists ? nextScenes.find((item) => `${item.id}` === `${selectedSceneId}`) : chooseSampleScene(nextScenes);
      const nextSceneId = nextScene ? `${nextScene.id}` : "";
      setSelectedSceneId(nextSceneId);
      await refreshSceneBundle(nextSceneId, { silent: true });
    } catch (error) {
      setLoadError(normalizeErrorMessage(error, "运行样板场景加载失败"));
      setScenes([]);
      setSelectedSceneId("");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  useEffect(() => {
    refreshScenes();
    // refreshScenes internally保留当前选择，不需要重复依赖 selectedSceneId。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    if (!loading) {
      setLoadTimeout(false);
      return undefined;
    }
    const timerId = window.setTimeout(() => {
      setLoadTimeout(true);
    }, 8000);
    return () => window.clearTimeout(timerId);
  }, [loading]);

  useEffect(() => {
    const requestedFields = runtimeContextState.requestedFields.join(", ");
    if (!runtimeContextState.sceneCode && !requestedFields && !runtimeContextState.purpose) {
      return;
    }
    setForm((prev) => ({
      ...prev,
      requestedFields: requestedFields || prev.requestedFields,
      purpose: runtimeContextState.purpose || prev.purpose,
    }));
  }, [runtimeContextState.purpose, runtimeContextState.requestedFields, runtimeContextState.sceneCode]);

  useEffect(() => {
    if (!runtimeContextState.sceneCode || scenes.length === 0) {
      return;
    }
    const matched = scenes.find((item) => `${item?.sceneCode || ""}`.trim() === runtimeContextState.sceneCode);
    if (!matched || `${matched.id}` === `${selectedSceneId}`) {
      return;
    }
    setSelectedSceneId(`${matched.id}`);
    refreshSceneBundle(`${matched.id}`, { silent: true });
    // refreshSceneBundle intentionally uses latest in-render token/state;
    // depending on it here would retrigger this handoff effect on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [runtimeContextState.sceneCode, scenes, selectedSceneId]);

  useEffect(() => {
    if (!selectedScene) {
      return;
    }
    setForm((prev) => ({
      ...prev,
      purpose: `${selectedScene.sceneTitle || "代发明细"} MVP 运行验证`,
    }));
  }, [selectedScene]);

  async function handleSceneChange(nextSceneId) {
    setSelectedSceneId(nextSceneId);
    setResult(null);
    setPipeline(null);
    setQueryError("");
    await refreshSceneBundle(nextSceneId);
  }

  const latestVersion = sceneBundle.versions?.[0] || null;

  function handleClearResult() {
    setResult(null);
    setPipeline(null);
    setQueryError("");
  }

  async function submitQueryText(queryText) {
    if (!selectedScene) {
      return;
    }
    setSubmitting(true);
    setQueryError("");
    try {
      const sceneSearch = await apiRequest(API_CONTRACTS.sceneSearch, {
        token,
        method: "POST",
        body: {
          queryText,
          mode: "GLOBAL",
          domainId: selectedScene.domainId || null,
          operator: role,
        },
      });
      const planSelect = await apiRequest(API_CONTRACTS.planSelect, {
        token,
        method: "POST",
        body: {
          queryText,
          mode: "LOCAL",
          sceneId: selectedScene.id,
          operator: role,
        },
      });
      const payload = await apiRequest(API_CONTRACTS.graphQuery, {
        token,
        method: "POST",
        body: {
          queryText,
          snapshotId: latestVersion?.id || null,
          selectedSceneId: selectedScene.id,
          selectedPlanId: planSelect?.candidates?.[0]?.planId || null,
          slotHintsJson: JSON.stringify({
            identifierType: form.identifierType,
            identifierValue: form.identifierValue.trim(),
            requestedFields: parseRequestedFields(form.requestedFields),
          }),
          identifierType: form.identifierType,
          identifierValue: form.identifierValue.trim(),
          dateFrom: form.dateFrom,
          dateTo: form.dateTo,
          requestedFields: parseRequestedFields(form.requestedFields),
          purpose: form.purpose.trim(),
          operator: role,
        },
      });
      setPipeline({
        queryText,
        sceneSearch,
        planSelect,
        generatedAt: new Date().toISOString(),
      });
      setResult(payload);
    } catch (error) {
      if (error.code === "IDENTIFIER_REQUIRED") {
        setQueryError("请提供协议号或客户号以执行检索。");
      } else {
        setQueryError(normalizeErrorMessage(error, "知识包查询失败"));
      }
      setResult(null);
      setPipeline(null);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const queryText = buildQueryText(selectedScene, form);
    await submitQueryText(queryText);
  }

  async function handleSubQuestionSubmit(subQuestion) {
    await submitQueryText(subQuestion);
  }

  const requestedFieldCount = parseRequestedFields(form.requestedFields).length;
  const identifierOptions = useMemo(
    () => buildIdentifierOptions(sceneBundle.inputSlots, form.identifierType),
    [form.identifierType, sceneBundle.inputSlots],
  );
  const resultAssetRef = useMemo(() => {
    const planCode = `${result?.plan?.planCode || ""}`.trim();
    const sceneCode = `${result?.scene?.sceneCode || ""}`.trim();
    if (planCode) {
      return `plan:${planCode}`;
    }
    if (sceneCode) {
      return `scene:${sceneCode}`;
    }
    return "";
  }, [result?.plan?.planCode, result?.scene?.sceneCode]);
  const runtimeToApprovalHref = useMemo(
    () => buildWorkbenchHref("/approval", {
      source_workbench: "runtime",
      target_workbench: "approval",
      intent: "submit_approval",
      trace_id: result?.trace?.traceId,
      scene_code: result?.scene?.sceneCode,
      plan_code: result?.plan?.planCode,
      snapshot_id: result?.trace?.snapshotId,
      inference_snapshot_id: result?.trace?.inferenceSnapshotId,
      requested_fields: result?.contract?.visibleFields || [],
      purpose: form.purpose.trim(),
      lock_mode: "frozen",
    }),
    [
      form.purpose,
      result?.contract?.visibleFields,
      result?.plan?.planCode,
      result?.scene?.sceneCode,
      result?.trace?.inferenceSnapshotId,
      result?.trace?.snapshotId,
      result?.trace?.traceId,
    ],
  );
  const runtimeToMapHref = useMemo(
    () => buildWorkbenchHref("/map", {
      source_workbench: "runtime",
      target_workbench: "map",
      intent: "view_node",
      trace_id: result?.trace?.traceId,
      scene_code: result?.scene?.sceneCode,
      plan_code: result?.plan?.planCode,
      asset_ref: resultAssetRef,
      coverage_segment_id: result?.coverage?.matchedSegment,
      evidence_refs: (result?.evidence || []).map((item) => item?.evidenceCode || item?.title || "").filter(Boolean),
      lock_mode: "latest",
    }),
    [
      result?.coverage?.matchedSegment,
      result?.evidence,
      result?.plan?.planCode,
      result?.scene?.sceneCode,
      result?.trace?.traceId,
      resultAssetRef,
    ],
  );
  const hasApprovalSnapshotPair = Boolean(result?.trace?.snapshotId && result?.trace?.inferenceSnapshotId);

  if (loading) {
    return (
      <section className="panel knowledge-package-page runtime-workbench-page">
        {runtimeContextState.banner ? (
          <div className={`workbench-route-notice ${runtimeContextState.banner.tone}`} role="status">
            <strong>{runtimeContextState.banner.title}</strong>
            <span>{runtimeContextState.banner.message}</span>
          </div>
        ) : null}
        {contextError ? <UiInlineError message={contextError} /> : null}
        <div className="panel-head">
          <div>
            <h2>运行决策台</h2>
            <p>正在加载已发布的代发明细场景与知识包样板资产…</p>
          </div>
        </div>
        {loadTimeout ? (
          <UiInlineError
            message={loadError || "加载超时，请检查后端服务或稍后重试。"}
            actionText="重新加载"
            onAction={() => refreshScenes()}
          />
        ) : null}
      </section>
    );
  }

  if (scenes.length === 0) {
    return (
      <section className="panel knowledge-package-page runtime-workbench-page">
        {runtimeContextState.banner ? (
          <div className={`workbench-route-notice ${runtimeContextState.banner.tone}`} role="status">
            <strong>{runtimeContextState.banner.title}</strong>
            <span>{runtimeContextState.banner.message}</span>
          </div>
        ) : null}
        {contextError ? <UiInlineError message={contextError} /> : null}
        <div className="panel-head">
          <div>
            <h2>运行决策台</h2>
            <p>当前 MVP 只验证“代发 / 薪资域”多场景检索闭环，请先完成样例材料导入与发布。</p>
          </div>
        </div>
        <UiEmptyState
          icon={<TestTube2 size={24} strokeWidth={1.7} />}
          title="尚未找到可运行的已发布样板"
          description={loadError || "请先在知识生产台导入并发布“代发明细”样例场景，然后再进入运行决策台。"}
          action={(
            <UiButton as={Link} to="/production/ingest">
              去知识生产台
            </UiButton>
          )}
        />
      </section>
    );
  }

  return (
    <section className="panel knowledge-package-page runtime-workbench-page">
      {runtimeContextState.banner ? (
        <div className={`workbench-route-notice ${runtimeContextState.banner.tone}`} role="status">
          <strong>{runtimeContextState.banner.title}</strong>
          <span>{runtimeContextState.banner.message}</span>
        </div>
      ) : null}
      <div className="panel-head">
        <div>
          <h2>运行决策台</h2>
          <p>这里直接串起场景召回、方案选择和 Knowledge Package（知识包）返回，用于验证检索链路是否闭环。</p>
        </div>
        <div className="knowledge-package-page-actions">
          <UiBadge tone="neutral">MVP 检索验证</UiBadge>
          <UiButton
            variant="secondary"
            loading={refreshing}
            disabled={runtimeContextState.disableRefresh}
            onClick={() => refreshScenes({ silent: true })}
          >
            刷新场景
          </UiButton>
        </div>
      </div>

      {contextError ? <UiInlineError message={contextError} /> : null}
      {loadError ? <UiInlineError message={loadError} actionText="重新加载" onAction={() => refreshScenes()} /> : null}
      {bundleError ? <UiInlineError message={bundleError} actionText="刷新当前场景" onAction={() => refreshSceneBundle(selectedSceneId)} /> : null}

      {(() => {
        const projectionStatus = sceneBundle.projection?.status
          ? describeProjectionStatus(sceneBundle.projection.status)
          : { label: "未投影", tone: "neutral" };
        return (
      <div className="workbench-metric-strip">
        <UiCard className="workbench-metric-card"><span>已发布样板</span><strong>{scenes.length}</strong><small>当前只展示可运行场景</small></UiCard>
        <UiCard className="workbench-metric-card"><span>已声明方案</span><strong>{sceneBundle.plans.length}</strong><small>来自真实方案资产</small></UiCard>
        <UiCard className="workbench-metric-card"><span>覆盖分段</span><strong>{sceneBundle.coverages.length}</strong><small>用于时段命中判断</small></UiCard>
        <UiCard className="workbench-metric-card"><span>图谱投影</span><strong>{projectionStatus.label}</strong><small>{formatDateTime(sceneBundle.projection?.lastProjectedAt)}</small></UiCard>
      </div>
        );
      })()}

      <div className="knowledge-package-grid">
        <div className="knowledge-package-column">
          <UiCard className="knowledge-package-card" elevation="card">
            <div className="knowledge-package-card-head">
              <div>
                <h3>当前样板</h3>
                <p className="subtle-note">运行决策台默认只读取已发布的代发 / 薪资域场景。</p>
              </div>
              {(() => {
                const projectionStatus = sceneBundle.projection?.status
                  ? describeProjectionStatus(sceneBundle.projection.status)
                  : { label: "未投影", tone: "neutral" };
                return <UiBadge tone={projectionStatus.tone}>{projectionStatus.label}</UiBadge>;
              })()}
            </div>
            <label className="knowledge-package-field">
              <span>选择场景</span>
              <select value={selectedSceneId} onChange={(event) => handleSceneChange(event.target.value)}>
                {scenes.map((item) => (
                  <option key={item.id} value={item.id}>{item.sceneTitle}</option>
                ))}
              </select>
            </label>
            {selectedScene ? (
              <dl className="knowledge-package-kv">
                <div><dt>场景编码</dt><dd>{selectedScene.sceneCode || "-"}</dd></div>
                <div><dt>场景类型</dt><dd>{selectedScene.sceneType || "未设置"}</dd></div>
                <div><dt>最新快照</dt><dd>{latestVersion?.versionTag || "未生成"}</dd></div>
                <div><dt>发布时间</dt><dd>{formatDateTime(latestVersion?.publishedAt || selectedScene.publishedAt)}</dd></div>
              </dl>
            ) : null}
          </UiCard>

          <UiCard className="knowledge-package-card" elevation="card">
            <div className="knowledge-package-card-head">
              <div>
                <h3>已发布资产摘要</h3>
                <p className="subtle-note">先确认契约、策略和覆盖都已到位，再执行知识包验证。</p>
              </div>
              <Database size={18} strokeWidth={1.8} />
            </div>
            <div className="knowledge-package-list">
              <article className="knowledge-package-list-item">
                <div>
                  <strong>方案资产</strong>
                  <p>{sceneBundle.plans.map((item) => item.planName || item.planCode).join("、") || "暂无方案"}</p>
                </div>
                <UiBadge tone="neutral">{sceneBundle.plans.length}</UiBadge>
              </article>
              <article className="knowledge-package-list-item">
                <div>
                  <strong>输入槽位</strong>
                  <p>{sceneBundle.inputSlots.map((item) => item.slotName || item.slotCode).join("、") || "暂无槽位"}</p>
                </div>
                <UiBadge tone="neutral">{sceneBundle.inputSlots.length}</UiBadge>
              </article>
              <article className="knowledge-package-list-item">
                <div>
                  <strong>输出契约</strong>
                  <p>{deriveRequestedFields(sceneBundle.contractViews, sceneBundle.outputContracts).join("、")}</p>
                </div>
                <UiBadge tone="neutral">{sceneBundle.outputContracts.length}</UiBadge>
              </article>
            </div>
          </UiCard>

          <UiCard className="knowledge-package-card" elevation="card">
            <div className="knowledge-package-card-head">
              <div>
                <h3>覆盖与门禁</h3>
                <p className="subtle-note">运行前先确认覆盖段和发布门禁都能成立。</p>
              </div>
              <ShieldCheck size={18} strokeWidth={1.8} />
            </div>
            <div className="knowledge-package-coverage-list">
              {sceneBundle.coverages.map((item) => {
                const coverageStatus = describeCoverageStatus(item.coverageStatus || item.status);
                return (
                  <article key={item.id || item.coverageCode} className="knowledge-package-coverage-item">
                    <div>
                      <strong>{item.coverageTitle || item.coverageCode}</strong>
                      <p>{item.applicablePeriod || item.statementText || "未补充说明"}</p>
                    </div>
                    <UiBadge tone={coverageStatus.tone}>{coverageStatus.label}</UiBadge>
                  </article>
                );
              })}
              {!sceneBundle.coverages.length ? (
                <p className="subtle-note">当前场景还没有已发布覆盖声明。</p>
              ) : null}
            </div>
            <div className="knowledge-package-check-list">
              {(sceneBundle.publishCheck?.items || []).slice(0, 5).map((item) => (
                <article key={item.key} className="knowledge-package-check-item">
                  <div>
                    <strong>{item.name}</strong>
                    <p>{item.message}</p>
                  </div>
                  <UiBadge tone={toneByCheck(item.passed)}>{item.passed ? "通过" : "阻断"}</UiBadge>
                </article>
              ))}
            </div>
          </UiCard>
        </div>

        <div className="knowledge-package-column">
          <UiCard className="knowledge-package-card" elevation="card">
            <div className="knowledge-package-card-head">
              <div>
                <h3>结构化查询</h3>
                <p className="subtle-note">先输入问题、标识、时间范围和请求字段，再并行查看召回过程和知识包结果。</p>
              </div>
              <UiBadge tone="neutral">操作人：{role}</UiBadge>
            </div>
            <form className="knowledge-package-form" onSubmit={handleSubmit}>
              <label className="knowledge-package-field">
                <span>问题输入</span>
                <UiTextarea
                  rows={3}
                  value={buildQueryText(selectedScene, form)}
                  readOnly
                  hint="当前问题画像会同时传给场景召回、方案选择与知识包组装。"
                />
              </label>
              <label className="knowledge-package-field">
                <span>标识类型</span>
                <select value={form.identifierType} onChange={(event) => setForm((prev) => ({ ...prev, identifierType: event.target.value }))}>
                  {identifierOptions.map((item) => (
                    <option key={item} value={item}>{item}</option>
                  ))}
                </select>
              </label>
              <label className="knowledge-package-field">
                <span>标识值</span>
                <UiInput
                  value={form.identifierValue}
                  onChange={(event) => setForm((prev) => ({ ...prev, identifierValue: event.target.value }))}
                  placeholder="请输入协议号或客户号"
                />
              </label>
              <div className="knowledge-package-form-grid">
                <label className="knowledge-package-field">
                  <span>开始日期</span>
                  <UiInput type="date" value={form.dateFrom} onChange={(event) => setForm((prev) => ({ ...prev, dateFrom: event.target.value }))} />
                </label>
                <label className="knowledge-package-field">
                  <span>结束日期</span>
                  <UiInput type="date" value={form.dateTo} onChange={(event) => setForm((prev) => ({ ...prev, dateTo: event.target.value }))} />
                </label>
              </div>
              <label className="knowledge-package-field">
                <span>请求字段</span>
                <UiTextarea
                  rows={4}
                  value={form.requestedFields}
                  onChange={(event) => setForm((prev) => ({ ...prev, requestedFields: event.target.value }))}
                  placeholder="多个字段请用逗号或换行分隔"
                  hint="当前 MVP 依赖契约视图做字段裁剪，命中受限字段会返回 need_approval。"
                />
              </label>
              <label className="knowledge-package-field">
                <span>用途说明</span>
                <UiInput
                  value={form.purpose}
                  onChange={(event) => setForm((prev) => ({ ...prev, purpose: event.target.value }))}
                  placeholder="请输入本次验证用途"
                />
              </label>
              <div className="knowledge-package-form-actions">
                <UiButton type="submit" loading={submitting} disabled={runtimeContextState.disableSubmit}>生成知识包</UiButton>
              </div>
            </form>
            {queryError ? <UiInlineError message={queryError} /> : null}
          </UiCard>

          {pipeline ? (
            <UiCard className="knowledge-package-card" elevation="card">
              <div className="knowledge-package-card-head">
                <div>
                  <h3>检索过程</h3>
                  <p className="subtle-note">这里显式展示 Scene Recall（场景召回）和 Plan Selection（方案选择）的实时结果。</p>
                </div>
                <Route size={18} strokeWidth={1.8} />
              </div>
              <div className="runtime-process-list">
                <article className="runtime-process-item">
                  <strong>输入查询画像</strong>
                  <p>{pipeline.queryText}</p>
                  <span>{formatDateTime(pipeline.generatedAt)}</span>
                </article>
                <article className="runtime-process-item">
                  <strong>场景召回</strong>
                  <p>{pipeline.sceneSearch?.reasons?.join("；") || "未返回召回说明"}</p>
                  <span>场景候选数 {pipeline.sceneSearch?.candidates?.length || 0} 个</span>
                </article>
                <article className="runtime-process-item">
                  <strong>方案选择</strong>
                  <p>{pipeline.planSelect?.reasons?.join("；") || "未返回方案选择说明"}</p>
                  <span>{pipeline.planSelect?.candidates?.[0]?.planName || "未命中首选方案"}</span>
                </article>
              </div>
              <div className="knowledge-package-list">
                {pipeline.planSelect?.candidates?.slice(0, 3).map((item) => {
                  const decisionStatus = describeDecisionStatus(item.decision);
                  return (
                    <article key={`${item.sceneId}-${item.planId}`} className="knowledge-package-list-item">
                      <div>
                        <strong>{item.planName || item.planCode}</strong>
                        <p>{item.sceneTitle || item.sceneCode} · {item.sourceTables?.join("、") || "未声明来源表"}</p>
                      </div>
                      <UiBadge tone={decisionStatus.tone}>{decisionStatus.label}</UiBadge>
                    </article>
                  );
                })}
              </div>
            </UiCard>
          ) : null}

          {result ? (
            <>
              {result.clarification ? (
                <UiCard className="knowledge-package-card" elevation="card">
                  <div className="knowledge-package-card-head">
                    <div>
                      <h3>需要补充条件</h3>
                      <p className="subtle-note">当前请求已命中跨场景多意图，系统返回知识拆解结果而不是伪造混合知识包。</p>
                    </div>
                    {(() => {
                      const decisionStatus = describeDecisionStatus(result.decision);
                      return <UiBadge tone={decisionStatus.tone}>{decisionStatus.label}</UiBadge>;
                    })()}
                  </div>
                  <p>{result.clarification.summary || "请先拆分子问题后再检索。"}</p>
                  <div className="knowledge-package-detail-grid">
                    <div>
                      <h4>候选场景</h4>
                      <ul>
                        {(result.clarification.sceneCandidates || []).map((item) => (
                          <li key={item.sceneId || item.sceneCode}>{item.sceneTitle || item.sceneCode}{item.snapshotId ? ` · 快照 ${item.snapshotId}` : ""}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>候选方案</h4>
                      <ul>
                        {(result.clarification.planCandidates || []).map((item) => (
                          <li key={`${item.sceneCode || "scene"}-${item.planId || item.planCode}`}>
                            {item.planName || item.planCode}{item.sceneCode ? ` · ${item.sceneCode}` : ""}
                          </li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>建议拆分</h4>
                      <ul>
                        {(result.clarification.subQuestions || []).map((item) => (
                          <li key={item}>
                            {item}
                            <UiButton
                              variant="secondary"
                              disabled={submitting}
                              onClick={() => handleSubQuestionSubmit(item)}
                            >
                              用此子问题检索
                            </UiButton>
                          </li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>合并提示</h4>
                      <ul>
                        {(result.clarification.mergeHints || []).map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>澄清问题</h4>
                      <ul>
                        {(result.clarification.clarificationQuestions || []).map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>追踪信息</h4>
                      <ul>
                        <li>Trace：{result.trace?.traceId || "未生成"}</li>
                        <li>原因编码：{result.reasonCode || "未生成"}</li>
                        <li>运行模式：{result.runtimeMode || "未生成"}</li>
                        <li>降级原因：{(result.degradeReasonCodes || []).join("、") || "无"}</li>
                        <li>推理快照：{result.trace?.inferenceSnapshotId || "未生成"}</li>
                      </ul>
                    </div>
                  </div>
                  <div className="proto-action-row">
                    <UiButton variant="secondary" onClick={handleClearResult}>
                      返回修改查询
                    </UiButton>
                  </div>
                </UiCard>
              ) : (
                <UiCard className="knowledge-package-card" elevation="card">
                  <div className="knowledge-package-card-head">
                    <div>
                      <h3>知识包摘要</h3>
                      <p className="subtle-note">最终返回的是知识包，不是裸字段或 SQL 结果。</p>
                    </div>
                    {(() => {
                      const decisionStatus = describeDecisionStatus(result.decision);
                      return <UiBadge tone={decisionStatus.tone}>{decisionStatus.label}</UiBadge>;
                    })()}
                  </div>
                  <div className="knowledge-package-summary-grid">
                    <div className="knowledge-package-summary-item">
                      <ClipboardList size={16} />
                      <div>
                        <strong>场景 / 方案</strong>
                        <p>{result.scene?.sceneTitle || "未命中场景"} / {result.plan?.planName || "未命中方案"}</p>
                      </div>
                    </div>
                    <div className="knowledge-package-summary-item">
                      <Route size={16} />
                      <div>
                        <strong>覆盖判定</strong>
                        <p>{result.coverage?.status ? describeCoverageStatus(result.coverage.status).label : "未知状态"} · {result.coverage?.matchedSegment || "未命中分段"}</p>
                      </div>
                    </div>
                    <div className="knowledge-package-summary-item">
                      <ShieldCheck size={16} />
                      <div>
                        <strong>策略结果</strong>
                        <p>{result.policy?.decision ? describeDecisionStatus(result.policy.decision).label : describeDecisionStatus(result.decision).label} · 风险等级 {result.risk?.riskLevel || "未知"}</p>
                      </div>
                    </div>
                    <div className="knowledge-package-summary-item">
                      <TestTube2 size={16} />
                      <div>
                        <strong>运行模式 / 降级原因</strong>
                        <p>{result.runtimeMode || "未生成"} · {(result.degradeReasonCodes || []).join("、") || "无降级原因"}</p>
                      </div>
                    </div>
                    <div className="knowledge-package-summary-item">
                      <Database size={16} />
                      <div>
                        <strong>追踪信息</strong>
                        <p>
                          {result.trace?.traceId || "未生成"} / 快照 {result.trace?.snapshotId || "未绑定"} /
                          推理快照 {result.trace?.inferenceSnapshotId || "未绑定"}
                        </p>
                      </div>
                    </div>
                  </div>
                  <div className="proto-action-row">
                    <UiButton as={Link} to={runtimeToMapHref} variant="secondary">
                      去数据地图查看定位
                    </UiButton>
                    <UiButton as={Link} to={runtimeToApprovalHref} variant="secondary" disabled={!hasApprovalSnapshotPair}>
                      提交审批与导出
                    </UiButton>
                  </div>
                  <div className="knowledge-package-detail-grid">
                    <div>
                      <h4>字段裁剪</h4>
                      <ul>
                        <li>可见字段：{result.contract?.visibleFields?.join("、") || "无"}</li>
                        <li>脱敏字段：{result.contract?.maskedFields?.join("、") || "无"}</li>
                        <li>受限字段：{result.contract?.restrictedFields?.join("、") || "无"}</li>
                        <li>禁止字段：{result.contract?.forbiddenFields?.join("、") || "无"}</li>
                      </ul>
                    </div>
                    <div>
                      <h4>解析路径</h4>
                      <ul>
                        {(result.path?.resolutionSteps || []).map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>证据与来源</h4>
                      <ul>
                        {(result.evidence || []).map((item) => (
                          <li key={item.evidenceCode || item.title}>{item.title}{item.sourceAnchor ? ` · ${item.sourceAnchor}` : ""}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>风险说明</h4>
                      <ul>
                        {(result.risk?.riskReasons || []).map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </UiCard>
              )}

              {result.experiment ? (
                <UiCard className="knowledge-package-card" elevation="card">
                  <div className="knowledge-package-card-head">
                    <div>
                      <h3>实验检索调试</h3>
                      <p className="subtle-note">实验侧车只返回候选和引用，不替代正式知识包决策链。</p>
                    </div>
                    <TestTube2 size={18} strokeWidth={1.8} />
                  </div>
                  <div className="knowledge-package-summary-grid">
                    <div className="knowledge-package-summary-item">
                      <TestTube2 size={16} />
                      <div>
                        <strong>{result.experiment.adapterName || result.trace?.retrievalAdapter || "未知适配器"}</strong>
                        <p>检索适配器 · {result.experiment.status || result.trace?.retrievalStatus || "UNKNOWN"}</p>
                      </div>
                    </div>
                    <div className="knowledge-package-summary-item">
                      <ClipboardList size={16} />
                      <div>
                        <strong>{(result.experiment.candidateScenes || []).length} 个候选场景</strong>
                        <p>{result.experiment.summary || "未返回实验摘要"}</p>
                      </div>
                    </div>
                    <div className="knowledge-package-summary-item">
                      <FileJson2 size={16} />
                      <div>
                        <strong>候选引用 {Number(result.experiment.referenceRefs?.length || 0)} 条</strong>
                        <p>{result.experiment.fallbackToFormal ? "已降级回正式链路" : "当前未触发正式链路降级"}</p>
                      </div>
                    </div>
                  </div>
                  <div className="knowledge-package-detail-grid">
                    <div>
                      <h4>候选场景</h4>
                      <ul>
                        {(result.experiment.candidateScenes || []).map((item) => (
                          <li key={`${item.sceneCode || item.sceneId}`}>
                            {item.sceneTitle || item.sceneCode}
                            {item.sceneCode ? ` · ${item.sceneCode}` : ""}
                            {item.score != null ? ` · ${formatExperimentScore(item.score)}` : ""}
                            {item.source ? ` · ${item.source}` : ""}
                          </li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>候选证据</h4>
                      <ul>
                        {(result.experiment.candidateEvidence || []).map((item) => (
                          <li key={`${item.evidenceCode || item.referenceRef || item.title}`}>
                            {item.title || item.evidenceCode}
                            {item.referenceRef ? ` · ${item.referenceRef}` : ""}
                            {item.sourceAnchor ? ` · ${item.sourceAnchor}` : ""}
                            {item.score != null ? ` · ${formatExperimentScore(item.score)}` : ""}
                          </li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>候选引用</h4>
                      <ul>
                        {(result.experiment.referenceRefs || []).map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <h4>分数拆解</h4>
                      <ul>
                        {(result.experiment.scoreBreakdown || []).map((item, index) => (
                          <li key={`${item.label || "score"}-${index}`}>
                            {item.label || "score"} · {formatExperimentScore(item.score)}
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </UiCard>
              ) : null}

              <UiCard className="knowledge-package-card" elevation="card">
                <div className="knowledge-package-card-head">
                  <div>
                    <h3>知识包 JSON</h3>
                    <p className="subtle-note">用于后端联调、trace 回放和页面结果核对。</p>
                  </div>
                  <FileJson2 size={18} strokeWidth={1.8} />
                </div>
                <pre className="knowledge-package-json">{JSON.stringify(result, null, 2)}</pre>
              </UiCard>
            </>
          ) : (
            <UiEmptyState
              icon={<FileJson2 size={24} strokeWidth={1.7} />}
              title="尚未生成知识包"
              description={`当前已选择 ${selectedScene?.sceneTitle || "样板场景"}，请输入标识值并提交一次检索验证。当前请求字段数：${requestedFieldCount}。`}
            />
          )}
        </div>
      </div>
    </section>
  );
}
