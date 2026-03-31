import { useMemo } from "react";
import { FileDown, ShieldAlert } from "lucide-react";
import { useLocation } from "react-router-dom";
import { UiBadge, UiButton, UiCard, UiInlineError } from "../components/ui";
import { readValidatedWorkbenchContext } from "../navigation/workbenchContext";
import { resolveApprovalContextState } from "../navigation/workbenchContextReceivers";

const PENDING_APPROVALS = [
  { applicant: "运行支持-华东", scene: "代发明细查询", purpose: "工单核验", risk: "高", submittedAt: "今天 10:32" },
  { applicant: "数据治理-总行", scene: "客户开户机构变更", purpose: "规则复核", risk: "中", submittedAt: "今天 09:18" },
];

const EXPORT_RECORDS = [
  { ticket: "APR-20260327-001", fingerprint: "sha256:9f21...ab7d", status: "待执行" },
  { ticket: "APR-20260326-017", fingerprint: "sha256:51a8...d2c4", status: "已归档" },
];

export function ApprovalExportPage() {
  const location = useLocation();
  const contextValidation = useMemo(
    () => readValidatedWorkbenchContext(location.search, "approval"),
    [location.search],
  );
  const contextError = contextValidation.ok ? "" : contextValidation.message;
  const approvalContextState = useMemo(
    () => resolveApprovalContextState(contextValidation.ok ? contextValidation.context : null),
    [contextValidation],
  );

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
            {PENDING_APPROVALS.map((item) => (
              <article key={`${item.applicant}-${item.scene}`} className="workbench-list-row">
                <div>
                  <strong>{item.scene}</strong>
                  <p>{item.applicant} · {item.purpose}</p>
                </div>
                <div className="workbench-row-side">
                  <UiBadge tone={item.risk === "高" ? "bad" : "warn"}>{item.risk}风险</UiBadge>
                  <span>{item.submittedAt}</span>
                </div>
              </article>
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
          <ul className="proto-bullet-list">
            <li>命中策略：全量收款账号需要审批，审批通过后可扩展契约视图。</li>
            <li>字段风险：协议号、交易日期、金额可见；收款账号需审批；密码修改日志禁止返回。</li>
            <li>机器建议：改为脱敏导出可直接放行，全量导出需补充说明并保留审计事件。</li>
          </ul>
          <div className="proto-action-row">
            <UiButton>同意</UiButton>
            <UiButton variant="secondary">拒绝</UiButton>
            <UiButton variant="secondary">改为脱敏导出</UiButton>
          </div>
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
        <div className="workbench-list">
          {EXPORT_RECORDS.map((item) => (
            <article key={item.ticket} className="workbench-list-row">
              <div>
                <strong>{item.ticket}</strong>
                <p>{item.fingerprint}</p>
              </div>
              <div className="workbench-row-side">
                <UiBadge tone={item.status === "待执行" ? "warn" : "neutral"}>{item.status}</UiBadge>
              </div>
            </article>
          ))}
        </div>
      </UiCard>
    </section>
  );
}
