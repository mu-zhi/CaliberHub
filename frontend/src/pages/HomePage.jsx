import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest } from "../api/client";
import { API_CONTRACTS } from "../api/contracts";
import { UiBadge, UiButton, UiCard, UiEmptyState, UiInput } from "../components/ui";
import { describeSceneStatus } from "../components/ui/statusPresentation";

const DATETIME_FORMATTER = new Intl.DateTimeFormat("zh-CN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

const TODO_ITEMS = [
  { title: "补齐历史补查覆盖缺口", owner: "知识生产台", tone: "warn", link: "/production/modeling" },
  { title: "审批模板绑定到扩展契约视图", owner: "发布中心", tone: "warn", link: "/publish" },
  { title: "处理待审批导出申请", owner: "审批与导出", tone: "neutral", link: "/approval" },
];

const ACTIONS = [
  {
    title: "开始知识生产",
    summary: "上传材料，进入实体抽取、图谱构建和资产建模主链路。",
    path: "/production/ingest",
    cta: "进入知识生产台",
  },
  {
    title: "查看数据地图",
    summary: "从图谱、路径和证据回溯进入当前知识底座的关系视图。",
    path: "/map",
    cta: "打开数据地图",
  },
  {
    title: "运行样板验证",
    summary: "验证 Scene（业务场景）、Coverage（覆盖声明）、Policy（策略对象）和知识包解释。",
    path: "/runtime",
    cta: "进入运行决策台",
  },
  {
    title: "检查候选发布",
    summary: "先看阻断项、回滚点和契约兑现情况，再决定是否整体切换。",
    path: "/publish",
    cta: "进入发布中心",
  },
];

function formatTime(value) {
  if (!value) {
    return "-";
  }
  const dt = new Date(value);
  if (Number.isNaN(dt.getTime())) {
    return value;
  }
  return DATETIME_FORMATTER.format(dt).replace(/\//g, "-");
}

function isHighRisk(scene) {
  const raw = `${scene?.qualityJson || ""}`.toLowerCase();
  return raw.includes("high") || raw.includes("高");
}

function normalizeRows(result) {
  if (!Array.isArray(result)) {
    return [];
  }
  return [...result].sort((a, b) => {
    const left = new Date(a?.updatedAt || a?.createdAt || 0).getTime();
    const right = new Date(b?.updatedAt || b?.createdAt || 0).getTime();
    return right - left;
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

function needsManualCompletion(sceneTitle) {
  const title = `${sceneTitle || ""}`.trim();
  if (!title) {
    return true;
  }
  return title.length <= 2 || title.includes("/*");
}

function matchesKeyword(row, keyword) {
  const matcher = `${keyword || ""}`.trim().toLowerCase();
  if (!matcher) {
    return true;
  }
  return [
    row?.id,
    row?.sceneTitle,
    row?.domainName,
    row?.domain,
    row?.status,
  ].some((value) => `${value || ""}`.toLowerCase().includes(matcher));
}

function HomeEmptyIllustration() {
  return (
    <svg viewBox="0 0 160 120" role="img" aria-hidden="true">
      <defs>
        <linearGradient id="homeTrayStroke" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#cfd7df" />
          <stop offset="100%" stopColor="#b8c3ce" />
        </linearGradient>
      </defs>
      <path d="M20 42h42l10 14h68v38a10 10 0 0 1-10 10H30A10 10 0 0 1 20 94z" fill="#eef2f6" />
      <path d="M20 42h42l10 14h68v38a10 10 0 0 1-10 10H30A10 10 0 0 1 20 94z" fill="none" stroke="url(#homeTrayStroke)" strokeWidth="2" />
      <path d="M20 56h120" stroke="#d4dde6" strokeWidth="2" />
      <circle cx="56" cy="74" r="11" fill="none" stroke="#b8c3ce" strokeWidth="2.2" />
      <path d="M64 82l8 8" stroke="#b8c3ce" strokeWidth="2.2" strokeLinecap="round" />
      <circle cx="112" cy="33" r="5" fill="#d8e1e9" />
      <circle cx="126" cy="40" r="3.5" fill="#d8e1e9" />
    </svg>
  );
}

export function HomePage() {
  const [keyword, setKeyword] = useState("");
  const [overviewRows, setOverviewRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function loadOverview() {
      setLoading(true);
      setError("");
      try {
        const result = await apiRequest(API_CONTRACTS.scenes);
        if (!active) {
          return;
        }
        setOverviewRows(normalizeRows(result));
      } catch (err) {
        if (!active) {
          return;
        }
        setError(normalizeErrorMessage(err, "加载首页总览失败，请稍后重试"));
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadOverview();

    return () => {
      active = false;
    };
  }, []);

  const filteredRows = useMemo(
    () => overviewRows.filter((row) => matchesKeyword(row, keyword)),
    [keyword, overviewRows],
  );

  const metrics = useMemo(() => {
    const draft = overviewRows.filter((row) => row.status === "DRAFT").length;
    const published = overviewRows.filter((row) => row.status === "PUBLISHED").length;
    const highRisk = overviewRows.filter((row) => isHighRisk(row)).length;
    const today = overviewRows.filter((row) => {
      if (!row.updatedAt) {
        return false;
      }
      const dt = new Date(row.updatedAt);
      const now = new Date();
      return dt.getFullYear() === now.getFullYear() && dt.getMonth() === now.getMonth() && dt.getDate() === now.getDate();
    }).length;
    return { draft, published, highRisk, today };
  }, [overviewRows]);

  const hotDomains = useMemo(() => {
    const counter = new Map();
    overviewRows.forEach((row) => {
      const key = `${row.domainName || row.domain || ""}`.trim();
      if (!key) {
        return;
      }
      counter.set(key, (counter.get(key) || 0) + 1);
    });
    return [...counter.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 4)
      .map(([name]) => name);
  }, [overviewRows]);

  const recentRows = filteredRows.slice(0, 6);
  const riskRows = filteredRows.filter((row) => isHighRisk(row) || needsManualCompletion(row.sceneTitle)).slice(0, 4);
  const updatedAt = overviewRows[0]?.updatedAt || overviewRows[0]?.createdAt || "";

  return (
    <section className="panel home-overview-panel">
      <section className="home-overview-hero" data-animate="1">
        <div className="home-overview-copy">
          <p className="home-overview-kicker">首页总览 / 指挥页</p>
        </div>
        <div className="home-overview-statusbar" aria-label="当前全局状态">
          <div className="home-overview-statuschip">
            <span>稳定版本</span>
            <strong>v0.2</strong>
          </div>
          <div className="home-overview-statuschip home-overview-statuschip-warn">
            <span>候选发布</span>
            <strong>2 个阻断项</strong>
          </div>
          <div className="home-overview-statuschip">
            <span>待审批</span>
            <strong>2 单</strong>
          </div>
          <div className="home-overview-statuschip">
            <span>运行告警</span>
            <strong>2 条</strong>
          </div>
          <div className="home-overview-statuschip">
            <span>更新时间</span>
            <strong>{formatTime(updatedAt)}</strong>
          </div>
        </div>
      </section>

      <div className="home-overview-actions">
        {ACTIONS.map((item, index) => (
          <UiCard key={item.title} className="home-overview-action-card" data-animate={`${index + 2}`}>
            <div className="home-overview-action-copy">
              <h3>{item.title}</h3>
              <p>{item.summary}</p>
            </div>
            <UiButton as={Link} to={item.path} variant="ghost">
              {item.cta}
            </UiButton>
          </UiCard>
        ))}
      </div>

      <div className="home-overview-metrics">
        <UiCard className="home-overview-metric-card">
          <span>已发布场景</span>
          <strong>{metrics.published}</strong>
          <small>当前可对外稳定使用</small>
        </UiCard>
        <UiCard className="home-overview-metric-card">
          <span>草稿待完善</span>
          <strong>{metrics.draft}</strong>
          <small>仍需补齐契约、覆盖或策略</small>
        </UiCard>
        <UiCard className="home-overview-metric-card">
          <span>高风险提醒</span>
          <strong>{metrics.highRisk}</strong>
          <small>建议优先复核证据与覆盖边界</small>
        </UiCard>
        <UiCard className="home-overview-metric-card">
          <span>今日更新</span>
          <strong>{metrics.today}</strong>
          <small>近 24 小时新增或更新场景</small>
        </UiCard>
      </div>

      {error ? <p className="field-hint danger-text" role="status" aria-live="polite">{error}</p> : null}

      <div className="home-overview-grid">
        <UiCard className="home-overview-panel-card home-overview-panel-card-wide">
          <div className="home-overview-section-head">
            <div>
              <h3>近期变更</h3>
              <p className="subtle-note">优先展示最近发生变化、需要继续推进的业务场景。</p>
            </div>
            <UiBadge tone="neutral">{keyword.trim() ? "已过滤" : "全部最近更新"}</UiBadge>
          </div>
          {recentRows.length === 0 ? (
            <UiEmptyState
              className="home-scene-empty"
              icon={<HomeEmptyIllustration />}
              title={keyword.trim() ? "未匹配到相关场景" : "暂无可展示场景"}
              description={keyword.trim() ? "可尝试缩短关键词或切换业务词汇。" : "请先进入知识生产台创建或导入场景。"}
            />
          ) : (
            <div className="home-overview-list">
              {recentRows.map((row) => {
                const statusPresentation = describeSceneStatus(row.status);
                return (
                  <article key={row.id} className="home-overview-list-row">
                    <div className="home-overview-row-main">
                      <div className="home-overview-row-title">
                        <strong>{row.sceneTitle || "-"}</strong>
                        {needsManualCompletion(row.sceneTitle) ? (
                          <UiBadge tone="warn">需人工补全</UiBadge>
                        ) : null}
                      </div>
                      <p>{row.domainName || row.domain || "未归属领域"} · 最近更新时间 {formatTime(row.updatedAt || row.createdAt)}</p>
                    </div>
                    <div className="home-overview-row-side">
                      <UiBadge tone={statusPresentation.tone}>
                        {statusPresentation.label}
                      </UiBadge>
                      <span>{row.id}</span>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </UiCard>

        <UiCard className="home-overview-panel-card">
          <div className="home-overview-section-head">
            <div>
              <h3>我的待办</h3>
              <p className="subtle-note">优先推进会阻断发布或影响运行解释的事项。</p>
            </div>
            <UiBadge tone="warn">{TODO_ITEMS.length} 项</UiBadge>
          </div>
          <div className="home-overview-list">
            {TODO_ITEMS.map((item) => (
              <article key={item.title} className="home-overview-list-row">
                <div className="home-overview-row-main">
                  <strong>{item.title}</strong>
                  <p>{item.owner}</p>
                </div>
                <div className="home-overview-row-side">
                  <UiBadge tone={item.tone}>{item.tone === "warn" ? "优先处理" : "待跟进"}</UiBadge>
                  <UiButton as={Link} to={item.link} variant="ghost" size="sm">进入</UiButton>
                </div>
              </article>
            ))}
          </div>
        </UiCard>

        <UiCard className="home-overview-panel-card">
          <div className="home-overview-section-head">
            <div>
              <h3>重点风险</h3>
              <p className="subtle-note">把高风险、需人工补全和发布阻断候选集中显示。</p>
            </div>
            <UiBadge tone="warn">{riskRows.length || 0} 项</UiBadge>
          </div>
          {riskRows.length === 0 ? (
            <p className="subtle-note">当前没有额外风险项，建议进入发布中心确认阻断项是否已清零。</p>
          ) : (
            <div className="home-overview-list">
              {riskRows.map((row) => (
                <article key={`risk-${row.id}`} className="home-overview-list-row">
                  <div className="home-overview-row-main">
                    <strong>{row.sceneTitle || row.id}</strong>
                    <p>{isHighRisk(row) ? "命中高风险质量提示" : "命中人工补全提示"}</p>
                  </div>
                  <div className="home-overview-row-side">
                    <UiBadge tone="warn">{isHighRisk(row) ? "高风险" : "待补全"}</UiBadge>
                  </div>
                </article>
              ))}
            </div>
          )}
        </UiCard>
      </div>

      <div className="home-overview-grid">
        <UiCard className="home-overview-panel-card">
          <div className="home-overview-section-head">
            <div>
              <h3>运行健康</h3>
              <p className="subtle-note">首页只保留最关键的运行和发布健康信号，不展开技术细节。</p>
            </div>
          </div>
          <div className="home-overview-health">
            <article className="home-overview-health-row">
              <strong>场景命中率</strong>
              <span>93% · 近 24 小时</span>
            </article>
            <article className="home-overview-health-row">
              <strong>发布通过率</strong>
              <span>84% · 含 2 个阻断发布</span>
            </article>
            <article className="home-overview-health-row">
              <strong>审批积压</strong>
              <span>2 单 · 当前均在 SLA 内</span>
            </article>
            <article className="home-overview-health-row">
              <strong>图查询超时</strong>
              <span>近 1 小时 0 次</span>
            </article>
          </div>
        </UiCard>

        <UiCard className="home-overview-panel-card">
          <div className="home-overview-section-head">
            <div>
              <h3>快速定位</h3>
              <p className="subtle-note">搜索下沉为辅助动作，便于快速定位最近改动的场景或领域。</p>
            </div>
          </div>
          <div className="home-overview-search">
            <UiInput
              id="homeOverviewKeyword"
              name="homeOverviewKeyword"
              autoComplete="off"
              type="text"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="输入场景、领域、状态或编号"
            />
            <div className="home-hot-domains">
              <span className="subtle-note">热门业务领域：</span>
              {hotDomains.map((name) => (
                <button
                  key={name}
                  type="button"
                  className="home-domain-chip"
                  onClick={() => setKeyword(name)}
                >
                  {name}
                </button>
              ))}
            </div>
          </div>
        </UiCard>
      </div>

      {loading ? <p className="subtle-note">正在刷新首页总览…</p> : null}
    </section>
  );
}
