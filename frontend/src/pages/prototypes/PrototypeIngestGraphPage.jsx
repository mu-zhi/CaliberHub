import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, CheckCircle2, FileStack, UploadCloud } from "lucide-react";
import { UiBadge, UiButton, UiCard, UiInput, UiTextarea } from "../../components/ui";
import {
  PrototypeGraphCanvas,
  PrototypeKvList,
  PrototypeMetricStrip,
  PrototypeWorkbenchShell,
} from "../../components/prototype/PrototypeWorkbenchShell";

const STEPS = [
  { no: 1, label: "材料接入", summary: "来源接入契约已登记 8 / 8 项关键字段", status: "done", statusLabel: "已就绪" },
  { no: 2, label: "实体抽取", summary: "候选实体 46 个，待确认关系 7 条", status: "active", statusLabel: "处理中" },
  { no: 3, label: "资产建模", summary: "等待人工确认后进入建模", status: "pending", statusLabel: "待进入" },
  { no: 4, label: "运行验证", summary: "暂未触发", status: "pending", statusLabel: "未开始" },
  { no: 5, label: "发布检查", summary: "暂未触发", status: "pending", statusLabel: "未开始" },
];

const METRICS = [
  { label: "材料批次", value: "8", hint: "已归档 6，当前处理中 2" },
  { label: "候选实体", value: "46", hint: "含 5 个待归一实体" },
  { label: "关系边", value: "31", hint: "7 条需要人工确认" },
  { label: "证据锚点", value: "19", hint: "命中原文行号与截图片段" },
];

const NODES = [
  { id: "doc", label: "代发历史口径文档", hint: "来源材料", x: 14, y: 18, tone: "document" },
  { id: "scene", label: "代发明细查询", hint: "候选业务场景", x: 46, y: 16, tone: "scene" },
  { id: "corp", label: "公司户口号", hint: "输入槽位", x: 19, y: 54, tone: "field" },
  { id: "agreement", label: "代发协议号", hint: "核心标识", x: 42, y: 48, tone: "field" },
  { id: "main", label: "主表明细", hint: "来源表", x: 70, y: 42, tone: "source" },
  { id: "history", label: "历史明细表", hint: "补历史表", x: 78, y: 70, tone: "source" },
  { id: "evidence", label: "证据片段", hint: "原文锚点", x: 44, y: 78, tone: "evidence" },
];

const EDGES = [
  { from: "doc", to: "scene", label: "标题语义", emphasis: true },
  { from: "doc", to: "corp", label: "输入字段" },
  { from: "doc", to: "agreement", label: "输出字段", emphasis: true },
  { from: "agreement", to: "main", label: "主表路由" },
  { from: "agreement", to: "history", label: "历史补查", dashed: true },
  { from: "doc", to: "evidence", label: "证据挂载" },
  { from: "scene", to: "agreement", label: "场景归一" },
];

const LEGEND = [
  { label: "来源材料", tone: "document" },
  { label: "候选场景", tone: "scene" },
  { label: "字段与标识", tone: "field" },
  { label: "来源表", tone: "source" },
  { label: "证据片段", tone: "evidence" },
];

const DETAIL_BY_NODE = {
  doc: {
    title: "代发历史口径文档",
    badge: "来源材料",
    confidence: "92%",
    summary: "系统已从原文中识别出 Step 结构、历史表时段说明和输出字段线索，当前需要确认“历史明细表”是否属于同一业务场景。",
    kv: [
      { label: "材料类型", value: "口径文档 + SQL 样例" },
      { label: "解析状态", value: "已抽取候选实体与关系" },
      { label: "风险提醒", value: "历史分段描述存在 OCR 噪声" },
    ],
    tags: ["来源接入契约", "原文锚点", "待补充主对象"],
  },
  scene: {
    title: "代发明细查询",
    badge: "候选业务场景",
    confidence: "88%",
    summary: "标题、Step 注释和输出字段都指向“代发明细查询”，但系统还需要确认该场景是否拆分为“主表查询”和“历史补查”两个子场景。",
    kv: [
      { label: "候选标题", value: "代发明细查询" },
      { label: "适用范围", value: "代发协议明细 / 历史补查" },
      { label: "下一步", value: "进入元数据对齐前完成场景拆分确认" },
    ],
    tags: ["候选场景", "待确认拆分", "可进入建模"],
  },
  corp: {
    title: "公司户口号",
    badge: "输入槽位",
    confidence: "95%",
    summary: "已识别为主要输入槽位，可映射为场景的必填输入，并作为路由前置条件之一。",
    kv: [
      { label: "槽位类型", value: "主对象标识" },
      { label: "命中位置", value: "Step 1 注释、SQL where 条件" },
      { label: "建议动作", value: "接受为 Scene 输入字段" },
    ],
    tags: ["输入字段", "建议接受", "高可信"],
  },
  agreement: {
    title: "代发协议号",
    badge: "核心标识",
    confidence: "81%",
    summary: "系统已把它识别为主输出标识，并检测到主表与历史表都通过该标识关联。当前需要人工确认是否保留为统一主键。",
    kv: [
      { label: "关系状态", value: "主表 / 历史表双向关联" },
      { label: "证据命中", value: "原文第 12、18、32 行" },
      { label: "人工动作", value: "接受 / 合并 / 拆分" },
    ],
    tags: ["待确认关系", "双表关联", "需要人工判断"],
  },
  main: {
    title: "主表明细",
    badge: "来源表",
    confidence: "90%",
    summary: "主表已被系统识别为 2014 年后的默认查询入口，适合作为首选来源契约候选。",
    kv: [
      { label: "适用时段", value: "2014 至今" },
      { label: "字段落位", value: "已识别 11 / 14 个候选字段" },
      { label: "状态", value: "待进入元数据对齐" },
    ],
    tags: ["主表来源", "可落位", "后续对齐"],
  },
  history: {
    title: "历史明细表",
    badge: "补历史表",
    confidence: "67%",
    summary: "系统识别出它只在历史时段补查时启用，当前低于自动接受阈值，需要人工确认是否纳入同一场景覆盖。",
    kv: [
      { label: "适用时段", value: "2014 年之前" },
      { label: "关系状态", value: "候选补查链路" },
      { label: "风险", value: "缺少完整字段说明" },
    ],
    tags: ["低置信度", "历史补查", "待人工确认"],
  },
  evidence: {
    title: "证据片段",
    badge: "原文锚点",
    confidence: "100%",
    summary: "证据片段已经带有行号定位，可在后续 Scene（业务场景）和 Plan（方案资产）详情中继续挂载，不必再重新检索原文。",
    kv: [
      { label: "命中行号", value: "12-18 / 31-35" },
      { label: "挂载对象", value: "场景标题、关系边、时段说明" },
      { label: "追踪值", value: "trace_ingest_20260327_01" },
    ],
    tags: ["证据锚点", "可复用", "可追踪"],
  },
};

const CONSOLE_TABS = [
  {
    value: "business",
    label: "业务输出",
    title: "解析与确认输出",
    description: "只显示当前步骤最值得人工判断的业务提示与阻断信息。",
    logs: [
      { at: "09:31:22", level: "success", message: "已识别 1 个候选业务场景：代发明细查询" },
      { at: "09:31:26", level: "info", message: "已发现主表路由和历史补查路由，建议人工确认覆盖边界" },
      { at: "09:31:31", level: "warn", message: "检测到 1 个低置信度来源表：历史明细表" },
      { at: "09:31:38", level: "info", message: "证据片段已挂载到场景标题、输出字段和时段说明" },
    ],
  },
  {
    value: "trace",
    label: "技术追踪",
    title: "控制台与追踪摘要",
    description: "保留本次原型想表达的 Console（控制台）/ Trace（追踪） 视角。",
    logs: [
      {
        at: "09:31:20",
        level: "info",
        message: "source_intake_contract 已通过最小字段校验",
        detail: "{\n  \"batch_id\": \"ingest_demo_20260327\",\n  \"required_fields\": 8,\n  \"validated\": 8\n}",
      },
      {
        at: "09:31:27",
        level: "warn",
        message: "relation_candidates[6] 置信度低于自动接受阈值",
        detail: "{\n  \"edge\": \"代发协议号 -> 历史明细表\",\n  \"confidence\": 0.67,\n  \"reason_code\": \"LOW_CONFIDENCE_HISTORY_BRANCH\"\n}",
      },
      {
        at: "09:31:39",
        level: "success",
        message: "evidence_fragment 已写入可追踪锚点",
        detail: "{\n  \"trace_id\": \"trace_ingest_20260327_01\",\n  \"anchors\": [12, 18, 31, 35]\n}",
      },
    ],
  },
];

const SAMPLE_MATERIAL = {
  id: "sample-payroll-ingest",
  name: "代发明细查询-上传样例材料.txt",
  title: "代发明细查询样例材料",
  candidateFields: "公司户口号、代发协议号、交易日期、金额、收款账号",
  size: 3258,
  type: "OCR TEXT / 口径整理",
  source: "已归档样例",
  href: "/samples/ingest/代发明细查询-上传样例材料.txt",
};

function formatFileSize(bytes) {
  if (!bytes || Number.isNaN(bytes)) {
    return "-";
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  return `${(bytes / 1024).toFixed(1)} KB`;
}

function stripExtension(name) {
  return `${name || ""}`.replace(/\.[^.]+$/, "");
}

function toLocalMaterial(file) {
  return {
    id: `${file.name}-${file.lastModified || Date.now()}`,
    name: file.name,
    title: stripExtension(file.name),
    candidateFields: "待解析后回填",
    size: file.size || 0,
    type: file.type || "本地文件",
    source: "本地上传",
    href: "",
  };
}

export function PrototypeIngestGraphPage({
  nextPath = "/prototype/modeling-dual-pane",
  nextLabel = "进入资产建模",
}) {
  const [selectedId, setSelectedId] = useState("agreement");
  const [queuedMaterials, setQueuedMaterials] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [uploadNotice, setUploadNotice] = useState("当前还未选择材料，可上传本地文件或直接载入归档样例。");
  const fileInputRef = useRef(null);
  const detail = DETAIL_BY_NODE[selectedId] || DETAIL_BY_NODE.agreement;
  const currentMaterial = queuedMaterials[0] || SAMPLE_MATERIAL;

  function handleOpenFilePicker() {
    fileInputRef.current?.click();
  }

  function handleFiles(files) {
    const next = Array.from(files || []).map((file) => toLocalMaterial(file));
    if (!next.length) {
      return;
    }
    setQueuedMaterials(next);
    setUploadNotice(`已接收 ${next.length} 份本地材料，可直接开始解析或继续补充更多文件。`);
  }

  function handleUseSampleMaterial() {
    setQueuedMaterials([SAMPLE_MATERIAL]);
    setUploadNotice("已载入归档样例，可直接开始解析并进入后续图谱确认。");
  }

  function handleStartParse() {
    if (!queuedMaterials.length) {
      setQueuedMaterials([SAMPLE_MATERIAL]);
      setUploadNotice("当前未选择本地文件，已自动载入归档样例并进入解析准备态。");
    } else {
      setUploadNotice(`已读取 ${queuedMaterials.length} 份材料，当前页面展示的是解析后的样例图谱与初判结果。`);
    }
    setSelectedId("scene");
  }

  return (
    <PrototypeWorkbenchShell
      kicker="知识生产台 / Step 1-2 / 建图模式"
      title="上传材料，完成实体抽取与图谱构建"
      description="主舞台先展示上传区和抽取结果，再让人工在图谱与右侧面板里完成确认，而不是先进入长表单。"
      tags={[
        { label: "图谱主舞台", tone: "good" },
        { label: "待人工确认 7 项", tone: "warn" },
      ]}
      steps={STEPS}
      consoleTabs={CONSOLE_TABS}
      main={(
        <>
          <PrototypeMetricStrip items={METRICS} />

          <div className="proto-grid-two">
            <UiCard className="proto-dropzone-card" elevation="card">
              <div className="proto-card-head">
                <div>
                  <h3>上传材料，开始构图</h3>
                  <p className="subtle-note">支持口径文档、SQL 样例、工单截图和 OCR 文本，本轮原型只演示静态态势。</p>
                </div>
                <UiBadge tone="neutral">来源接入已填写</UiBadge>
              </div>

              <input
                ref={fileInputRef}
                className="visually-hidden"
                type="file"
                multiple
                accept=".pdf,.doc,.docx,.sql,.png,.jpg,.jpeg,.txt"
                onChange={(event) => handleFiles(event.target.files)}
              />

              <button
                type="button"
                className={`proto-dropzone ${isDragging ? "is-dragging" : ""}`}
                onClick={handleOpenFilePicker}
                onDragOver={(event) => {
                  event.preventDefault();
                  setIsDragging(true);
                }}
                onDragLeave={() => setIsDragging(false)}
                onDrop={(event) => {
                  event.preventDefault();
                  setIsDragging(false);
                  handleFiles(event.dataTransfer.files);
                }}
              >
                <UploadCloud size={28} strokeWidth={1.7} />
                <strong>拖拽上传材料或点击选择文件</strong>
                <span>PDF / DOCX / SQL / PNG / OCR TEXT</span>
                <small className="proto-dropzone-note">当前支持拖拽、文件选择和直接载入归档样例。</small>
              </button>

              <div className="proto-action-row">
                <UiButton onClick={handleOpenFilePicker}>选择本地材料</UiButton>
                <UiButton variant="secondary" onClick={handleUseSampleMaterial}>载入归档样例</UiButton>
                <UiButton
                  as="a"
                  href={SAMPLE_MATERIAL.href}
                  variant="secondary"
                  target="_blank"
                  rel="noreferrer"
                >
                  下载样例材料
                </UiButton>
              </div>

              <div className="proto-upload-list" aria-label="当前已接收材料">
                {(queuedMaterials.length ? queuedMaterials : [SAMPLE_MATERIAL]).map((item) => (
                  <article key={item.id} className="proto-upload-row">
                    <div className="proto-upload-row-main">
                      <strong>{item.name}</strong>
                      <p>{item.source} · {item.type}</p>
                    </div>
                    <div className="proto-upload-row-side">
                      <span>{formatFileSize(item.size)}</span>
                      {item.href ? (
                        <a href={item.href} target="_blank" rel="noreferrer">查看材料</a>
                      ) : (
                        <span>待本地解析</span>
                      )}
                    </div>
                  </article>
                ))}
              </div>

              <div className="proto-inline-fields">
                <UiInput value={currentMaterial.title} readOnly aria-label="材料标题" hint="标题已从当前选中材料同步" />
                <UiInput value={currentMaterial.candidateFields} readOnly aria-label="候选字段" hint="样例材料已内置核心字段，真实上传文件会在解析后回填" />
              </div>

              <div className="proto-action-row">
                <UiButton onClick={handleStartParse}>开始解析</UiButton>
                <UiButton variant="secondary">查看历史批次</UiButton>
              </div>
              <p className="subtle-note">{uploadNotice}</p>
            </UiCard>

            <UiCard className="proto-card-stack" elevation="card">
              <div className="proto-card-head">
                <div>
                  <h3>系统初判摘要</h3>
                  <p className="subtle-note">先展示当前批次最重要的可信度判断和缺口，不把用户带进辅助说明里。</p>
                </div>
                <FileStack size={18} strokeWidth={1.9} />
              </div>
              <PrototypeKvList
                items={[
                  { label: "主对象", value: "代发协议号" },
                  { label: "候选场景", value: "代发明细查询" },
                  { label: "时段拆分", value: "2014 至今主表 / 2014 年之前历史补查" },
                  { label: "阻断项", value: "历史明细表字段说明不完整" },
                ]}
              />
              <div className="proto-chip-row">
                <span className="proto-soft-chip tone-good">主表已识别</span>
                <span className="proto-soft-chip tone-warn">待确认历史表</span>
                <span className="proto-soft-chip tone-neutral">证据已挂载</span>
              </div>
            </UiCard>
          </div>

          <PrototypeGraphCanvas
            title="实体抽取结果与候选关系图"
            description="点击节点查看右侧的当前步骤操作，保持“看图即工作”的节奏。"
            nodes={NODES}
            edges={EDGES}
            legend={LEGEND}
            selectedId={selectedId}
            onSelect={setSelectedId}
          />
        </>
      )}
      side={(
        <>
          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>当前对象</h3>
                <p className="subtle-note">右侧不是纯详情抽屉，而是当前步骤的检查与操作区。</p>
              </div>
              <UiBadge tone={detail.badge.includes("低置信度") ? "warn" : "good"}>{detail.badge}</UiBadge>
            </div>
            <h4 className="proto-side-title">{detail.title}</h4>
            <p className="proto-side-summary">{detail.summary}</p>
            <div className="proto-emphasis-row">
              <span>置信度</span>
              <strong>{detail.confidence}</strong>
            </div>
            <PrototypeKvList items={detail.kv} />
            <div className="proto-chip-row">
              {detail.tags.map((tag) => <span key={tag} className="proto-soft-chip tone-neutral">{tag}</span>)}
            </div>
            <div className="proto-action-row">
              <UiButton icon={<CheckCircle2 size={16} />}>接受当前判断</UiButton>
              <UiButton variant="secondary">合并 / 拆分</UiButton>
              <UiButton variant="secondary">驳回并转缺口</UiButton>
            </div>
          </UiCard>

          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>来源接入契约</h3>
                <p className="subtle-note">把标题、主对象、默认时间和敏感级别这些硬门禁前置。</p>
              </div>
            </div>
            <PrototypeKvList
              items={[
                { label: "标题", value: "代发历史口径整理" },
                { label: "主对象", value: "代发协议号" },
                { label: "默认时间", value: "交易日期" },
                { label: "敏感级别", value: "中敏，字段级控制" },
                { label: "来源线索", value: "主表明细 / 历史明细表" },
              ]}
            />
          </UiCard>

          <UiCard className="proto-side-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>证据片段</h3>
                <p className="subtle-note">证据片段必须可回到原文锚点，不能只留下抽取后的结论。</p>
              </div>
            </div>
            <UiTextarea
              value={"[012] Step 1：根据公司户口号查询代发协议号\n[018] Step 2：若历史表命中，则补查 2014 年之前交易明细\n[031] 输出字段：协议号、交易日期、金额、收款账号"}
              readOnly
              aria-label="证据片段"
              hint="当前片段会同时挂载到场景标题、时段说明和关系边。"
            />
            <div className="proto-action-row">
              <UiButton variant="secondary">查看原文定位</UiButton>
              <UiButton variant="secondary">复制追踪编号</UiButton>
            </div>
          </UiCard>

          <UiCard className="proto-side-card proto-next-card" elevation="card">
            <div className="proto-card-head">
              <div>
                <h3>下一步</h3>
                <p className="subtle-note">确认完当前批次后，进入 Step 3 的双栏建模页。</p>
              </div>
            </div>
            <p className="proto-side-summary">进入元数据对齐和资产建模时，图谱继续保留在左侧，右侧集中编辑治理对象。</p>
            <UiButton as={Link} to={nextPath} variant="secondary" icon={<ArrowRight size={16} />}>
              {nextLabel}
            </UiButton>
          </UiCard>
        </>
      )}
    />
  );
}
