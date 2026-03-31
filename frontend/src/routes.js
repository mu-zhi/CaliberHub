export const TOP_MODULES = [
  { key: "overview", label: "首页总览", path: "/overview" },
  { key: "map", label: "数据地图", path: "/map" },
  { key: "production", label: "知识生产台", path: "/production" },
  { key: "publish", label: "发布中心", path: "/publish" },
  { key: "runtime", label: "运行决策台", path: "/runtime" },
  { key: "approval", label: "审批与导出", path: "/approval" },
  { key: "monitoring", label: "监控与审计", path: "/monitoring" },
  { key: "tools", label: "全局工具区", path: "/workspace/todo", inTopNav: false },
  { key: "prototype", label: "原型工作台", path: "/prototype", inTopNav: false },
];

const ALWAYS_ACCESSIBLE_TOP_KEYS = new Set(["overview", "tools", "prototype"]);

const ROLE_TOP_MODULE_ACCESS = {
  admin: new Set(["map", "production", "publish", "runtime", "approval", "monitoring"]),
  support: new Set(["map", "runtime", "monitoring"]),
  expert: new Set(["production", "publish", "runtime", "monitoring"]),
  governance: new Set(["map", "production", "publish", "runtime", "monitoring"]),
  frontline: new Set(["map", "runtime"]),
  compliance: new Set(["map", "runtime", "approval", "monitoring"]),
};

export const LEGACY_ROUTE_REDIRECTS = {
  "/home": "/overview",
  "/knowledge": "/production/ingest",
  "/knowledge/import": "/production/ingest",
  "/knowledge/manual": "/production/modeling",
  "/knowledge/domains": "/production/domains",
  "/knowledge/feedback": "/production/feedback",
  "/assets": "/map",
  "/assets/map": "/map",
  "/assets/scenes": "/map/scenes",
  "/assets/lineage": "/map/lineage",
  "/assets/knowledge-package": "/runtime",
  "/assets/views": "/map/views",
  "/assets/dicts": "/map/dicts",
  "/assets/rules": "/map/rules",
  "/assets/topics": "/map/topics",
  "/assets/services": "/map/services",
  "/assets/guide": "/map/guide",
  "/assets/market": "/map/market",
  "/workspace": "/workspace/todo",
  "/system": "/system/guide",
};

function normalizePathname(pathname) {
  const text = `${pathname || "/"}`.trim();
  if (!text || text === "/") {
    return "/";
  }
  return text.replace(/\/+$/, "");
}

function buildRoute(route, config = {}) {
  const maturity = config.maturity || "implemented";
  return {
    ...route,
    implemented: maturity !== "future",
    maturity,
    statusLabel: config.statusLabel || "已实现",
    fallbackGoal: config.fallbackGoal || "",
    fallbackPhase: config.fallbackPhase || "",
    fallbackWorkaround: config.fallbackWorkaround || "",
  };
}

function readyRoute(route) {
  return buildRoute(route);
}

function sampleRoute(route) {
  return buildRoute(route, {
    maturity: "sample",
    statusLabel: "样例数据",
  });
}

function prototypeRoute(route) {
  return buildRoute(route, {
    maturity: "prototype",
    statusLabel: "原型评审",
  });
}

function futureRoute(route) {
  return buildRoute(route, {
    maturity: "future",
    statusLabel: "后续建设",
    fallbackGoal: route.fallbackGoal || "能力正在建设中，暂不可直接使用。",
    fallbackPhase: route.fallbackPhase || "后续建设",
    fallbackWorkaround: route.fallbackWorkaround || "请先使用当前工作台的已实现入口完成任务。",
  });
}

export const SIDE_ROUTES = [
  readyRoute({ path: "/overview", topKey: "overview", label: "首页总览", pageKey: "overview" }),

  readyRoute({ path: "/map", topKey: "map", label: "数据地图", pageKey: "map", view: "map" }),
  readyRoute({ path: "/map/scenes", topKey: "map", label: "业务场景", pageKey: "map", view: "scenes" }),
  readyRoute({ path: "/map/lineage", topKey: "map", label: "资产图谱", pageKey: "map", view: "lineage" }),
  futureRoute({
    path: "/map/views",
    topKey: "map",
    label: "语义视图",
    pageKey: "map",
    view: "views",
    fallbackGoal: "统一展示场景字段级语义视图与口径解释。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先在“数据地图”和“业务场景”里查看结构关系与口径说明。",
  }),
  futureRoute({
    path: "/map/dicts",
    topKey: "map",
    label: "字典",
    pageKey: "map",
    view: "dicts",
    fallbackGoal: "集中维护码值字典与业务释义。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先在“业务场景”中查看码值说明字段。",
  }),
  futureRoute({
    path: "/map/rules",
    topKey: "map",
    label: "派生规则",
    pageKey: "map",
    view: "rules",
    fallbackGoal: "管理指标与字段派生规则，支持追溯。",
    fallbackPhase: "后续建设（第三阶段）",
    fallbackWorkaround: "先通过“资产图谱”识别上下游影响。",
  }),

  readyRoute({ path: "/production", topKey: "production", label: "知识生产台", pageKey: "production" }),
  readyRoute({ path: "/production/ingest", topKey: "production", label: "材料接入与解析", pageKey: "production" }),
  readyRoute({ path: "/production/modeling", topKey: "production", label: "资产建模", pageKey: "production" }),
  readyRoute({ path: "/production/domains", topKey: "production", label: "领域配置", pageKey: "production" }),
  futureRoute({
    path: "/production/feedback",
    topKey: "production",
    label: "质量反馈",
    pageKey: "production",
    fallbackGoal: "集中回收建模质量反馈与回退建议。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "当前请先在资产建模页完成校正后再提交发布检查。",
  }),

  readyRoute({ path: "/publish", topKey: "publish", label: "发布中心", pageKey: "publish" }),
  readyRoute({ path: "/runtime", topKey: "runtime", label: "运行决策台", pageKey: "runtime" }),
  sampleRoute({ path: "/approval", topKey: "approval", label: "审批与导出", pageKey: "approval" }),
  sampleRoute({ path: "/monitoring", topKey: "monitoring", label: "监控与审计", pageKey: "monitoring" }),

  readyRoute({ path: "/workspace/todo", topKey: "tools", label: "个人协作", pageKey: "tools", view: "todo" }),
  readyRoute({ path: "/workspace/notice", topKey: "tools", label: "通知", pageKey: "tools", view: "notice" }),
  readyRoute({ path: "/workspace/favorites", topKey: "tools", label: "收藏", pageKey: "tools", view: "favorites" }),
  readyRoute({ path: "/workspace/recent", topKey: "tools", label: "最近浏览", pageKey: "tools", view: "recent" }),

  readyRoute({ path: "/system/guide", topKey: "tools", label: "系统设置", pageKey: "tools", view: "guide" }),
  readyRoute({ path: "/system/llm", topKey: "tools", label: "大模型配置", pageKey: "tools", view: "llm" }),
  readyRoute({ path: "/system/prompts", topKey: "tools", label: "预处理提示词", pageKey: "tools", view: "prompts" }),

  prototypeRoute({ path: "/prototype", topKey: "prototype", label: "原型入口", pageKey: "prototype" }),
  prototypeRoute({ path: "/prototype/ingest-graph", topKey: "prototype", label: "原型：材料接入与图谱构建", pageKey: "prototype" }),
  prototypeRoute({ path: "/prototype/modeling-dual-pane", topKey: "prototype", label: "原型：双栏校正与资产建模", pageKey: "prototype" }),
  prototypeRoute({ path: "/prototype/runtime-publish", topKey: "prototype", label: "原型：运行验证与发布检查", pageKey: "prototype" }),
];

export function isTopModuleAccessible(topKey, role = "admin") {
  if (ALWAYS_ACCESSIBLE_TOP_KEYS.has(topKey)) {
    return true;
  }
  if (role === "admin") {
    return true;
  }
  return ROLE_TOP_MODULE_ACCESS[role]?.has(topKey) || false;
}

export function findRoute(pathname) {
  const normalizedPath = normalizePathname(pathname);
  if (normalizedPath === "/") {
    return SIDE_ROUTES.find((item) => item.path === "/overview") || SIDE_ROUTES[0];
  }
  const routedPath = LEGACY_ROUTE_REDIRECTS[normalizedPath] || normalizedPath;
  return SIDE_ROUTES.find((item) => item.path === routedPath) || SIDE_ROUTES[0];
}

export function routesByTop(topKey, isAdmin, role = "admin") {
  if (!isTopModuleAccessible(topKey, role)) {
    return [];
  }
  return SIDE_ROUTES.filter((item) => item.topKey === topKey && (!item.adminOnly || isAdmin));
}

export function isAdminRole(role) {
  return role === "admin";
}
