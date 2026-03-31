import { Link } from "react-router-dom";
import { ArrowRight, Blocks, GitBranchPlus, PlayCircle } from "lucide-react";
import { UiBadge, UiButton, UiCard } from "../components/ui";

const PROTOTYPE_CARDS = [
  {
    path: "/prototype/ingest-graph",
    icon: <GitBranchPlus size={18} strokeWidth={1.9} />,
    title: "Step 1-2：材料接入与图谱构建",
    summary: "上传材料、抽取实体和关系、在图谱上完成首轮可信度复核。",
    points: ["上传区先行", "图谱为主舞台", "右侧显示证据与确认动作"],
  },
  {
    path: "/prototype/modeling-dual-pane",
    icon: <Blocks size={18} strokeWidth={1.9} />,
    title: "Step 3：双栏校正与资产建模",
    summary: "保留图谱上下文，把抽取事实落成正式治理资产与规则对象。",
    points: ["双栏模式", "左看图谱与覆盖", "右侧编辑 Scene / Plan / Coverage 等对象"],
  },
  {
    path: "/prototype/runtime-publish",
    icon: <PlayCircle size={18} strokeWidth={1.9} />,
    title: "Step 4-5：运行验证与发布检查",
    summary: "在同一骨架里串起 Knowledge Package（知识包） 验证和候选版本发布检查。",
    points: ["工作台模式", "运行链路 8 步解释", "发布检查与回滚预案同屏查看"],
  },
];

export function PrototypeIndexPage() {
  return (
    <section className="panel proto-index-page">
      <div className="panel-head">
        <div>
          <h2>图谱驱动工作台原型</h2>
          <p>这 3 张静态原型页只用于结构定型和评审，不替代现有正式业务页面，也不进入一级业务导航。</p>
        </div>
        <UiBadge tone="neutral">隐藏原型路由</UiBadge>
      </div>

      <div className="proto-index-hero">
        <div>
          <p className="proto-page-kicker">Prototype / 评审入口</p>
          <h3>先把主舞台做对，再把能力拆回正式工作台</h3>
          <p className="subtle-note">
            原型统一承接“顶部阶段条 + 主工作区 + 右侧 Inspector（检查面板）/ Action Panel（操作面板） + 底部 Console（控制台）/ Trace（追踪）”骨架。
          </p>
        </div>
        <UiButton as={Link} to="/prototype/ingest-graph" icon={<ArrowRight size={16} />}>
          进入第一张原型
        </UiButton>
      </div>

      <div className="proto-index-grid">
        {PROTOTYPE_CARDS.map((item) => (
          <UiCard key={item.path} as="article" className="proto-index-card" elevation="card">
            <div className="proto-index-card-head">
              <span className="proto-index-icon" aria-hidden="true">{item.icon}</span>
              <h3>{item.title}</h3>
            </div>
            <p>{item.summary}</p>
            <ul className="proto-bullet-list">
              {item.points.map((point) => <li key={point}>{point}</li>)}
            </ul>
            <UiButton as={Link} to={item.path} variant="secondary" icon={<ArrowRight size={16} />}>
              查看原型
            </UiButton>
          </UiCard>
        ))}
      </div>
    </section>
  );
}
