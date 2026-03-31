import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, CheckCircle2, FileSearch, PackageCheck } from "lucide-react";
import { UiBadge, UiButton, UiCard, UiInput, UiSegmentedControl, UiTextarea } from "../../components/ui";
import {
  PrototypeKvList,
  PrototypeMetricStrip,
  PrototypeWorkbenchShell,
} from "../../components/prototype/PrototypeWorkbenchShell";

const RUNTIME_STEPS = [
  { no: 1, label: "材料接入", summary: "已完成", status: "done", statusLabel: "已完成" },
  { no: 2, label: "实体抽取", summary: "已完成", status: "done", statusLabel: "已完成" },
  { no: 3, label: "资产建模", summary: "已完成", status: "done", statusLabel: "已完成" },
  { no: 4, label: "运行验证", summary: "正在解释一次真实请求的命中路径", status: "active", statusLabel: "验证中" },
  { no: 5, label: "发布检查", summary: "等待运行验证完成后执行", status: "pending", statusLabel: "待进入" },
];

const PUBLISH_STEPS = [
  { no: 1, label: "材料接入", summary: "已完成", status: "done", statusLabel: "已完成" },
  { no: 2, label: "实体抽取", summary: "已完成", status: "done", statusLabel: "已完成" },
  { no: 3, label: "资产建模", summary: "已完成", status: "done", statusLabel: "已完成" },
  { no: 4, label: "运行验证", summary: "已通过样板问题验证", status: "done", statusLabel: "已完成" },
  { no: 5, label: "发布检查", summary: "正在检查契约、覆盖、策略和回滚边界", status: "active", statusLabel: "检查中" },
];

const RUNTIME_METRICS = [
  { label: "命中场景", value: "1", hint: "代发明细查询" },
  { label: "命中方案", value: "1", hint: "主表优先，历史补查受控" },
  { label: "覆盖结果", value: "部分覆盖", hint: "2014 年之前需审批" },
  { label: "决策结果", value: "需要审批", hint: "全量账号查看需审批" },
];

const PUBLISH_METRICS = [
  { label: "候选版本", value: "v0.3", hint: "待替换当前稳定版本 v0.2" },
  { label: "变更对象", value: "6", hint: "含 1 个新增覆盖声明" },
  { label: "阻断项", value: "2", hint: "审批模板 / 历史字段缺口" },
  { label: "回滚点", value: "1", hint: "可回退到稳定快照 v0.2" },
];

const RUNTIME_PROCESS = [
  { no: "01", title: "查询改写", summary: "把“查 2012 年代发明细”改写成结构化槽位请求。", tone: "done", tags: ["用途=运行支持", "是否导出=否"] },
  { no: "02", title: "槽位补齐", summary: "自动补齐角色、时间范围、请求字段和用途说明。", tone: "done", tags: ["角色=support", "时间=2012-01-01..2012-12-31"] },
  { no: "03", title: "场景召回", summary: "命中 Scene：代发明细查询。", tone: "done", tags: ["场景=SCN_PAYROLL_DETAIL"] },
  { no: "04", title: "方案选择", summary: "命中主方案：主表优先，历史补查受控。", tone: "done", tags: ["方案=PLAN_PAYROLL_DETAIL_V1"] },
  { no: "05", title: "覆盖判定", summary: "2012 年请求进入历史补查段，命中部分覆盖。", tone: "warn", tags: ["覆盖=PARTIAL", "缺口=开户机构"] },
  { no: "06", title: "策略决策", summary: "请求包含全量收款账号，触发审批。", tone: "warn", tags: ["决策=需审批", "policy=POLICY_PAYROLL_EXPORT"] },
  { no: "07", title: "路径解析", summary: "路径缩域为“代发协议号 -> 历史明细表”，未开放自由跳表。", tone: "done", tags: ["路径模板生效"] },
  { no: "08", title: "知识包输出", summary: "返回 Knowledge Package（知识包） 摘要，而不是裸结果。", tone: "active", tags: ["trace_id=trace_runtime_20260327_07"] },
];

const PUBLISH_CHECKS = [
  { label: "契约可兑现", status: "good", detail: "必返字段已全部落位" },
  { label: "契约视图完整", status: "warn", detail: "审批后扩展视图尚未绑定审批模板" },
  { label: "覆盖声明有效", status: "warn", detail: "历史补查段仍存在字段缺口" },
  { label: "策略对象可执行", status: "good", detail: "允许 / 审批 / 拒绝规则均已定义" },
  { label: "证据可追溯", status: "good", detail: "证据片段与 trace_id 已绑定" },
  { label: "元数据可落位", status: "good", detail: "主表字段全部对齐" },
  { label: "缓存与索引刷新", status: "neutral", detail: "等待候选版本切换后执行" },
  { label: "整体回滚可执行", status: "good", detail: "可一键回退到稳定版本 v0.2" },
];

const RUNTIME_CONSOLE = [
  {
    value: "business",
    label: "运行输出",
    title: "运行链路输出",
    description: "保留业务上最重要的命中、部分覆盖和审批原因。",
    logs: [
      { at: "11:02:11", level: "info", message: "已接收结构化请求：代发明细查询 / 2012-01-01 至 2012-12-31" },
      { at: "11:02:15", level: "success", message: "Scene（业务场景） 命中：SCN_PAYROLL_DETAIL" },
      { at: "11:02:19", level: "warn", message: "Coverage（覆盖声明） 命中部分覆盖，历史补查段仍有字段缺口" },
      { at: "11:02:23", level: "warn", message: "Policy（策略对象） 判定为需要审批：全量收款账号属于需审批字段" },
    ],
  },
  {
    value: "trace",
    label: "追踪摘要",
    title: "Trace 与原因码",
    description: "运行态最终要把为什么命中、为什么拒绝、为什么审批讲清楚。",
    logs: [
      {
        at: "11:02:23",
        level: "warn",
        message: "runtime_trace 已写入",
        detail: "{\n  \"trace_id\": \"trace_runtime_20260327_07\",\n  \"reason_code\": [\"PARTIAL_COVERAGE\", \"APPROVAL_REQUIRED\"]\n}",
      },
      {
        at: "11:02:24",
        level: "success",
        message: "knowledge_package 已生成解释结果",
        detail: "{\n  \"scene\": \"SCN_PAYROLL_DETAIL\",\n  \"decision\": \"需审批\",\n  \"masked_fields\": [\"收款账号\"]\n}",
      },
    ],
  },
];

const PUBLISH_CONSOLE = [
  {
    value: "business",
    label: "检查输出",
    title: "发布检查输出",
    description: "只把真正影响能不能切换版本的信息放到主控制台里。",
    logs: [
      { at: "11:18:03", level: "success", message: "候选版本 v0.3 已完成样板运行验证" },
      { at: "11:18:11", level: "warn", message: "Contract View（契约视图） 缺少审批模板绑定，暂不能整体放行" },
      { at: "11:18:19", level: "warn", message: "历史补查段仍存在字段缺口，覆盖声明未清零" },
      { at: "11:18:27", level: "info", message: "整体回滚点已预生成：snapshot_v0_2" },
    ],
  },
  {
    value: "trace",
    label: "发布追踪",
    title: "发布与回滚追踪",
    description: "发布不是按钮，是一组可回溯、可回滚的状态机。",
    logs: [
      {
        at: "11:18:12",
        level: "warn",
        message: "publish_check 存在阻断项",
        detail: "{\n  \"candidate_version\": \"v0.3\",\n  \"blocked_checks\": [\"contract_view_approval_template\", \"history_field_gap\"]\n}",
      },
      {
        at: "11:18:29",
        level: "success",
        message: "rollback_plan 已生成",
        detail: "{\n  \"rollback_snapshot\": \"snapshot_v0_2\",\n  \"switch_mode\": \"atomic\"\n}",
      },
    ],
  },
];

function checkToneClass(status) {
  if (status === "good") {
    return "tone-good";
  }
  if (status === "warn") {
    return "tone-warn";
  }
  if (status === "bad") {
    return "tone-bad";
  }
  return "tone-neutral";
}

function processToneClass(tone) {
  if (tone === "done") {
    return "tone-good";
  }
  if (tone === "warn") {
    return "tone-warn";
  }
  if (tone === "active") {
    return "tone-neutral";
  }
  return "tone-neutral";
}

export function PrototypeRuntimePublishPage({
  initialSurface = "runtime",
  allowSurfaceSwitch = true,
}) {
  const [surface, setSurface] = useState(initialSurface);

  useEffect(() => {
    setSurface(initialSurface);
  }, [initialSurface]);

  const runtimeShellProps = useMemo(() => {
    if (surface === "publish") {
      return {
        kicker: "发布中心 / Step 5 / 工作台模式",
        title: "检查候选版本，再决定是否整体切换",
        description: "同一张原型页里承接运行验证后的发布检查，把候选版本、阻断项和回滚边界放到同一工作台里。",
        tags: [
          { label: "发布检查", tone: "good" },
          { label: "阻断项 2 个", tone: "warn" },
        ],
        steps: PUBLISH_STEPS,
        consoleTabs: PUBLISH_CONSOLE,
      };
    }
    return {
        kicker: "运行决策台 / Step 4 / 工作台模式",
        title: "运行样板问题，验证知识包输出",
        description: "左侧先展示 Knowledge Package（知识包） 解释结果，右侧再把 8 步运行链路讲清楚，不做黑盒返回。",
        tags: [
          { label: "运行验证", tone: "good" },
          { label: "需要审批", tone: "warn" },
        ],
      steps: RUNTIME_STEPS,
      consoleTabs: RUNTIME_CONSOLE,
    };
  }, [surface]);

  return (
    <PrototypeWorkbenchShell
      key={surface}
      {...runtimeShellProps}
      toolbar={allowSurfaceSwitch ? (
        <div className="proto-toolbar-inner">
          <UiSegmentedControl
            ariaLabel="运行验证与发布检查切换"
            value={surface}
            onChange={setSurface}
            options={[
              { value: "runtime", label: "Step 4 运行验证" },
              { value: "publish", label: "Step 5 发布检查" },
            ]}
          />
        </div>
      ) : null}
      main={surface === "runtime" ? (
        <>
          <PrototypeMetricStrip items={RUNTIME_METRICS} />

          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>样板问题与槽位</h3>
                <p className="subtle-note">运行态先表达请求本身，再解释系统为什么这样判断。</p>
              </div>
              <UiBadge tone="warn">需要审批</UiBadge>
            </div>
            <div className="proto-inline-fields">
              <UiInput value="查 2012 年代发明细，并查看全量收款账号" readOnly aria-label="样板问题" />
              <UiInput value="角色=运行支持 / 用途=工单核验 / 是否导出=否" readOnly aria-label="运行槽位" />
            </div>
            <UiTextarea
              value={"系统已将请求改写为结构化槽位：\n- Scene：代发明细查询\n- 时间范围：2012-01-01 ~ 2012-12-31\n- 请求字段：协议号、交易日期、金额、收款账号\n- 风险点：请求全量收款账号"}
              readOnly
              aria-label="运行槽位说明"
            />
          </UiCard>

          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>Knowledge Package（知识包） 结果</h3>
                <p className="subtle-note">左侧展示知识包而不是裸字段或 SQL 结果。</p>
              </div>
              <PackageCheck size={18} strokeWidth={1.9} />
            </div>
            <PrototypeKvList
              items={[
                { label: "业务场景", value: "代发明细查询" },
                { label: "命中方案", value: "主表优先，历史补查受控" },
                { label: "覆盖结果", value: "部分覆盖（历史段需审批或缩小范围）" },
                { label: "决策结果", value: "需要审批" },
                { label: "trace_id", value: "trace_runtime_20260327_07" },
              ]}
            />
            <div className="proto-chip-row">
              <span className="proto-soft-chip tone-good">可见字段：协议号、交易日期、金额</span>
              <span className="proto-soft-chip tone-neutral">默认脱敏：收款账号后四位</span>
              <span className="proto-soft-chip tone-warn">需审批：全量收款账号</span>
              <span className="proto-soft-chip tone-bad">禁止返回：密码修改日志</span>
            </div>
            <UiTextarea
              value={"证据摘要：\n1. Coverage（覆盖声明） 显示 2014 年之前仅部分覆盖。\n2. Policy（策略对象） 规定全量收款账号需要审批。\n3. Path Resolution（路径解析） 只允许命中历史补查模板，不开放自由跳表。"}
              readOnly
              aria-label="证据摘要"
            />
          </UiCard>
        </>
      ) : (
        <>
          <PrototypeMetricStrip items={PUBLISH_METRICS} />

          <div className="proto-grid-two">
            <UiCard className="proto-side-card" elevation="card">
              <div className="proto-card-head">
                <div>
                  <h3>候选版本差异</h3>
                  <p className="subtle-note">发布中心不是单一按钮，而是版本差异、检查矩阵和回滚边界的集合。</p>
                </div>
                <FileSearch size={18} strokeWidth={1.9} />
              </div>
              <ul className="proto-bullet-list">
                <li>新增 1 个 Coverage（覆盖声明） 分段：历史补查审批回退。</li>
                <li>修改 1 个 Output Contract（输出契约）：收款账号默认脱敏规则。</li>
                <li>新增 1 个 Contract View（契约视图）：审批后扩展视图。</li>
                <li>更新 1 个 Policy（策略对象）：导出用途新增审计要求。</li>
              </ul>
            </UiCard>

            <UiCard className="proto-side-card" elevation="card">
              <div className="proto-card-head">
                <div>
                  <h3>回滚预案</h3>
                  <p className="subtle-note">整体切换和整体回滚都要在前台被明确表达。</p>
                </div>
                <AlertTriangle size={18} strokeWidth={1.9} />
              </div>
              <PrototypeKvList
                items={[
                  { label: "当前稳定版本", value: "v0.2" },
                  { label: "候选版本", value: "v0.3" },
                  { label: "回滚快照", value: "snapshot_v0_2" },
                  { label: "切换方式", value: "整体切换 / 整体回滚" },
                ]}
              />
            </UiCard>
          </div>
        </>
      )}
      side={surface === "runtime" ? (
        <>
          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>8 步运行链路</h3>
                <p className="subtle-note">右侧过程区最终落点固定为知识包解释结果，不做“报告生成器”。</p>
              </div>
              <UiBadge tone="warn">需要审批</UiBadge>
            </div>
            <div className="proto-process-list">
              {RUNTIME_PROCESS.map((item) => (
                <article key={item.no} className={`proto-process-card ${processToneClass(item.tone)}`}>
                  <header>
                    <span className="proto-process-no">{item.no}</span>
                    <div>
                      <h4>{item.title}</h4>
                      <p>{item.summary}</p>
                    </div>
                  </header>
                  <div className="proto-chip-row">
                    {item.tags.map((tag) => <span key={tag} className="proto-soft-chip tone-neutral">{tag}</span>)}
                  </div>
                </article>
              ))}
            </div>
          </UiCard>

          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>当前动作</h3>
                <p className="subtle-note">运行决策台不隐藏动作边界，让用户明确知道下一步是什么。</p>
              </div>
            </div>
            <div className="proto-action-row">
              <UiButton>提交审批</UiButton>
              <UiButton variant="secondary">复制追踪编号</UiButton>
            </div>
          </UiCard>
        </>
      ) : (
        <>
          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>发布检查矩阵</h3>
                <p className="subtle-note">候选版本能不能切换，要靠矩阵化检查，而不是靠经验判断。</p>
              </div>
              <UiBadge tone="warn">已阻断</UiBadge>
            </div>
            <div className="proto-check-list">
              {PUBLISH_CHECKS.map((item) => (
                <article key={item.label} className={`proto-check-row ${checkToneClass(item.status)}`}>
                  <div className="proto-check-row-head">
                    <strong>{item.label}</strong>
                    {item.status === "good" ? <CheckCircle2 size={16} /> : item.status === "warn" ? <AlertTriangle size={16} /> : <span>等待</span>}
                  </div>
                  <p>{item.detail}</p>
                </article>
              ))}
            </div>
          </UiCard>

          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>影响分析与操作</h3>
                <p className="subtle-note">发布影响和阻断修复要和主操作同屏，不把用户扔到别的页面找答案。</p>
              </div>
            </div>
            <ul className="proto-bullet-list">
              <li>影响 1 个运行场景、1 个审批模板、1 个契约视图。</li>
              <li>阻断修复建议：先绑定审批模板，再补齐历史字段缺口。</li>
            </ul>
            <div className="proto-action-row">
              <UiButton variant="secondary">保存草稿</UiButton>
              <UiButton variant="secondary">执行回滚预演</UiButton>
              <UiButton>发起复核</UiButton>
            </div>
          </UiCard>
        </>
      )}
    />
  );
}
