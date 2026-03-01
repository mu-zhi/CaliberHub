export const TOP_MODULES = [
  { key: "home", label: "首页", path: "/home" },
  { key: "knowledge", label: "口径治理", path: "/knowledge/import" },
  { key: "assets", label: "数据地图", path: "/assets/map" },
  { key: "system", label: "系统管理", path: "/system/guide" },
  { key: "workspace", label: "个人中心", path: "/workspace/todo" },
];

function normalizePathname(pathname) {
  const text = `${pathname || "/"}`.trim();
  if (text === "") {
    return "/";
  }
  if (text === "/") {
    return "/";
  }
  return text.replace(/\/+$/, "");
}

function readyRoute(route) {
  return {
    ...route,
    implemented: true,
    statusLabel: "已实现",
    fallbackGoal: "",
    fallbackPhase: "",
    fallbackWorkaround: "",
  };
}

function futureRoute(route) {
  return {
    ...route,
    implemented: false,
    statusLabel: "后续建设",
    fallbackGoal: route.fallbackGoal || "能力正在建设中，暂不可直接使用。",
    fallbackPhase: route.fallbackPhase || "后续建设",
    fallbackWorkaround: route.fallbackWorkaround || "请先使用“数据图谱 / 业务场景 / 血缘分析”完成当前任务。",
  };
}

export const SIDE_ROUTES = [
  readyRoute({ path: "/home", topKey: "home", label: "首页", pageKey: "home" }),

  readyRoute({ path: "/knowledge/import", topKey: "knowledge", label: "导入口径文档", pageKey: "knowledge" }),
  readyRoute({ path: "/knowledge/manual", topKey: "knowledge", label: "手动创建", pageKey: "knowledge" }),
  readyRoute({ path: "/knowledge/domains", topKey: "knowledge", label: "业务域管理", pageKey: "knowledge" }),
  readyRoute({ path: "/knowledge/feedback", topKey: "knowledge", label: "质量反馈", pageKey: "knowledge" }),

  readyRoute({ path: "/assets/map", topKey: "assets", label: "数据图谱", pageKey: "assets", view: "map" }),
  readyRoute({ path: "/assets/scenes", topKey: "assets", label: "业务场景", pageKey: "assets", view: "scenes" }),
  futureRoute({
    path: "/assets/views",
    topKey: "assets",
    label: "语义视图",
    pageKey: "assets",
    view: "views",
    fallbackGoal: "统一展示场景字段级语义视图与口径解释。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先在“业务场景”查看口径描述，并在“数据图谱”中定位来源表与关系。",
  }),
  futureRoute({
    path: "/assets/dicts",
    topKey: "assets",
    label: "字典",
    pageKey: "assets",
    view: "dicts",
    fallbackGoal: "集中维护码值字典与业务释义。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先在“业务场景”中查看码值说明字段，必要时在场景描述补充解释。",
  }),
  futureRoute({
    path: "/assets/rules",
    topKey: "assets",
    label: "派生规则",
    pageKey: "assets",
    view: "rules",
    fallbackGoal: "管理指标与字段派生规则，支持追溯。",
    fallbackPhase: "后续建设（第三阶段）",
    fallbackWorkaround: "先通过“血缘分析”识别上下游影响，在场景的取数方案中记录规则说明。",
  }),
  futureRoute({
    path: "/assets/topics",
    topKey: "assets",
    label: "主题节点",
    pageKey: "assets",
    view: "topics",
    fallbackGoal: "按主题域管理资产节点层级与导航入口。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先在当前“数据图谱”左侧主题树进行主题定位与节点筛选。",
  }),
  futureRoute({
    path: "/assets/services",
    topKey: "assets",
    label: "服务说明",
    pageKey: "assets",
    view: "services",
    fallbackGoal: "面向消费侧提供可复用的数据服务输入输出契约。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先发布业务场景并在“业务场景”详情查看输入输出与取数方案说明。",
  }),
  futureRoute({
    path: "/assets/guide",
    topKey: "assets",
    label: "取数指南",
    pageKey: "assets",
    view: "guide",
    fallbackGoal: "提供面向业务用户的取数步骤与最佳实践指引。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先在“业务场景”查看已发布场景并结合“数据图谱”完成路径判断。",
  }),
  futureRoute({
    path: "/assets/market",
    topKey: "assets",
    label: "场景集市",
    pageKey: "assets",
    view: "market",
    fallbackGoal: "提供场景推荐、检索与复用入口。",
    fallbackPhase: "后续建设（第二阶段）",
    fallbackWorkaround: "先通过首页全局搜索与“业务场景”列表进行查找和复用。",
  }),
  readyRoute({ path: "/assets/lineage", topKey: "assets", label: "血缘分析", pageKey: "assets", view: "lineage" }),

  readyRoute({ path: "/workspace/todo", topKey: "workspace", label: "我的待办", pageKey: "workspace", view: "todo" }),
  readyRoute({ path: "/workspace/notice", topKey: "workspace", label: "通知", pageKey: "workspace", view: "notice" }),
  readyRoute({ path: "/workspace/favorites", topKey: "workspace", label: "收藏", pageKey: "workspace", view: "favorites" }),
  readyRoute({ path: "/workspace/recent", topKey: "workspace", label: "最近浏览", pageKey: "workspace", view: "recent" }),

  readyRoute({ path: "/system/guide", topKey: "system", label: "系统介绍", pageKey: "system", view: "guide" }),
  readyRoute({ path: "/system/llm", topKey: "system", label: "大模型配置", pageKey: "system", view: "llm" }),
  readyRoute({ path: "/system/prompts", topKey: "system", label: "预处理提示词", pageKey: "system", view: "prompts" }),
];

export function findRoute(pathname) {
  const normalizedPath = normalizePathname(pathname);
  if (normalizedPath === "/assets") {
    return SIDE_ROUTES.find((item) => item.path === "/assets/map") || SIDE_ROUTES[0];
  }
  if (normalizedPath === "/system") {
    return SIDE_ROUTES.find((item) => item.path === "/system/guide") || SIDE_ROUTES[0];
  }
  return SIDE_ROUTES.find((item) => item.path === normalizedPath) || SIDE_ROUTES[0];
}

export function routesByTop(topKey, isAdmin) {
  return SIDE_ROUTES.filter((item) => item.topKey === topKey && (!item.adminOnly || isAdmin));
}

export function isAdminRole(role) {
  return role === "admin";
}
