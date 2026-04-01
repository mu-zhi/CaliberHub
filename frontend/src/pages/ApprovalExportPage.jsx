import { useEffect, useMemo, useState } from "react";
import { FileDown, ShieldAlert } from "lucide-react";
import { useLocation } from "react-router-dom";
import { apiRequest } from "../api/client";
import { API_CONTRACTS, buildApiPath } from "../api/contracts";
import { UiBadge, UiButton, UiCard, UiInlineError } from "../components/ui";
import { readValidatedWorkbenchContext } from "../navigation/workbenchContext";
import { resolveApprovalContextState } from "../navigation/workbenchContextReceivers";
import { useAuthStore } from "../store/authStore";

const PENDING_APPROVALS = [
  {
    sceneCode: "SCN_PAYROLL_DETAIL",
    applicant: "运行支持-华东",
    scene: "代发明细查询",
    purpose: "工单核验",
    risk: "高",
    submittedAt: "今天 10:32",
    timeoutStatus: "即将超时",
  },
  {
    sceneCode: "SCN_ACCOUNT_OPENING_BRANCH_CHANGE",
    applicant: "数据治理-总行",
    scene: "客户开户机构变更",
    purpose: "规则复核",
    risk: "中",
    submittedAt: "今天 09:18",
    timeoutStatus: "正常",
  },
];

function timeoutTone(timeoutStatus) {
  if (timeoutStatus === "即将超时" || timeoutStatus === "已超时") {
    return "bad";
  }
  if (timeoutStatus === "正常") {
    return "good";
  }
  return "neutral";
}

function formatDateTime(value) {
  if (!value) {
    return "待生成";
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

function mapServiceSpecRecord(item) {
  const specCode = `${item?.specCode || "SPEC-UNKNOWN"}`.trim();
  const specVersion = Number(item?.specVersion || 0);
  return {
    ticket: specVersion > 0 ? `${specCode}#${specVersion}` : specCode,
    fingerprint: `场景 #${item?.sceneId || "-"}`,
    status: "已完成",
    operator: item?.exportedBy || "system",
    exportedAt: formatDateTime(item?.exportedAt),
    maskingPolicy: "按当前契约视图导出",
    archiveStatus: "待归档",
  };
}

export function ApprovalExportPage() {
  const location = useLocation();
  const token = useAuthStore((state) => state.token);
  const [actionMessage, setActionMessage] = useState("");
  const [selectedApprovalIndex, setSelectedApprovalIndex] = useState(0);
  const [exportRecords, setExportRecords] = useState([]);
  const [sceneRows, setSceneRows] = useState([]);
  const [exportLoading, setExportLoading] = useState(true);
  const [exportError, setExportError] = useState("");
  const [exportSubmitting, setExportSubmitting] = useState(false);
  const contextValidation = useMemo(
    () => readValidatedWorkbenchContext(location.search, "approval"),
    [location.search],
  );
  const contextError = contextValidation.ok ? "" : contextValidation.message;
  const approvalContextState = useMemo(
    () => resolveApprovalContextState(contextValidation.ok ? contextValidation.context : null),
    [contextValidation],
  );
  const selectedApproval = PENDING_APPROVALS[selectedApprovalIndex] || PENDING_APPROVALS[0] || null;

  useEffect(() => {
    if (!approvalContextState.summary.sceneCode) {
      return;
    }
    const matchedIndex = PENDING_APPROVALS.findIndex((item) => item.sceneCode === approvalContextState.summary.sceneCode);
    if (matchedIndex >= 0) {
      setSelectedApprovalIndex(matchedIndex);
    }
  }, [approvalContextState.summary.sceneCode]);

  const selectedSceneId = useMemo(() => {
    if (selectedApproval) {
      const sceneMatched = sceneRows.find((item) => item.sceneCode === selectedApproval.sceneCode || item.sceneTitle === selectedApproval.scene);
      if (sceneMatched?.id) {
        return sceneMatched.id;
      }
    }
    if (!approvalContextState.summary.sceneCode) {
      return null;
    }
    const contextMatched = sceneRows.find((item) => item.sceneCode === approvalContextState.summary.sceneCode);
    return contextMatched?.id || null;
  }, [approvalContextState.summary.sceneCode, sceneRows, selectedApproval]);

  useEffect(() => {
    let cancelled = false;

    async function loadInitialData() {
      setExportLoading(true);
      setExportError("");
      try {
        const [scenes, records] = await Promise.all([
          apiRequest(API_CONTRACTS.scenes, {
            token,
            query: { status: "PUBLISHED" },
          }),
          apiRequest(API_CONTRACTS.serviceSpecs, {
            token,
            query: { limit: 20 },
          }),
        ]);
        if (!cancelled) {
          setSceneRows(Array.isArray(scenes) ? scenes : []);
          setExportRecords(Array.isArray(records) ? records.map(mapServiceSpecRecord) : []);
        }
      } catch (_) {
        if (!cancelled) {
          setExportRecords([]);
          setSceneRows([]);
          setExportError("导出记录加载失败，请稍后重试。");
        }
      } finally {
        if (!cancelled) {
          setExportLoading(false);
        }
      }
    }

    loadInitialData();
    return () => {
      cancelled = true;
    };
  }, [token]);

  async function submitAction(action) {
    if (!selectedApproval) {
      return;
    }
    if (action === "approve") {
      setActionMessage(`已同意「${selectedApproval.scene}」申请，等待审批回写结果。`);
      return;
    }
    if (action === "reject") {
      setActionMessage(`已拒绝「${selectedApproval.scene}」申请，申请单将退回申请方。`);
      return;
    }
    if (action === "clarify") {
      setActionMessage(`已要求「${selectedApproval.scene}」补充说明，审批单保持待处理状态。`);
      return;
    }
    if (!selectedSceneId) {
      setExportError("当前申请尚未匹配到已发布场景，无法生成真实导出记录。");
      return;
    }
    setExportSubmitting(true);
    setExportError("");
    try {
      const record = await apiRequest(buildApiPath("serviceSpecExport", { sceneId: selectedSceneId }), {
        method: "POST",
        token,
        body: {
          operator: "system",
        },
      });
      const nextRecord = mapServiceSpecRecord(record);
      setExportRecords((prev) => [nextRecord, ...prev.filter((item) => item.ticket !== nextRecord.ticket)]);
      setActionMessage(`已将「${selectedApproval.scene}」改为脱敏导出，已生成真实导出记录。`);
    } catch (_) {
      setExportError("脱敏导出失败，请稍后重试。");
    } finally {
      setExportSubmitting(false);
    }
  }

  return (
    <section className="panel workbench-page">
      {approvalContextState.banner ? (
        <div className={`workbench-route-notice ${approvalContextState.banner.tone}`} role="status">
          <strong>{approvalContextState.banner.title}</strong>
          <span>{approvalContextState.banner.message}</span>
        </div>
      ) : null}
      <div className="panel-head">
        <div>
          <h2>审批与导出</h2>
          <p>先看审批依据、字段风险和导出边界，再做同意、拒绝、脱敏导出等动作。</p>
        </div>
        <UiBadge tone="warn">待审批 2</UiBadge>
      </div>

      {contextError ? <UiInlineError message={contextError} /> : null}
      {approvalContextState.summary.traceId ? (
        <UiCard className="workbench-pane">
          <div className="proto-card-head">
            <div>
              <h3>冻结审批上下文</h3>
              <p className="subtle-note">审批页收到上下文包后，先固定版本基线，再展示审批依据。</p>
            </div>
          </div>
          <dl className="knowledge-package-kv publish-center-kv">
            <div><dt>追踪编号</dt><dd>{approvalContextState.summary.traceId}</dd></div>
            <div><dt>场景编码</dt><dd>{approvalContextState.summary.sceneCode || "未提供"}</dd></div>
            <div><dt>方案编码</dt><dd>{approvalContextState.summary.planCode || "未提供"}</dd></div>
            <div><dt>控制快照</dt><dd>{approvalContextState.summary.snapshotId || "未提供"}</dd></div>
            <div><dt>推理快照</dt><dd>{approvalContextState.summary.inferenceSnapshotId || "未提供"}</dd></div>
            <div><dt>用途说明</dt><dd>{approvalContextState.summary.purpose || "未提供"}</dd></div>
            <div><dt>请求字段</dt><dd>{(approvalContextState.summary.requestedFields || []).join("、") || "未提供"}</dd></div>
          </dl>
        </UiCard>
      ) : null}

      <div className="workbench-metric-strip">
        <UiCard className="workbench-metric-card"><span>待审批</span><strong>2</strong><small>含 1 个高敏申请</small></UiCard>
        <UiCard className="workbench-metric-card"><span>导出排队</span><strong>1</strong><small>待审批通过后执行</small></UiCard>
        <UiCard className="workbench-metric-card"><span>已归档</span><strong>17</strong><small>近 30 天导出记录</small></UiCard>
        <UiCard className="workbench-metric-card"><span>异常提醒</span><strong>1</strong><small>字段级风险需复核</small></UiCard>
      </div>

      <div className="workbench-grid">
        <UiCard className="workbench-pane">
          <div className="proto-card-head">
            <div>
              <h3>待审批列表</h3>
              <p className="subtle-note">申请人、用途、场景、风险等级和提交时间前置展示。</p>
            </div>
          </div>
          <div className="workbench-list">
            {PENDING_APPROVALS.map((item, index) => (
              <button
                key={`${item.applicant}-${item.scene}`}
                type="button"
                className={`workbench-list-row ${selectedApprovalIndex === index ? "is-active" : ""}`}
                onClick={() => setSelectedApprovalIndex(index)}
              >
                <div>
                  <strong>{item.scene}</strong>
                  <p>{item.applicant} · {item.purpose}</p>
                </div>
                <div className="workbench-row-side">
                  <UiBadge tone={item.risk === "高" ? "bad" : "warn"}>{item.risk}风险</UiBadge>
                  <UiBadge tone={timeoutTone(item.timeoutStatus)}>{item.timeoutStatus || "未标记"}</UiBadge>
                  <span>{item.submittedAt}</span>
                </div>
              </button>
            ))}
          </div>
        </UiCard>

        <UiCard className="workbench-pane">
          <div className="proto-card-head">
            <div>
              <h3>申请详情</h3>
              <p className="subtle-note">字段风险、命中策略和建议动作必须同屏可见。</p>
            </div>
            <ShieldAlert size={18} strokeWidth={1.9} />
          </div>
          {selectedApproval ? (
            <dl className="knowledge-package-kv publish-center-kv">
              <div><dt>当前申请</dt><dd>{selectedApproval.scene}</dd></div>
              <div><dt>申请人</dt><dd>{selectedApproval.applicant}</dd></div>
              <div><dt>用途</dt><dd>{selectedApproval.purpose}</dd></div>
              <div><dt>风险等级</dt><dd>{selectedApproval.risk}风险</dd></div>
            </dl>
          ) : null}
          <ul className="proto-bullet-list">
            <li>命中策略：全量收款账号需要审批，审批通过后可扩展契约视图。</li>
            <li>字段风险：协议号、交易日期、金额可见；收款账号需审批；密码修改日志禁止返回。</li>
            <li>机器建议：改为脱敏导出可直接放行，全量导出需补充说明并保留审计事件。</li>
          </ul>
          <div className="proto-action-row">
            <UiButton onClick={() => submitAction("approve")}>同意</UiButton>
            <UiButton variant="secondary" onClick={() => submitAction("reject")}>拒绝</UiButton>
            <UiButton variant="secondary" onClick={() => submitAction("clarify")}>要求补充说明</UiButton>
            <UiButton
              variant="secondary"
              loading={exportSubmitting}
              onClick={() => submitAction("masked")}
            >
              改为脱敏导出
            </UiButton>
          </div>
          {actionMessage ? <p className="subtle-note" role="status" aria-live="polite">{actionMessage}</p> : null}
        </UiCard>
      </div>

      <UiCard className="workbench-pane">
        <div className="proto-card-head">
          <div>
            <h3>导出记录</h3>
            <p className="subtle-note">审批单号、文件指纹、状态和归档边界统一归到导出记录区。</p>
          </div>
          <FileDown size={18} strokeWidth={1.9} />
        </div>
        {exportError ? <UiInlineError message={exportError} /> : null}
        <div className="workbench-list">
          {exportLoading ? (
            <article className="workbench-list-row">
              <div>
                <strong>导出记录加载中</strong>
                <p>正在从真实后端拉取最近导出记录…</p>
              </div>
            </article>
          ) : null}
          {!exportLoading && exportRecords.length === 0 && !exportError ? (
            <article className="workbench-list-row">
              <div>
                <strong>暂无导出记录</strong>
                <p>当前还没有可展示的真实导出结果。</p>
              </div>
            </article>
          ) : null}
          {exportRecords.map((item) => (
            <article key={item.ticket} className="workbench-list-row">
              <div>
                <strong>{item.ticket}</strong>
                <p>{item.fingerprint}</p>
                <p className="subtle-note approval-export-record-meta">
                  {`执行人：${item.operator} · 导出时间：${item.exportedAt}`}
                </p>
                <p className="subtle-note approval-export-record-meta">
                  {`遮蔽方案：${item.maskingPolicy} · 归档状态：${item.archiveStatus}`}
                </p>
              </div>
              <div className="workbench-row-side">
                <UiBadge tone={item.status === "待执行" ? "warn" : "good"}>{item.status}</UiBadge>
              </div>
            </article>
          ))}
        </div>
      </UiCard>
    </section>
  );
}
