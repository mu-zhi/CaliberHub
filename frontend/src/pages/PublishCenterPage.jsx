import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ClipboardList, Database, RefreshCw, Route, ShieldCheck } from "lucide-react";
import { apiRequest } from "../api/client";
import { API_CONTRACTS, buildApiPath } from "../api/contracts";
import {
  UiBadge,
  UiButton,
  UiCard,
  UiEmptyState,
  UiInlineError,
  UiInput,
  describeProjectionStatus,
  describePublishStatus,
  describeSceneStatus,
} from "../components/ui";
import { buildWorkbenchHref } from "../navigation/workbenchContext";

function nowLocalInputValue() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
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

function normalizeRows(rows) {
  if (!Array.isArray(rows)) {
    return [];
  }
  return [...rows].sort((left, right) => {
    const leftAt = new Date(left?.updatedAt || left?.createdAt || 0).getTime();
    const rightAt = new Date(right?.updatedAt || right?.createdAt || 0).getTime();
    return rightAt - leftAt;
  });
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

function describeCheckResult(item) {
  const level = `${item?.level || ""}`.trim().toLowerCase();
  if (level === "warn" || level === "warning") {
    return { tone: "warn", label: "预警" };
  }
  if (level === "info") {
    return { tone: "neutral", label: "提示" };
  }
  return item?.passed ? { tone: "good", label: "通过" } : { tone: "bad", label: "阻断" };
}

function chooseDefaultScene(rows) {
  if (!Array.isArray(rows) || rows.length === 0) {
    return null;
  }
  return rows.find((item) => item.status !== "PUBLISHED") || rows[0];
}

async function safeRequest(path, options) {
  try {
    return await apiRequest(path, options);
  } catch (_) {
    return null;
  }
}

async function loadSceneBundle(sceneId) {
  const [
    publishCheck,
    versions,
    projection,
    plans,
    coverages,
    policies,
    contractViews,
    sourceContracts,
    outputContracts,
    inputSlots,
  ] = await Promise.all([
    safeRequest(buildApiPath("publishChecks", { sceneId })),
    safeRequest(buildApiPath("sceneVersions", { id: sceneId })),
    safeRequest(buildApiPath("graphProjection", { sceneId })),
    safeRequest(API_CONTRACTS.plans, { query: { sceneId } }),
    safeRequest(API_CONTRACTS.coverageDeclarations, { query: { sceneId } }),
    safeRequest(API_CONTRACTS.policies, { query: { sceneId } }),
    safeRequest(API_CONTRACTS.contractViews, { query: { sceneId } }),
    safeRequest(API_CONTRACTS.sourceContracts, { query: { sceneId } }),
    safeRequest(API_CONTRACTS.outputContracts, { query: { sceneId } }),
    safeRequest(API_CONTRACTS.inputSlotSchemas, { query: { sceneId } }),
  ]);
  return {
    publishCheck,
    versions: Array.isArray(versions) ? versions : [],
    projection,
    plans: Array.isArray(plans) ? plans : [],
    coverages: Array.isArray(coverages) ? coverages : [],
    policies: Array.isArray(policies) ? policies : [],
    contractViews: Array.isArray(contractViews) ? contractViews : [],
    sourceContracts: Array.isArray(sourceContracts) ? sourceContracts : [],
    outputContracts: Array.isArray(outputContracts) ? outputContracts : [],
    inputSlots: Array.isArray(inputSlots) ? inputSlots : [],
  };
}

export function PublishCenterPage() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [rebuilding, setRebuilding] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [error, setError] = useState("");
  const [bundleError, setBundleError] = useState("");
  const [projectionMessage, setProjectionMessage] = useState("");
  const [loadTimeout, setLoadTimeout] = useState(false);
  const [scenes, setScenes] = useState([]);
  const [selectedSceneId, setSelectedSceneId] = useState("");
  const [publishSummary, setPublishSummary] = useState("MVP 样板场景发布");
  const [publishVerifiedAt, setPublishVerifiedAt] = useState(nowLocalInputValue());
  const [bundle, setBundle] = useState({
    publishCheck: null,
    versions: [],
    projection: null,
    plans: [],
    coverages: [],
    policies: [],
    contractViews: [],
    sourceContracts: [],
    outputContracts: [],
    inputSlots: [],
  });

  const selectedScene = useMemo(
    () => scenes.find((item) => `${item.id}` === `${selectedSceneId}`) || null,
    [scenes, selectedSceneId],
  );
  const latestVersion = bundle.versions[0] || null;
  const publishToMapHref = useMemo(
    () => buildWorkbenchHref("/map", {
      source_workbench: "publish",
      target_workbench: "map",
      intent: "view_impact",
      asset_ref: selectedScene?.sceneCode ? `scene:${selectedScene.sceneCode}` : "",
      snapshot_id: latestVersion?.snapshotId,
      inference_snapshot_id: latestVersion?.inferenceSnapshotId,
      lock_mode: "frozen",
    }),
    [latestVersion?.inferenceSnapshotId, latestVersion?.snapshotId, selectedScene?.sceneCode],
  );
  const publishToRuntimeHref = useMemo(
    () => buildWorkbenchHref("/runtime", {
      source_workbench: "publish",
      target_workbench: "runtime",
      intent: "run_query",
      scene_code: selectedScene?.sceneCode,
      plan_code: bundle.plans[0]?.planCode,
      lock_mode: "latest",
    }),
    [bundle.plans, selectedScene?.sceneCode],
  );
  const hasFrozenSnapshotPair = Boolean(latestVersion?.snapshotId && latestVersion?.inferenceSnapshotId);

  const metrics = useMemo(() => {
    const draft = scenes.filter((item) => item.status === "DRAFT").length;
    const published = scenes.filter((item) => item.status === "PUBLISHED").length;
    const discarded = scenes.filter((item) => item.status === "DISCARDED").length;
    const ready = scenes.filter((item) => item.status === "PUBLISHED").length;
    return { draft, published, discarded, ready };
  }, [scenes]);
  const publishBlockReason = useMemo(() => {
    if (!selectedScene) {
      return "请先选择一个场景。";
    }
    if (selectedScene.status === "PUBLISHED") {
      return "当前场景已经发布，无需重复发布。";
    }
    if (!bundle.publishCheck?.publishReady) {
      return "发布检查未通过，请先处理阻断项。";
    }
    if (!`${publishSummary || ""}`.trim()) {
      return "请先填写本次发布摘要。";
    }
    return "";
  }, [bundle.publishCheck?.publishReady, publishSummary, selectedScene]);
  const projectionBlockReason = useMemo(() => {
    const projectionStatus = `${bundle.projection?.status || ""}`.toUpperCase();
    const projectionMessageText = `${bundle.projection?.message || ""}`;
    if (projectionStatus === "SKIPPED" || /图投影已关闭/.test(projectionMessageText)) {
      return "当前环境已关闭图谱投影，重建操作不可用。";
    }
    return "";
  }, [bundle.projection?.message, bundle.projection?.status]);

  useEffect(() => {
    if (!loading) {
      setLoadTimeout(false);
      return undefined;
    }
    const timer = window.setTimeout(() => {
      setLoadTimeout(true);
    }, 8000);
    return () => window.clearTimeout(timer);
  }, [loading]);

  async function refreshSceneBundle(sceneId, { silent = false } = {}) {
    if (!sceneId) {
      setBundle({
        publishCheck: null,
        versions: [],
        projection: null,
        plans: [],
        coverages: [],
        policies: [],
        contractViews: [],
        sourceContracts: [],
        outputContracts: [],
        inputSlots: [],
      });
      return;
    }
    if (!silent) {
      setBundleError("");
    }
    try {
      const nextBundle = await loadSceneBundle(sceneId);
      setBundle(nextBundle);
    } catch (err) {
      setBundleError(normalizeErrorMessage(err, "发布检查加载失败，请稍后重试"));
    }
  }

  async function refreshScenes({ silent = false } = {}) {
    if (silent) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError("");
    try {
      const rows = normalizeRows(await apiRequest(API_CONTRACTS.scenes));
      setScenes(rows);
      const currentExists = rows.some((item) => `${item.id}` === `${selectedSceneId}`);
      const nextScene = currentExists ? rows.find((item) => `${item.id}` === `${selectedSceneId}`) : chooseDefaultScene(rows);
      const nextSceneId = nextScene ? `${nextScene.id}` : "";
      setSelectedSceneId(nextSceneId);
      await refreshSceneBundle(nextSceneId, { silent: true });
    } catch (err) {
      setError(normalizeErrorMessage(err, "发布中心加载失败，请稍后重试"));
      setScenes([]);
      setSelectedSceneId("");
      setBundle({
        publishCheck: null,
        versions: [],
        projection: null,
        plans: [],
        coverages: [],
        policies: [],
        contractViews: [],
        sourceContracts: [],
        outputContracts: [],
        inputSlots: [],
      });
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  useEffect(() => {
    refreshScenes();
    // refreshScenes internally keeps current selection when possible.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleSceneChange(nextSceneId) {
    setSelectedSceneId(nextSceneId);
    setBundleError("");
    setProjectionMessage("");
    await refreshSceneBundle(nextSceneId);
  }

  async function handleRebuildProjection() {
    if (!selectedScene?.id || projectionBlockReason) {
      return;
    }
    setRebuilding(true);
    setBundleError("");
    setProjectionMessage("");
    try {
      const projection = await apiRequest(buildApiPath("graphRebuild", { sceneId: selectedScene.id }), {
        method: "POST",
        body: {},
      });
      setBundle((prev) => ({ ...prev, projection }));
      if (`${projection?.status || ""}`.toUpperCase() === "SKIPPED") {
        setProjectionMessage("当前环境已关闭图谱投影，已切换为关系库运行时。");
      } else {
        setProjectionMessage("已触发图谱投影重建，请关注下方投影状态。");
      }
    } catch (err) {
      setBundleError(normalizeErrorMessage(err, "图谱投影重建失败，请稍后重试"));
    } finally {
      setRebuilding(false);
    }
  }

  async function handlePublish() {
    if (!selectedScene?.id || selectedScene.status === "PUBLISHED") {
      return;
    }
    if (!bundle.publishCheck?.publishReady) {
      setBundleError("当前发布检查未通过，请先补齐阻断项后再发布。");
      return;
    }
    const summary = `${publishSummary || ""}`.trim();
    if (!summary) {
      setBundleError("请先填写本次发布摘要。");
      return;
    }
    setPublishing(true);
    setBundleError("");
    try {
      await apiRequest(buildApiPath("scenePublish", { id: selectedScene.id }), {
        method: "POST",
        body: {
          verifiedAt: new Date(publishVerifiedAt || nowLocalInputValue()).toISOString(),
          changeSummary: summary,
          operator: "",
        },
      });
      await refreshScenes({ silent: true });
    } catch (err) {
      if (err.status === 400 || err.status === 409) {
        setBundleError(err.message || "由于验证失败，无法完成发布，请检查上述发布的阻断项。");
      } else {
        setBundleError(normalizeErrorMessage(err, "执行发布失败，请稍后重试"));
      }
    } finally {
      setPublishing(false);
    }
  }

  if (loading) {
    return (
      <section className="panel workbench-page">
        <div className="panel-head">
          <div>
            <h2>发布中心</h2>
            <p>正在加载候选场景、发布门禁和图谱投影状态…</p>
          </div>
        </div>
        {loadTimeout ? (
          <UiInlineError
            message={error || "加载超时，请检查后端服务或稍后重试。"}
            actionText="重新加载"
            onAction={() => refreshScenes()}
          />
        ) : null}
        {!loadTimeout ? (
          <div className="panel-actions">
            <UiButton variant="secondary" loading={refreshing} onClick={() => refreshScenes({ silent: true })}>
              刷新状态
            </UiButton>
          </div>
        ) : null}
      </section>
    );
  }

  if (scenes.length === 0) {
    return (
      <section className="panel workbench-page">
        <div className="panel-head">
          <div>
            <h2>发布中心</h2>
            <p>先完成知识生产，再在这里查看门禁、快照和投影状态。</p>
          </div>
        </div>
        <UiEmptyState
          icon={<ClipboardList size={24} strokeWidth={1.8} />}
          title="暂无可检查场景"
          description={error || "当前还没有草稿或已发布场景。请先进入知识生产台导入样例材料并生成草稿。"}
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
    <section className="panel workbench-page publish-center-page">
      <div className="panel-head">
        <div>
          <h2>发布中心</h2>
          <p>这里直接读取真实发布门禁、版本快照和图谱投影，不再使用静态演示数据。</p>
        </div>
        <div className="knowledge-package-page-actions">
          <UiBadge tone="neutral">MVP 最小闭环</UiBadge>
          <UiButton variant="secondary" loading={refreshing} icon={<RefreshCw size={15} />} onClick={() => refreshScenes({ silent: true })}>
            刷新状态
          </UiButton>
        </div>
      </div>

      {error ? <UiInlineError message={error} actionText="重新加载" onAction={() => refreshScenes()} /> : null}
      {bundleError ? <UiInlineError message={bundleError} actionText="重新加载当前场景" onAction={() => refreshSceneBundle(selectedSceneId)} /> : null}

      <div className="workbench-metric-strip">
        <UiCard className="workbench-metric-card"><span>草稿场景</span><strong>{metrics.draft}</strong><small>待补充与待发布</small></UiCard>
        <UiCard className="workbench-metric-card"><span>已发布</span><strong>{metrics.published}</strong><small>可进入运行决策台</small></UiCard>
        <UiCard className="workbench-metric-card"><span>已弃用</span><strong>{metrics.discarded}</strong><small>默认不进入检索</small></UiCard>
        <UiCard className="workbench-metric-card"><span>当前门禁</span><strong>{bundle.publishCheck?.publishReady ? "通过" : "待补齐"}</strong><small>实时读取发布检查项</small></UiCard>
      </div>

      <div className="publish-center-layout">
        <UiCard className="workbench-pane publish-center-scene-pane">
          <div className="proto-card-head">
            <div>
              <h3>场景清单</h3>
              <p className="subtle-note">优先处理草稿，再复核已发布场景的快照与投影。</p>
            </div>
          </div>
          <div className="publish-center-scene-list">
            {scenes.map((item) => {
              const sceneStatus = describeSceneStatus(item.status);
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`publish-center-scene-item ${selectedSceneId === `${item.id}` ? "is-active" : ""}`}
                  onClick={() => handleSceneChange(`${item.id}`)}
                >
                  <div>
                    <strong>{item.sceneTitle || `场景 #${item.id}`}</strong>
                    <p>{item.domainName || item.domain || "未绑定业务领域"} · {item.sceneType || "未声明类型"}</p>
                  </div>
                  <div className="workbench-row-side">
                    <UiBadge tone={sceneStatus.tone}>{sceneStatus.label}</UiBadge>
                    <span>{formatDateTime(item.updatedAt || item.createdAt)}</span>
                  </div>
                </button>
              );
            })}
          </div>
        </UiCard>

        <div className="publish-center-detail-grid">
          <UiCard className="workbench-pane">
            {selectedScene ? (
              (() => {
                const selectedSceneStatus = describeSceneStatus(selectedScene.status);
                const projectionStatus = bundle.projection?.status
                  ? describeProjectionStatus(bundle.projection.status)
                  : { label: "未投影", tone: "neutral" };
                return (
                  <>
                    <div className="proto-card-head">
                      <div>
                        <h3>当前场景</h3>
                        <p className="subtle-note">发布门禁、快照和投影状态放在一屏里看完。</p>
                      </div>
                      <UiBadge tone={selectedSceneStatus.tone}>{selectedSceneStatus.label}</UiBadge>
                    </div>
                    <dl className="knowledge-package-kv publish-center-kv">
                      <div><dt>场景名称</dt><dd>{selectedScene.sceneTitle}</dd></div>
                      <div><dt>业务领域</dt><dd>{selectedScene.domainName || selectedScene.domain || "未绑定"}</dd></div>
                      <div><dt>场景类型</dt><dd>{selectedScene.sceneType || "未设置"}</dd></div>
                      <div><dt>最近更新时间</dt><dd>{formatDateTime(selectedScene.updatedAt || selectedScene.createdAt)}</dd></div>
                      <div><dt>最新快照</dt><dd>{bundle.versions[0]?.versionTag || "未生成"}</dd></div>
                      <div><dt>图谱投影</dt><dd>{projectionStatus.label}</dd></div>
                    </dl>
                  </>
                );
              })()
            ) : (
              <>
                <div className="proto-card-head">
                  <div>
                    <h3>当前场景</h3>
                    <p className="subtle-note">发布门禁、快照和投影状态放在一屏里看完。</p>
                  </div>
                  <UiBadge tone="neutral">未选择</UiBadge>
                </div>
                <p className="subtle-note">请选择左侧场景。</p>
              </>
            )}
            <div className="proto-action-row">
              <UiButton as={Link} to="/production/modeling" variant="secondary">
                回知识生产台继续编辑
              </UiButton>
              <UiButton as={Link} to={publishToMapHref} variant="secondary" disabled={!hasFrozenSnapshotPair}>
                去数据地图查看图谱
              </UiButton>
              <UiButton as={Link} to={publishToRuntimeHref} variant="secondary" disabled={!selectedScene?.sceneCode}>
                去运行决策台验证
              </UiButton>
              <UiButton
                variant="secondary"
                loading={rebuilding}
                disabled={Boolean(projectionBlockReason)}
                onClick={handleRebuildProjection}
              >
                重建图谱投影
              </UiButton>
            </div>
            {projectionBlockReason ? (
              <p className="subtle-note" role="status" aria-live="polite">{projectionBlockReason}</p>
            ) : null}
            {!projectionBlockReason && projectionMessage ? (
              <p className="subtle-note" role="status" aria-live="polite">{projectionMessage}</p>
            ) : null}
          </UiCard>

          <UiCard className="workbench-pane">
            <div className="proto-card-head">
              <div>
                <h3>发布检查</h3>
                <p className="subtle-note">门禁项直接读取后端计算结果，缺什么一眼能看见。</p>
              </div>
              <ShieldCheck size={18} strokeWidth={1.9} />
            </div>
            <div className="workbench-list">
              {(bundle.publishCheck?.items || []).map((item) => (
                <article key={item.key} className="workbench-list-row">
                  <div>
                    <strong>{item.name}</strong>
                    <p>{item.message}</p>
                  </div>
                  <div className="workbench-row-side">
                    {(() => {
                      const checkResult = describeCheckResult(item);
                      return <UiBadge tone={checkResult.tone}>{checkResult.label}</UiBadge>;
                    })()}
                  </div>
                </article>
              ))}
              {!bundle.publishCheck?.items?.length ? (
                <p className="subtle-note">当前场景还没有生成发布检查结果。</p>
              ) : null}
            </div>
            <div className="publish-center-ops">
              <UiInput
                type="datetime-local"
                value={publishVerifiedAt}
                onChange={(event) => setPublishVerifiedAt(event.target.value)}
                hint="复核时间会随发布请求一起写入版本快照。"
              />
              <UiInput
                value={publishSummary}
                onChange={(event) => setPublishSummary(event.target.value)}
                placeholder="请输入本次发布摘要"
                hint="建议写明样板范围、变更原因或风险说明。"
              />
              <UiButton
                loading={publishing}
                disabled={Boolean(publishBlockReason)}
                onClick={handlePublish}
              >
                {selectedScene?.status === "PUBLISHED" ? "当前已发布" : "执行发布"}
              </UiButton>
              {publishBlockReason ? (
                <p className="subtle-note" role="status" aria-live="polite">{publishBlockReason}</p>
              ) : null}
            </div>
          </UiCard>

          <UiCard className="workbench-pane">
            <div className="proto-card-head">
              <div>
                <h3>资产计数</h3>
                <p className="subtle-note">这组计数能快速判断当前场景是否具备最小可发布结构。</p>
              </div>
              <Database size={18} strokeWidth={1.9} />
            </div>
            <div className="publish-center-matrix">
              <div><span>方案资产</span><strong>{bundle.plans.length}</strong></div>
              <div><span>覆盖声明</span><strong>{bundle.coverages.length}</strong></div>
              <div><span>策略对象</span><strong>{bundle.policies.length}</strong></div>
              <div><span>契约视图</span><strong>{bundle.contractViews.length}</strong></div>
              <div><span>来源契约</span><strong>{bundle.sourceContracts.length}</strong></div>
              <div><span>输出契约</span><strong>{bundle.outputContracts.length}</strong></div>
              <div><span>输入槽位</span><strong>{bundle.inputSlots.length}</strong></div>
              <div><span>版本快照</span><strong>{bundle.versions.length}</strong></div>
            </div>
          </UiCard>

          <UiCard className="workbench-pane">
            <div className="proto-card-head">
              <div>
                <h3>版本与图谱投影</h3>
                <p className="subtle-note">发布后这里会看到最新快照与投影状态，便于确认图谱已同步。</p>
              </div>
              <Route size={18} strokeWidth={1.9} />
            </div>
            <div className="workbench-list">
              <article className="workbench-list-row">
                <div>
                  <strong>图谱投影状态</strong>
                  <p>{bundle.projection?.message || "尚未生成投影或尚未返回状态消息"}</p>
                </div>
                <div className="workbench-row-side">
                  {(() => {
                    const projectionStatus = bundle.projection?.status
                      ? describeProjectionStatus(bundle.projection.status)
                      : { label: "未投影", tone: "neutral" };
                    return <UiBadge tone={projectionStatus.tone}>{projectionStatus.label}</UiBadge>;
                  })()}
                  <span>{formatDateTime(bundle.projection?.lastProjectedAt || bundle.projection?.updatedAt)}</span>
                </div>
              </article>
              {bundle.versions.slice(0, 4).map((item) => {
                const publishStatus = describePublishStatus(item.publishStatus || "PUBLISHED");
                return (
                  <article key={item.id} className="workbench-list-row">
                    <div>
                      <strong>{item.versionTag || `快照 #${item.id}`}</strong>
                      <p>{item.changeSummary || "未填写变更摘要"}</p>
                    </div>
                    <div className="workbench-row-side">
                      <UiBadge tone={publishStatus.tone}>{publishStatus.label}</UiBadge>
                      <span>{formatDateTime(item.publishedAt || item.createdAt)}</span>
                    </div>
                  </article>
                );
              })}
              {!bundle.versions.length ? (
                <p className="subtle-note">当前场景还没有版本快照。</p>
              ) : null}
            </div>
          </UiCard>
        </div>
      </div>
    </section>
  );
}
