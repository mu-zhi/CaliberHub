import { useEffect, useMemo, useRef, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { HomePage } from "./pages/HomePage";
import { KnowledgePage } from "./pages/KnowledgePage";
import { DomainManagementPage } from "./pages/DomainManagementPage";
import { AssetsPage } from "./pages/AssetsPage";
import { WorkspacePage } from "./pages/WorkspacePage";
import { SystemPage } from "./pages/SystemPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { BrandMark } from "./components/BrandMark";
import { AppErrorBoundary } from "./components/AppErrorBoundary";
import { findRoute, routesByTop, TOP_MODULES } from "./routes";
import { useAuthStore } from "./store/authStore";
import { useAppStore } from "./store/appStore";

function routeLabel(pathname) {
  return findRoute(pathname)?.label || "首页";
}

function topKey(pathname) {
  return findRoute(pathname)?.topKey || "home";
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

  const currentTopKey = topKey(location.pathname);
  const isMainContentFocus = currentTopKey === "home" || currentTopKey === "knowledge" || currentTopKey === "assets";
  const isAdmin = useMemo(
    () => roles.some((item) => `${item || ""}`.replace(/^ROLE_/, "").toUpperCase() === "ADMIN"),
    [roles],
  );
  const sideRoutes = useMemo(() => routesByTop(currentTopKey, isAdmin), [currentTopKey, isAdmin]);
  const topModules = useMemo(
    () => TOP_MODULES.filter((item) => item.inTopNav !== false && (!item.adminOnly || isAdmin)),
    [isAdmin],
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
            <NavLink
              key={item.key}
              to={item.path}
              className={({ isActive }) => `mini-link top-link ${isActive || item.key === currentTopKey ? "is-active" : ""}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="mast-tools">
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
              {sideRoutes.map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) => `module-nav-btn side-route-btn ${isActive ? "is-active" : ""}`}
                >
                  <span className="route-label">{item.label}</span>
                  <span className="route-short" aria-hidden="true">{item.label.slice(0, 2)}</span>
                  {!item.implemented ? (
                    <span className="route-tag route-tag-future">[后续建设]</span>
                  ) : null}
                </NavLink>
              ))}
            </div>
          </aside>
        ) : null}

        {!isMainContentFocus ? (
          <p className="workspace-deck subtle-note">
            {(TOP_MODULES.find((item) => item.key === currentTopKey)?.label || "首页")} &gt; {routeLabel(location.pathname)}
          </p>
        ) : null}

        <main id="mainContent" className="workspace-main" tabIndex={-1}>{children}</main>
      </div>
      <div className="toast-holder" aria-live="polite">
        <div className={`toast ${toast.show ? "show" : ""} ${toast.tone}`}>{toast.message}</div>
      </div>
    </>
  );
}

function PageContainer() {
  return (
    <AppShell>
      <Routes>
        <Route path="/home" element={<HomePage />} />
        <Route path="/knowledge/import" element={<KnowledgePage preset="import" />} />
        <Route path="/knowledge/manual" element={<KnowledgePage preset="manual" />} />
        <Route path="/knowledge/domains" element={<DomainManagementPage />} />
        <Route path="/knowledge/feedback" element={<KnowledgePage preset="feedback" />} />

        <Route path="/assets/map" element={<AssetsPage view="map" />} />
        <Route path="/assets/scenes" element={<AssetsPage view="scenes" />} />
        <Route path="/assets/views" element={<AssetsPage view="views" />} />
        <Route path="/assets/dicts" element={<AssetsPage view="dicts" />} />
        <Route path="/assets/rules" element={<AssetsPage view="rules" />} />
        <Route path="/assets/topics" element={<AssetsPage view="topics" />} />
        <Route path="/assets/services" element={<AssetsPage view="services" />} />
        <Route path="/assets/guide" element={<AssetsPage view="guide" />} />
        <Route path="/assets/market" element={<AssetsPage view="market" />} />
        <Route path="/assets/lineage" element={<AssetsPage view="lineage" />} />
        <Route path="/assets" element={<Navigate to="/assets/map" replace />} />

        <Route path="/workspace/todo" element={<WorkspacePage view="todo" />} />
        <Route path="/workspace/notice" element={<WorkspacePage view="notice" />} />
        <Route path="/workspace/favorites" element={<WorkspacePage view="favorites" />} />
        <Route path="/workspace/recent" element={<WorkspacePage view="recent" />} />

        <Route path="/system/guide" element={<SystemPage view="guide" />} />
        <Route path="/system/llm" element={<SystemPage view="llm" />} />
        <Route path="/system/prompts" element={<SystemPage view="prompts" />} />
        <Route path="/system" element={<Navigate to="/system/guide" replace />} />
        <Route path="/" element={<Navigate to="/home" replace />} />
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
