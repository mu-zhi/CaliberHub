import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, ShieldCheck } from "lucide-react";
import { UiBadge, UiButton, UiCard, UiInput, UiSegmentedControl, UiTextarea } from "../../components/ui";
import {
  PrototypeGraphCanvas,
  PrototypeKvList,
  PrototypeMetricStrip,
  PrototypeWorkbenchShell,
} from "../../components/prototype/PrototypeWorkbenchShell";

const STEPS = [
  { no: 1, label: "材料接入", summary: "已完成", status: "done", statusLabel: "已完成" },
  { no: 2, label: "实体抽取", summary: "已完成候选关系确认", status: "done", statusLabel: "已完成" },
  { no: 3, label: "资产建模", summary: "正在把图谱事实落成治理资产", status: "active", statusLabel: "建模中" },
  { no: 4, label: "运行验证", summary: "等待资产保存后触发", status: "pending", statusLabel: "待进入" },
  { no: 5, label: "发布检查", summary: "等待运行验证完成", status: "pending", statusLabel: "待进入" },
];

const METRICS = [
  { label: "候选对象", value: "7", hint: "全部进入建模池" },
  { label: "需补齐字段", value: "4", hint: "时间、新鲜度、审批模板、回退动作" },
  { label: "覆盖分段", value: "2", hint: "完整覆盖 + 历史补查" },
  { label: "安全规则", value: "3", hint: "允许、需审批、禁止返回" },
];

const GRAPH_NODES = [
  { id: "scene", label: "代发明细查询", hint: "Scene", x: 18, y: 20, tone: "scene" },
  { id: "plan", label: "代发明细方案", hint: "Plan", x: 44, y: 20, tone: "plan" },
  { id: "coverage", label: "覆盖声明", hint: "Coverage", x: 70, y: 20, tone: "coverage" },
  { id: "policy", label: "策略对象", hint: "Policy", x: 82, y: 48, tone: "policy" },
  { id: "output", label: "输出契约", hint: "Output Contract", x: 42, y: 54, tone: "contract" },
  { id: "view", label: "契约视图", hint: "Contract View", x: 20, y: 58, tone: "view" },
  { id: "time", label: "时间语义", hint: "Selector", x: 64, y: 74, tone: "time" },
];

const GRAPH_EDGES = [
  { from: "scene", to: "plan", label: "运行入口", emphasis: true },
  { from: "plan", to: "coverage", label: "覆盖绑定", emphasis: true },
  { from: "plan", to: "output", label: "输出承诺" },
  { from: "output", to: "view", label: "字段裁剪" },
  { from: "coverage", to: "policy", label: "门禁判断" },
  { from: "plan", to: "time", label: "时间选择" },
];

const LEGEND = [
  { label: "业务场景", tone: "scene" },
  { label: "方案资产", tone: "plan" },
  { label: "覆盖声明", tone: "coverage" },
  { label: "策略对象", tone: "policy" },
  { label: "输出与视图", tone: "contract" },
];

const ASSET_OPTIONS = [
  { value: "scene", label: "业务场景" },
  { value: "plan", label: "方案资产" },
  { value: "coverage", label: "覆盖声明" },
  { value: "policy", label: "策略对象" },
  { value: "output", label: "输出契约" },
  { value: "view", label: "契约视图" },
  { value: "time", label: "时间语义" },
];

const ASSET_CONFIG = {
  scene: {
    title: "业务场景",
    tone: "good",
    description: "把“代发明细查询”作为正式消费入口，先定义边界，再决定是否可发布。",
    fields: [
      { label: "场景名称", value: "代发明细查询" },
      { label: "场景编码", value: "SCN_PAYROLL_DETAIL" },
      { label: "主对象", value: "代发协议号" },
      { label: "默认时间", value: "交易日期" },
    ],
    body: "场景范围覆盖代发协议明细查询，排除未落位历史表字段。历史补查只在时间早于 2014-01-01 且覆盖声明允许时启用。",
    tags: ["运行入口", "需复核", "可进入下一步"],
  },
  plan: {
    title: "方案资产",
    tone: "good",
    description: "方案资产负责把输入槽位、来源契约、回退动作和路径模板绑在一起。",
    fields: [
      { label: "方案编码", value: "PLAN_PAYROLL_DETAIL_V1" },
      { label: "路径模板", value: "公司户口号 -> 协议号 -> 主表 / 历史表" },
      { label: "回退动作", value: "缺主表字段时拒绝自动回退" },
      { label: "输入要求", value: "公司户口号、时间范围、用途" },
    ],
    body: "当前方案将主表查询设为默认路径，历史表只在覆盖声明命中且人工确认后的条件下启用，不允许在运行时自由跳表。",
    tags: ["路径模板", "来源受控", "可运行"],
  },
  coverage: {
    title: "覆盖声明",
    tone: "warn",
    description: "明确什么时候完整覆盖，什么时候只部分覆盖，以及缺口出现时的回退动作。",
    fields: [
      { label: "覆盖等级", value: "部分覆盖" },
      { label: "完整覆盖", value: "2014-01-01 至今" },
      { label: "补查覆盖", value: "2014-01-01 之前" },
      { label: "缺口动作", value: "提示缩小时间范围或发起审批" },
    ],
    body: "当前历史表字段说明不完整，因此历史补查段默认标记为部分覆盖，需要在发布前完成缺口清零或明确审批回退动作。",
    tags: ["部分覆盖", "需补缺", "发布阻断候选"],
  },
  policy: {
    title: "策略对象",
    tone: "warn",
    description: "把允许、审批、拒绝三类决策显式写成对象，而不是把规则埋进后端。",
    fields: [
      { label: "默认决策", value: "允许返回" },
      { label: "审批条件", value: "跨机构导出或请求高敏字段" },
      { label: "拒绝条件", value: "命中禁止返回字段或用途不明" },
      { label: "审计要求", value: "保留 trace_id 与审批单号" },
    ],
    body: "策略对象必须和字段级安全一起展示，避免用户只看到结果却看不到为什么需要审批或为什么被拒绝。",
    tags: ["字段安全", "审批触发", "审计必留"],
  },
  output: {
    title: "输出契约",
    tone: "good",
    description: "系统真正承诺对外返回什么，不返回什么，都在这里定死。",
    fields: [
      { label: "必返字段", value: "协议号、交易日期、金额" },
      { label: "可选字段", value: "收款账号、开户机构" },
      { label: "禁止字段", value: "密码修改日志、身份证件号" },
      { label: "默认脱敏", value: "收款账号后四位" },
    ],
    body: "运行态最终返回的是知识包，不是裸字段清单。输出契约决定知识包里哪些字段可以出现、如何出现。",
    tags: ["知识包输出", "字段边界", "不返裸结果"],
  },
  view: {
    title: "契约视图",
    tone: "warn",
    description: "同一份输出契约会按角色、用途和审批上下文裁成不同视图。",
    fields: [
      { label: "默认角色", value: "运行支持 / 数据治理" },
      { label: "审批后扩展", value: "可查看收款账号全值" },
      { label: "脱敏策略", value: "账号后四位 + 机构名保留" },
      { label: "替代动作", value: "无权限时显示脱敏导出方案" },
    ],
    body: "契约视图必须让字段级安全在前台可感知，用户要能看见当前角色可见字段、需审批字段和禁止返回字段。",
    tags: ["按角色裁剪", "字段级安全", "前台可感知"],
  },
  time: {
    title: "时间语义",
    tone: "neutral",
    description: "统一规定默认时间字段、时点优先级和回退顺序，避免运行时歧义。",
    fields: [
      { label: "默认时间", value: "交易日期" },
      { label: "时点类型", value: "区间查询" },
      { label: "历史分段阈值", value: "2014-01-01" },
      { label: "缺省规则", value: "未传时间时默认近 90 天" },
    ],
    body: "时间语义不是展示项，而是硬门禁对象。它会直接影响 Coverage（覆盖声明） 命中、路径选择和运行结果解释。",
    tags: ["时间门禁", "路径选择", "覆盖前置"],
  },
};

const CONSOLE_TABS = [
  {
    value: "business",
    label: "建模输出",
    title: "双栏建模输出",
    description: "把当前最关键的对象缺口和发布风险前置给建模人。",
    logs: [
      { at: "10:14:11", level: "info", message: "Scene（业务场景） 已继承候选标题与主对象" },
      { at: "10:14:18", level: "success", message: "Plan（方案资产） 已绑定主表路径与回退动作" },
      { at: "10:14:24", level: "warn", message: "Coverage（覆盖声明） 仍存在 1 个历史补查缺口" },
      { at: "10:14:33", level: "warn", message: "Contract View（契约视图） 尚未绑定审批模板" },
    ],
  },
  {
    value: "trace",
    label: "规则追踪",
    title: "规则写入与对象追踪",
    description: "保留当前原型想表达的结构化治理感，而不是回到 JSON 编辑。",
    logs: [
      {
        at: "10:14:16",
        level: "success",
        message: "graph_fact -> governed_asset 已建立映射",
        detail: "{\n  \"scene_id\": \"SCN_PAYROLL_DETAIL\",\n  \"plan_id\": \"PLAN_PAYROLL_DETAIL_V1\",\n  \"source\": \"trace_ingest_20260327_01\"\n}",
      },
      {
        at: "10:14:25",
        level: "warn",
        message: "coverage_declaration 存在未清零缺口",
        detail: "{\n  \"gap_segment\": \"2014-01-01 之前历史补查\",\n  \"fallback\": \"需要审批或缩小时间范围\"\n}",
      },
      {
        at: "10:14:34",
        level: "info",
        message: "contract_view 待绑定审批模板",
        detail: "{\n  \"view\": \"VIEW_PAYROLL_DETAIL_SUPPORT\",\n  \"approval_template\": \"需审批\"\n}",
      },
    ],
  },
];

function toneByAsset(assetKey) {
  if (assetKey === "coverage" || assetKey === "policy" || assetKey === "view") {
    return "warn";
  }
  if (assetKey === "time") {
    return "neutral";
  }
  return "good";
}

export function PrototypeModelingDualPanePage({
  nextPath = "/prototype/runtime-publish",
  nextLabel = "进入运行验证",
}) {
  const [activeAsset, setActiveAsset] = useState("plan");
  const config = useMemo(() => ASSET_CONFIG[activeAsset] || ASSET_CONFIG.plan, [activeAsset]);

  return (
    <PrototypeWorkbenchShell
      kicker="知识生产台 / Step 3 / 双栏模式"
      title="把图谱事实转成正式治理资产"
      description="左侧继续保留图谱和覆盖结构，右侧集中完成治理对象与规则对象的编辑，不回到跳页式后台表单。"
      tags={[
        { label: "双栏建模", tone: "good" },
        { label: "发布阻断 2 项", tone: "warn" },
      ]}
      steps={STEPS}
      consoleTabs={CONSOLE_TABS}
      main={(
        <>
          <PrototypeMetricStrip items={METRICS} />

          <PrototypeGraphCanvas
            title="治理对象关系图"
            description="图谱继续保留在左侧，用来说明当前建模对象之间的依赖与门禁，不让用户失去上下文。"
            nodes={GRAPH_NODES}
            edges={GRAPH_EDGES}
            legend={LEGEND}
            selectedId={activeAsset}
            onSelect={setActiveAsset}
          />

          <div className="proto-grid-two">
            <UiCard className="proto-side-card" elevation="card">
              <div className="proto-card-head">
                <div>
                  <h3>覆盖结构</h3>
                  <p className="subtle-note">图谱之外，还要让覆盖边界和缺口位置能被结构化阅读。</p>
                </div>
              </div>
              <div className="proto-coverage-stack">
                <div className="proto-coverage-row tone-good">
                  <strong>2014-01-01 至今</strong>
                  <span>完整覆盖 · 主表明细</span>
                </div>
                <div className="proto-coverage-row tone-warn">
                  <strong>2014-01-01 之前</strong>
                  <span>部分覆盖 · 历史补查需审批或补缺</span>
                </div>
              </div>
            </UiCard>

            <UiCard className="proto-side-card" elevation="card">
              <div className="proto-card-head">
                <div>
                  <h3>字段级安全结果</h3>
                  <p className="subtle-note">字段边界不放到最后一步，建模时就让它可见。</p>
                </div>
                <ShieldCheck size={18} strokeWidth={1.9} />
              </div>
              <div className="proto-chip-row">
                <span className="proto-soft-chip tone-good">普通可见：协议号、交易日期、金额</span>
                <span className="proto-soft-chip tone-neutral">默认脱敏：收款账号后四位</span>
                <span className="proto-soft-chip tone-warn">需审批：全量收款账号</span>
                <span className="proto-soft-chip tone-bad">禁止返回：密码修改日志</span>
              </div>
            </UiCard>
          </div>
        </>
      )}
      side={(
        <>
          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>当前编辑对象</h3>
                <p className="subtle-note">通过对象切换保持右侧编辑聚焦，不把所有治理对象同时铺满。</p>
              </div>
              <UiBadge tone={toneByAsset(activeAsset)}>{config.title}</UiBadge>
            </div>

            <UiSegmentedControl
              ariaLabel="治理对象切换"
              className="proto-object-segment"
              value={activeAsset}
              onChange={setActiveAsset}
              options={ASSET_OPTIONS}
            />

            <p className="proto-side-summary">{config.description}</p>
            <PrototypeKvList items={config.fields} />

            <UiTextarea
              value={config.body}
              readOnly
              aria-label={`${config.title} 说明`}
              hint="原型阶段先通过结构化只读字段表达对象编辑区，不回退到 JSON 主编辑。"
            />

            <div className="proto-chip-row">
              {config.tags.map((tag) => <span key={tag} className="proto-soft-chip tone-neutral">{tag}</span>)}
            </div>

            <div className="proto-inline-fields">
              <UiInput value={`当前对象：${config.title}`} readOnly aria-label="当前对象" />
              <UiInput value="版本边界：草稿 v0.3 / 已发布 v0.2" readOnly aria-label="版本边界" />
            </div>

            <div className="proto-action-row">
              <UiButton>保存草稿</UiButton>
              <UiButton variant="secondary">发起复核</UiButton>
            </div>
          </UiCard>

          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>当前阻断项</h3>
                <p className="subtle-note">阻断和缺口必须在建模阶段前置，而不是等发布时才告诉用户。</p>
              </div>
            </div>
            <ul className="proto-bullet-list">
              <li>历史补查段的字段落位还缺少“开户机构”。</li>
              <li>审批后扩展视图尚未绑定审批模板。</li>
              <li>时间语义的默认近 90 天规则需业务复核。</li>
            </ul>
          </UiCard>

          <UiCard className="proto-side-card proto-next-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>下一步</h3>
                <p className="subtle-note">对象落成后，进入 Step 4-5 的运行验证与发布检查页。</p>
              </div>
            </div>
            <p className="proto-side-summary">下一张原型会把 Knowledge Package（知识包） 验证和候选版本发布检查串到同一工作台骨架里。</p>
            <UiButton as={Link} to={nextPath} variant="secondary" icon={<ArrowRight size={16} />}>
              {nextLabel}
            </UiButton>
          </UiCard>
        </>
      )}
    />
  );
}
