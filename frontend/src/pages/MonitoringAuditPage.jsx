import { useMemo } from "react";
import { Activity, AlertTriangle, History } from "lucide-react";
import { Link } from "react-router-dom";
import { UiBadge, UiButton, UiCard, UiInput } from "../components/ui";
import { buildWorkbenchHref } from "../navigation/workbenchContext";

const ALERTS = [
  { title: "历史补查覆盖缺口未清零", detail: "代发明细查询在 2014 年之前仍存在字段缺口。", tone: "warn" },
  { title: "审批模板缺失", detail: "审批后扩展视图尚未绑定模板，发布仍被阻断。", tone: "warn" },
  { title: "图查询超时恢复正常", detail: "近 1 小时无新增图查询超时。", tone: "good" },
];

const TRACE_TIMELINE = [
  "11:02 请求进入：trace_runtime_20260327_07",
  "11:02 场景命中：SCN_PAYROLL_DETAIL",
  "11:02 覆盖判定：PARTIAL_COVERAGE",
  "11:02 策略决策：APPROVAL_REQUIRED",
  "11:03 审批任务生成：APR-20260327-001",
];

const RETRIEVAL_EXPERIMENT_METRICS = [
  { label: "scene hit@5", value: "0.93", detail: "首批 payroll 回放集" },
  { label: "evidence precision@10", value: "0.81", detail: "引用命中前 10" },
  { label: "误放行风险", value: "0", detail: "Policy false allow" },
  { label: "p95", value: "2.3s", detail: "实验侧车延迟" },
];

const INDEX_SYNC_STATUS = {
  indexVersion: "SCN_PAYROLL_DETAIL::SCN_PAYROLL_DETAIL-V001::42",
  manifestStatus: "ACTIVE",
  lockedSnapshot: "42",
  mismatchAlert: "snapshot mismatch 0",
};

export function MonitoringAuditPage() {
  const runtimeReplayHref = useMemo(
    () => buildWorkbenchHref("/runtime", {
      source_workbench: "monitoring",
      target_workbench: "runtime",
      intent: "replay_trace",
      trace_id: "trace_runtime_20260327_07",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      lock_mode: "replay",
    }),
    [],
  );
  const mapReplayHref = useMemo(
    () => buildWorkbenchHref("/map", {
      source_workbench: "monitoring",
      target_workbench: "map",
      intent: "view_node",
      asset_ref: "plan:PLAN_PAYROLL_DETAIL",
      snapshot_id: "snapshot-20260327-01",
      inference_snapshot_id: "inference-20260327-01",
      lock_mode: "replay",
    }),
    [],
  );

  return (
    <section className="panel workbench-page">
      <div className="panel-head">
        <div>
          <h2>监控与审计</h2>
          <p>把运行健康、告警、审计检索和链路回放放到同一工作台里，不再作为运维附属页。</p>
          <p className="subtle-note">当前回放跳转仍使用样例 trace 与快照对，用于验证上下文包链路和只读态表达。</p>
        </div>
        <div className="proto-action-row">
          <UiBadge tone="warn">告警 2</UiBadge>
          <UiButton as={Link} to={runtimeReplayHref} variant="secondary">回放到运行决策台</UiButton>
          <UiButton as={Link} to={mapReplayHref} variant="secondary">回放到数据地图</UiButton>
        </div>
      </div>

      <div className="workbench-metric-strip">
        <UiCard className="workbench-metric-card"><span>场景命中率</span><strong>93%</strong><small>近 24 小时</small></UiCard>
        <UiCard className="workbench-metric-card"><span>发布通过率</span><strong>84%</strong><small>含 2 个阻断发布</small></UiCard>
        <UiCard className="workbench-metric-card"><span>审批积压</span><strong>2</strong><small>均在 SLA 内</small></UiCard>
        <UiCard className="workbench-metric-card"><span>图查询超时</span><strong>0</strong><small>近 1 小时</small></UiCard>
      </div>

      <UiCard className="workbench-pane">
        <div className="proto-card-head">
          <div>
            <h3>检索实验评测</h3>
            <p className="subtle-note">用于观察运行检索实验侧车在 shadow mode 下的召回质量、误放行风险与回退动作。</p>
          </div>
          <UiBadge tone="good">Shadow Mode 已开启</UiBadge>
        </div>
        <div className="workbench-metric-strip">
          {RETRIEVAL_EXPERIMENT_METRICS.map((item) => (
            <UiCard key={item.label} className="workbench-metric-card">
              <span>{item.label}</span>
              <strong>{item.value}</strong>
              <small>{item.detail}</small>
            </UiCard>
          ))}
        </div>
        <p className="subtle-note">停灰度 -&gt; 停影子模式 -&gt; 禁用适配器</p>
      </UiCard>

      <UiCard className="workbench-pane">
        <div className="proto-card-head">
          <div>
            <h3>实验索引版本</h3>
            <p className="subtle-note">已发布快照与实验检索索引版本保持一一锁定；错配时只允许降级，不允许跨快照混读。</p>
          </div>
          <UiBadge tone="neutral">{INDEX_SYNC_STATUS.manifestStatus}</UiBadge>
        </div>
        <div className="workbench-metric-strip">
          <UiCard className="workbench-metric-card">
            <span>index version</span>
            <strong>{INDEX_SYNC_STATUS.indexVersion}</strong>
            <small>已锁定快照 {INDEX_SYNC_STATUS.lockedSnapshot}</small>
          </UiCard>
          <UiCard className="workbench-metric-card">
            <span>snapshot mismatch</span>
            <strong>{INDEX_SYNC_STATUS.mismatchAlert}</strong>
            <small>当前窗口未发现错配</small>
          </UiCard>
        </div>
      </UiCard>

      <div className="workbench-grid">
        <UiCard className="workbench-pane">
          <div className="proto-card-head">
            <div>
              <h3>告警列表</h3>
              <p className="subtle-note">误路由、高敏暴露风险、元数据漂移和发布阻断都前置到同一列表里。</p>
            </div>
            <AlertTriangle size={18} strokeWidth={1.9} />
          </div>
          <div className="workbench-list">
            {ALERTS.map((item) => (
              <article key={item.title} className="workbench-list-row">
                <div>
                  <strong>{item.title}</strong>
                  <p>{item.detail}</p>
                </div>
                <div className="workbench-row-side">
                  <UiBadge tone={item.tone}>{item.tone === "good" ? "已恢复" : "处理中"}</UiBadge>
                </div>
              </article>
            ))}
          </div>
        </UiCard>

        <UiCard className="workbench-pane">
          <div className="proto-card-head">
            <div>
              <h3>审计检索</h3>
              <p className="subtle-note">支持按追踪编号、审批任务、版本编号和操作人回查。</p>
            </div>
            <Activity size={18} strokeWidth={1.9} />
          </div>
          <div className="proto-inline-fields">
            <UiInput value="trace_runtime_20260327_07" readOnly aria-label="追踪编号" hint="当前示例为最近一次运行验证链路" />
            <UiInput value="APR-20260327-001" readOnly aria-label="审批任务编号" hint="审批与运行链路已关联" />
          </div>
        </UiCard>
      </div>

      <UiCard className="workbench-pane">
        <div className="proto-card-head">
          <div>
            <h3>链路回放</h3>
            <p className="subtle-note">从一次请求到一次审批，按时间轴回看当时为什么这样判断。</p>
          </div>
          <History size={18} strokeWidth={1.9} />
        </div>
        <div className="workbench-timeline">
          {TRACE_TIMELINE.map((item) => <p key={item}>{item}</p>)}
        </div>
      </UiCard>
    </section>
  );
}
