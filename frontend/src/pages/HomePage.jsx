import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Search } from "lucide-react";
import { apiRequest } from "../api/client";
import { UiBadge, UiButton, UiCard, UiEmptyState, UiInput } from "../components/ui";

const DATETIME_FORMATTER = new Intl.DateTimeFormat("zh-CN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

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
  const [rows, setRows] = useState([]);
  const [viewMode, setViewMode] = useState("recent");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const metrics = useMemo(() => {
    const source = overviewRows;
    const draft = source.filter((row) => row.status === "DRAFT").length;
    const published = source.filter((row) => row.status === "PUBLISHED").length;
    const highRisk = source.filter((row) => isHighRisk(row)).length;
    const today = source.filter((row) => {
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

  useEffect(() => {
    loadOverview();
  }, []);

  async function loadOverview() {
    setLoading(true);
    setError("");
    try {
      const result = await apiRequest("/scenes");
      const normalized = normalizeRows(result);
      setOverviewRows(normalized);
      setRows(normalized.slice(0, 12));
      setViewMode("recent");
    } catch (err) {
      setError(normalizeErrorMessage(err, "加载首页内容失败，请稍后重试"));
    } finally {
      setLoading(false);
    }
  }

  async function runSearch(keywordOverride) {
    const trimmed = `${keywordOverride ?? keyword}`.trim();
    if (!trimmed) {
      setRows(overviewRows.slice(0, 12));
      setViewMode("recent");
      return;
    }
    setLoading(true);
    setError("");
    try {
      const result = await apiRequest("/scenes", {
        query: { keyword: trimmed },
      });
      setRows(normalizeRows(result));
      setViewMode("search");
    } catch (err) {
      setError(normalizeErrorMessage(err, "查询失败，请稍后重试"));
    } finally {
      setLoading(false);
    }
  }

  const listTitle = viewMode === "search" ? `搜索结果：${keyword.trim()}` : "最近更新场景";
  const listHint = viewMode === "search"
    ? "按场景标题、描述、SQL 内容匹配结果"
    : "默认展示最近更新，便于快速进入编辑与发布";

  return (
    <section className="panel home-focus-panel">
      <section className="home-hero" data-animate="1">
        <p className="home-hero-kicker">Search-first 工作台</p>
        <h2>先搜索，再进入治理动作</h2>
        <p className="subtle-note">搜索全局资产：场景、业务领域、SQL 片段，优先定位可复用内容。</p>
        <div className="home-search-grid">
          <label className="visually-hidden" htmlFor="homeKeyword">全局搜索关键词</label>
          <div className="home-search-shell">
            <UiInput
              id="homeKeyword"
              name="homeKeyword"
              autoComplete="off"
              type="text"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  runSearch();
                }
              }}
              prefix={<Search size={17} strokeWidth={1.9} />}
              placeholder="搜索全局资产：场景/表/字段… 例如：零售客户"
            />
            <UiButton className="home-search-btn" onClick={runSearch} disabled={loading}>
              {loading ? "查询中…" : "搜索"}
            </UiButton>
          </div>
        </div>
        {hotDomains.length > 0 ? (
          <div className="home-hot-domains">
            <span className="subtle-note">热门业务领域：</span>
            {hotDomains.map((name) => (
              <button
                key={name}
                type="button"
                className="home-domain-chip"
                onClick={() => {
                  setKeyword(name);
                  runSearch(name);
                }}
              >
                {name}
              </button>
            ))}
          </div>
        ) : null}
      </section>

      <div className="home-quick-actions">
        <UiButton as={Link} to="/knowledge/import" variant="ghost">导入口径文档</UiButton>
        <UiButton as={Link} to="/knowledge/manual" variant="ghost">手动创建</UiButton>
        <UiButton as={Link} to="/workspace/todo" variant="ghost">我的待办</UiButton>
        <UiButton as={Link} to="/assets/map" variant="ghost">浏览数据地图</UiButton>
      </div>

      <div className="home-journey-stats">
        <UiCard as="article" className="home-stat-card" data-animate="2">
          <div className="home-stat-head"><span className="home-stat-icon" aria-hidden="true">◉</span><span>草稿待完善</span></div>
          <strong className="home-stat-value home-stat-value-draft">{metrics.draft}</strong>
          <small className="home-stat-delta">需优先补充发布信息</small>
        </UiCard>
        <UiCard as="article" className="home-stat-card" data-animate="3">
          <div className="home-stat-head"><span className="home-stat-icon" aria-hidden="true">▲</span><span>已发布场景</span></div>
          <strong className="home-stat-value home-stat-value-published">{metrics.published}</strong>
          <small className="home-stat-delta">可供一线直接复用</small>
        </UiCard>
        <UiCard as="article" className="home-stat-card" data-animate="4">
          <div className="home-stat-head"><span className="home-stat-icon" aria-hidden="true">!</span><span>高风险提醒</span></div>
          <strong className="home-stat-value home-stat-value-risk">{metrics.highRisk}</strong>
          <small className="home-stat-delta">建议优先复核质量</small>
        </UiCard>
        <UiCard as="article" className="home-stat-card" data-animate="5">
          <div className="home-stat-head"><span className="home-stat-icon" aria-hidden="true">↗</span><span>今日更新</span></div>
          <strong className="home-stat-value home-stat-value-today">{metrics.today}</strong>
          <small className="home-stat-delta">近 24 小时新增变更</small>
        </UiCard>
      </div>

      {error ? <p className="field-hint danger-text" role="status" aria-live="polite">{error}</p> : null}

      <div className="home-list-head">
        <h3>{listTitle}</h3>
        <p className="subtle-note">{listHint}</p>
      </div>
      <div className="home-scene-list" role="table" aria-label={listTitle}>
        <div className="home-scene-row home-scene-row-head" role="row">
          <span role="columnheader">场景ID</span>
          <span role="columnheader">标题</span>
          <span role="columnheader">业务领域</span>
          <span role="columnheader">状态</span>
          <span role="columnheader">更新时间</span>
        </div>
        {rows.length === 0 ? (
          <UiEmptyState
            className="home-scene-empty"
            icon={<HomeEmptyIllustration />}
            title={viewMode === "search" ? "未匹配到结果" : "暂无可展示场景"}
            description={viewMode === "search" ? "请尝试缩短关键词或切换业务词汇" : "可先导入口径文档或手动创建场景。"}
          />
        ) : rows.map((row) => (
          <article className="home-scene-row" key={row.id} role="row">
            <span role="cell">{row.id}</span>
            <span role="cell" className="home-scene-title-cell">
              <strong>{row.sceneTitle || "-"}</strong>
              {needsManualCompletion(row.sceneTitle) ? (
                <UiBadge tone="warn" className="home-warning-badge" aria-label="需人工补全">需人工补全</UiBadge>
              ) : null}
            </span>
            <span role="cell">{row.domainName || row.domain || "-"}</span>
            <span role="cell">{row.status === "PUBLISHED" ? "已发布" : "草稿"}</span>
            <span role="cell">{formatTime(row.updatedAt)}</span>
          </article>
        ))}
      </div>
    </section>
  );
}
