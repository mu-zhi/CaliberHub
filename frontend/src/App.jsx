import { useEffect, useMemo, useRef, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { HomePage } from "./pages/HomePage";
import { DomainManagementPage } from "./pages/DomainManagementPage";
import { AssetsPage } from "./pages/AssetsPage";
import { ApprovalExportPage } from "./pages/ApprovalExportPage";
import { MonitoringAuditPage } from "./pages/MonitoringAuditPage";
import { PublishCenterPage } from "./pages/PublishCenterPage";
import { WorkspacePage } from "./pages/WorkspacePage";
import { SystemPage } from "./pages/SystemPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { PrototypeIndexPage } from "./pages/PrototypeIndexPage";
import { KnowledgePage } from "./pages/KnowledgePage";
import { KnowledgePackageWorkbenchPage } from "./pages/KnowledgePackageWorkbenchPage";
import { BrandMark } from "./components/BrandMark";
import { AppErrorBoundary } from "./components/AppErrorBoundary";
import { findRoute, isTopModuleAccessible, LEGACY_ROUTE_REDIRECTS, routesByTop, TOP_MODULES } from "./routes";
import { useAuthStore } from "./store/authStore";
import { useAppStore } from "./store/appStore";
import { PrototypeIngestGraphPage } from "./pages/prototypes/PrototypeIngestGraphPage";
import { PrototypeModelingDualPanePage } from "./pages/prototypes/PrototypeModelingDualPanePage";
import { PrototypeRuntimePublishPage } from "./pages/prototypes/PrototypeRuntimePublishPage";

function routeLabel(pathname) {
  return findRoute(pathname)?.label || "首页总览";
}

function routeNotice(route) {
  if (route?.maturity === "prototype") {
    return {
      tone: "prototype",
      title: "原型评审态",
      message: "当前页面用于结构评审与交互定型，不计入正式交付范围。",
    };
  }
  if (route?.maturity === "sample") {
    return {
      tone: "sample",
      title: "样例数据页",
      message: "当前页面仍以样例数据表达流程边界，需和真实审批/审计链路分开理解。",
    };
  }
  return null;
}

const ROLE_OPTIONS = [
  { value: "admin", label: "系统管理员" },
  { value: "support", label: "数据支持" },
  { value: "expert", label: "数据专家" },
  { value: "governance", label: "治理专员" },
  { value: "frontline", label: "一线用户" },
  { value: "compliance", label: "合规审计" },
];

function AppShell({ children }) {
  const location = useLocation();
  const role = useAuthStore((state) => state.role);
  const setRole = useAuthStore((state) => state.setRole);
  const roles = useAuthStore((state) => state.roles);
  const navCollapsed = useAppStore((state) => state.navCollapsed);
  const setNavCollapsed = useAppStore((state) => state.setNavCollapsed);
  const recordRecent = useAppStore((state) => state.recordRecent);
  const toastTimerRef = useRef(null);
  const [toast, setToast] = useState({ show: false, tone: "warn", message: "" });

  function showToast(message, tone = "warn") {
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }
    setToast({ show: true, tone, message });
    toastTimerRef.current = window.setTimeout(() => {
      setToast((prev) => ({ ...prev, show: false }));
    }, 5000);
  }

  useEffect(() => {
    recordRecent(location.pathname);
  }, [location.pathname, recordRecent]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return undefined;
    }
    const onApiError = (event) => {
      const detail = event?.detail || {};
      const requestSuffix = detail.requestId ? `（请求ID：${detail.requestId}）` : "";
      if (detail.status === 401) {
        return;
      } else if (detail.status === 403) {
        showToast(`当前角色无权限执行该操作${requestSuffix}`, "warn");
      } else if (detail.status === 429) {
        showToast(`请求过于频繁，请稍后再试${requestSuffix}`, "bad");
      } else if (detail.status === 0 || detail.code === "NETWORK_ERROR") {
        showToast("网络连接异常，请检查网络后重试", "bad");
      } else if (typeof detail.status === "number" && detail.status >= 500) {
        showToast(`服务暂不可用，请稍后再试${requestSuffix}`, "bad");
      } else if (detail.message) {
        showToast(`${detail.message}${requestSuffix}`, "warn");
      }
    };
    const onUiError = (event) => {
      const detail = event?.detail || {};
      showToast(detail.message || "页面渲染异常，请刷新后重试", "bad");
    };
    window.addEventListener("dd-api-error", onApiError);
    window.addEventListener("dd-ui-error", onUiError);
    return () => {
      window.removeEventListener("dd-api-error", onApiError);
      window.removeEventListener("dd-ui-error", onUiError);
    };
  }, []);

  useEffect(() => () => {
    if (toastTimerRef.current) {
      window.clearTimeout(toastTimerRef.current);
    }
  }, []);

  const currentRoute = findRoute(location.pathname);
  const currentTopKey = currentRoute?.topKey || "overview";
  const isMainContentFocus = currentTopKey !== "tools";
  const maturityNotice = routeNotice(currentRoute);
  const isAdmin = useMemo(
    () => roles.some((item) => `${item || ""}`.replace(/^ROLE_/, "").toUpperCase() === "ADMIN"),
    [roles],
  );
  const sideRoutes = useMemo(() => routesByTop(currentTopKey, isAdmin, role), [currentTopKey, isAdmin, role]);
  const topModules = useMemo(
    () => TOP_MODULES
      .filter((item) => item.inTopNav !== false && (!item.adminOnly || isAdmin))
      .map((item) => ({ ...item, accessible: isTopModuleAccessible(item.key, role) })),
    [isAdmin, role],
  );

  return (
    <>
      <a href="#mainContent" className="skip-link">跳转到主内容</a>
      <div className="grain" />
      <div className="diag" />
      <header className="mast">
        <div className="mast-main">
          <h1 className="brand-title">
            <BrandMark />
            <span className="brand-word">
              <span className="brand-word-main">数据</span>
              <span className="brand-word-accent">直通车</span>
            </span>
            <span className="brand-slogan">让用户没有难取的数据</span>
          </h1>
        </div>
        <nav className="mast-main-nav" aria-label="一级模块导航">
          {topModules.map((item) => (
            item.accessible ? (
              <NavLink
                key={item.key}
                to={item.path}
                className={({ isActive }) => `mini-link top-link ${isActive || item.key === currentTopKey ? "is-active" : ""}`}
              >
                {item.label}
              </NavLink>
            ) : (
              <span
                key={item.key}
                className="mini-link top-link is-disabled"
                aria-disabled="true"
                title="当前角色无权限进入该工作台"
              >
                {item.label}
              </span>
            )
          ))}
        </nav>
        <div className="mast-tools">
          <div className="mast-tool-links" aria-label="全局工具区">
            <NavLink
              to="/workspace/todo"
              className={() => `mini-link mast-tool-link ${location.pathname.startsWith("/workspace") ? "is-active" : ""}`}
            >
              个人协作
            </NavLink>
            <NavLink
              to="/system/guide"
              className={() => `mini-link mast-tool-link ${location.pathname.startsWith("/system") ? "is-active" : ""}`}
            >
              系统设置
            </NavLink>
          </div>
          <div className="role-switch">
            <label htmlFor="topRoleSwitch">当前角色</label>
            <select
              id="topRoleSwitch"
              name="topRoleSwitch"
              autoComplete="off"
              value={role || "admin"}
              onChange={(event) => setRole(event.target.value)}
            >
              {ROLE_OPTIONS.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
          </div>
        </div>
      </header>

      <div className={`workspace-shell ${navCollapsed ? "nav-collapsed" : ""} ${isMainContentFocus ? "home-focus" : ""}`}>
        {!isMainContentFocus ? (
          <aside className="panel module-nav">
            <div className="module-nav-head">
              <h2 id="sideNavTitle">{TOP_MODULES.find((item) => item.key === currentTopKey)?.label || "首页"}</h2>
              <button
                id="sideNavToggle"
                className="side-nav-toggle"
                type="button"
                onClick={() => setNavCollapsed(!navCollapsed)}
              >
                {navCollapsed ? "展开" : "收起"}
              </button>
            </div>
            <p className="subtle-note" id="sideNavDesc">导航入口固定，右侧内容独立滚动。</p>
            <div className="side-nav-list">
              {sideRoutes.map((item) => {
                let tagClass = "";
                if (item.maturity === "future") {
                  tagClass = "route-tag-future";
                } else if (item.maturity === "sample") {
                  tagClass = "route-tag-sample";
                } else if (item.maturity === "prototype") {
                  tagClass = "route-tag-prototype";
                }
                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) => `module-nav-btn side-route-btn ${isActive ? "is-active" : ""}`}
                  >
                    <span className="route-label">{item.label}</span>
                    <span className="route-short" aria-hidden="true">{item.label.slice(0, 2)}</span>
                    {item.maturity !== "implemented" ? (
                      <span className={`route-tag ${tagClass}`}>[{item.statusLabel}]</span>
                    ) : null}
                  </NavLink>
                );
              })}
            </div>
          </aside>
        ) : null}

        {!isMainContentFocus ? (
          <p className="workspace-deck subtle-note">
            {(TOP_MODULES.find((item) => item.key === currentTopKey)?.label || "首页")} &gt; {routeLabel(location.pathname)}
          </p>
        ) : null}

        <main id="mainContent" className="workspace-main" tabIndex={-1}>
          {maturityNotice ? (
            <div className={`workbench-route-notice ${maturityNotice.tone}`} role="note">
              <strong>{maturityNotice.title}</strong>
              <span>{maturityNotice.message}</span>
            </div>
          ) : null}
          {children}
        </main>
      </div>
      <div className="toast-holder" aria-live="polite">
        <div className={`toast ${toast.show ? "show" : ""} ${toast.tone}`}>{toast.message}</div>
      </div>
    </>
  );
}

function PageContainer() {
  const legacyRedirectRoutes = Object.entries(LEGACY_ROUTE_REDIRECTS).map(([path, target]) => (
    <Route key={path} path={path} element={<Navigate to={target} replace />} />
  ));

  return (
    <AppShell>
      <Routes>
        <Route path="/overview" element={<HomePage />} />

        <Route path="/map" element={<AssetsPage view="map" />} />
        <Route path="/map/scenes" element={<AssetsPage view="scenes" />} />
        <Route path="/map/lineage" element={<AssetsPage view="lineage" />} />
        <Route path="/map/views" element={<AssetsPage view="views" />} />
        <Route path="/map/dicts" element={<AssetsPage view="dicts" />} />
        <Route path="/map/rules" element={<AssetsPage view="rules" />} />
        <Route path="/map/topics" element={<AssetsPage view="topics" />} />
        <Route path="/map/services" element={<AssetsPage view="services" />} />
        <Route path="/map/guide" element={<AssetsPage view="guide" />} />
        <Route path="/map/market" element={<AssetsPage view="market" />} />

        <Route path="/production" element={<Navigate to="/production/ingest" replace />} />
        <Route
          path="/production/ingest"
          element={<KnowledgePage preset="import" entry="ingest" />}
        />
        <Route
          path="/production/modeling"
          element={<KnowledgePage preset="import" entry="modeling" />}
        />
        <Route path="/production/domains" element={<DomainManagementPage />} />
        <Route path="/production/feedback" element={<Navigate to="/production/modeling" replace />} />

        <Route path="/publish" element={<PublishCenterPage />} />
        <Route path="/runtime" element={<KnowledgePackageWorkbenchPage />} />
        <Route path="/approval" element={<ApprovalExportPage />} />
        <Route path="/monitoring" element={<MonitoringAuditPage />} />

        <Route path="/workspace/todo" element={<WorkspacePage view="todo" />} />
        <Route path="/workspace/notice" element={<WorkspacePage view="notice" />} />
        <Route path="/workspace/favorites" element={<WorkspacePage view="favorites" />} />
        <Route path="/workspace/recent" element={<WorkspacePage view="recent" />} />

        <Route path="/system/guide" element={<SystemPage view="guide" />} />
        <Route path="/system/llm" element={<SystemPage view="llm" />} />
        <Route path="/system/prompts" element={<SystemPage view="prompts" />} />

        <Route path="/prototype" element={<PrototypeIndexPage />} />
        <Route path="/prototype/ingest-graph" element={<PrototypeIngestGraphPage />} />
        <Route path="/prototype/modeling-dual-pane" element={<PrototypeModelingDualPanePage />} />
        <Route path="/prototype/runtime-publish" element={<PrototypeRuntimePublishPage />} />

        {legacyRedirectRoutes}
        <Route path="/" element={<Navigate to="/overview" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppShell>
  );
}

export default function App() {
  return (
    <AppErrorBoundary>
      <Routes>
        <Route path="/*" element={<PageContainer />} />
      </Routes>
    </AppErrorBoundary>
  );
}
