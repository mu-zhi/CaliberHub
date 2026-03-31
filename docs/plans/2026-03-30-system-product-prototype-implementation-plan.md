# 全系统产品化升级原型 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前 7 个一级工作台整理为 14 张桌面端高保真静态 `Figma（设计协作工具，Figma）` 产品原型页面，用于业务演示与产品评审。

**Architecture:** 以现有前端页面和路由为真实信息架构来源，先建立统一 `Figma` 文件骨架与全局壳层，再按模块分批生成“入口页 + 代表深页”，最后通过截图自检统一校对页面层级、状态摘要条和页面命名。项目正式规则继续回写 `docs/architecture/frontend-workbench-design.md`，执行进度统一回写 `docs/engineering/current-delivery-status.md`。

**Tech Stack:** React 18、React Router、现有前端页面源码、Markdown 文档体系、`Figma` 插件工作流（`figma-create-new-file`、`figma-use`、`figma-generate-design`）

---

## Design Inputs

- `docs/architecture/frontend-workbench-design.md`
- `frontend/src/routes.js`
- `frontend/src/App.jsx`
- `frontend/src/pages/HomePage.jsx`
- `frontend/src/pages/AssetsPage.jsx`
- `frontend/src/pages/KnowledgePage.jsx`
- `frontend/src/pages/PublishCenterPage.jsx`
- `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- `frontend/src/pages/ApprovalExportPage.jsx`
- `frontend/src/pages/MonitoringAuditPage.jsx`

## File Map

- Create: `docs/plans/2026-03-30-system-product-prototype-implementation-plan.md`
- Modify: `docs/engineering/current-delivery-status.md`
- Reference: `docs/architecture/frontend-workbench-design.md`
- Reference: `frontend/src/routes.js`
- Reference: `frontend/src/App.jsx`
- Reference: `frontend/src/pages/HomePage.jsx`
- Reference: `frontend/src/pages/AssetsPage.jsx`
- Reference: `frontend/src/pages/KnowledgePage.jsx`
- Reference: `frontend/src/pages/PublishCenterPage.jsx`
- Reference: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Reference: `frontend/src/pages/ApprovalExportPage.jsx`
- Reference: `frontend/src/pages/MonitoringAuditPage.jsx`
- External Deliverable: `Figma` 文件 `数据直通车-全系统产品化升级原型`

## Scope

- 覆盖 14 张页面：7 张模块入口页 + 7 张代表深页。
- 只交付桌面端 `1440` 宽度高保真静态原型。
- 保留现有信息架构与业务语义，升级视觉层级、页面骨架和演示口径。
- 不在本轮交付点击连线、移动端适配和新业务流程。

### Task 1: 建立 Figma 文件骨架与全局壳层

**Files:**

- Reference: `docs/architecture/frontend-workbench-design.md`
- Reference: `frontend/src/App.jsx`
- Reference: `frontend/src/routes.js`
- External Deliverable: `Figma` 文件 `数据直通车-全系统产品化升级原型`

- [ ] **Step 1: 创建新的 Figma 设计文件**

在 `Figma` 插件中创建一个新的 `design（设计稿）` 文件，文件名固定为 `数据直通车-全系统产品化升级原型`。

Expected:

1. 新文件创建成功。
2. 可获得 `file_key（文件键）` 或等价文件链接。
3. 文件初始状态为空白设计文件，不使用 `FigJam（协作白板，FigJam）`。

- [ ] **Step 2: 建立 10 个固定页面分区**

在新文件中创建以下页面，并按顺序排列：

```text
00 Cover
01 Principles
02 Global Shell
03 Overview
04 Map
05 Production
06 Publish
07 Runtime
08 Approval
09 Monitoring
```

Expected:

1. 页面名称与顺序完全一致。
2. 不额外创建“草稿”“临时页”“测试页”等并行页面。
3. 顶部壳层、状态摘要条、通用右侧信息面板先统一放到 `02 Global Shell`。

- [ ] **Step 3: 在 `02 Global Shell` 中先做三套全局骨架**

在 `02 Global Shell` 中创建以下静态骨架：

1. 顶部横向轨道：品牌、7 个一级模块、全局工具区、当前角色。
2. 模块页头：模块标题、副说明、状态摘要条、主动作区。
3. 右侧判断区：风险、证据、建议动作的统一信息面板。

Expected:

1. 颜色语言收口为深蓝灰导航轨、冷白工作面、青绿色健康态、琥珀色待处理态、红色阻断态。
2. 组件命名采用中文，不出现“final”“v2”“new shell”一类过程性命名。
3. 三套骨架可以直接复用于后续 14 张页面。

### Task 2: 生成首页总览与数据地图模块原型

**Files:**

- Reference: `frontend/src/pages/HomePage.jsx`
- Reference: `frontend/src/pages/AssetsPage.jsx`
- Reference: `frontend/src/routes.js`
- External Deliverable: `03 Overview`
- External Deliverable: `04 Map`

- [ ] **Step 1: 生成首页总览 2 张页面**

在 `03 Overview` 中生成以下页面：

1. `首页总览 / 指挥页 / Default`
2. `首页总览 / 风险聚焦 / Filtered`

页面必须突出：

1. 今日待处理、候选发布、运行异常、审批积压和最近活跃场景。
2. 风险聚焦态中的阻断发布、高敏申请、历史覆盖缺口和告警串联。
3. 首页是系统级指挥页，不退回门户页或营销首屏。

- [ ] **Step 2: 生成数据地图 2 张页面**

在 `04 Map` 中生成以下页面：

1. `数据地图 / 模块入口 / Default`
2. `数据地图 / 资产图谱 / Default`

页面必须突出：

1. 模块入口页先建立“从哪里进入知识底座”的认知，再给出推荐入口。
2. 资产图谱页以图为主舞台，右侧固定展示节点摘要、上下游影响、来源证据和建议动作。
3. 图谱是主角色，但页面仍保持产品工作台表达，不退回研发调试画布。

- [ ] **Step 3: 对首页与数据地图执行截图自检**

逐页检查以下项目：

1. 顶部横向轨道是否统一。
2. 状态摘要条是否始终位于页头以下。
3. 图谱、指标卡、风险标签是否存在多个主焦点竞争。
4. 页面标题、模块名和状态标签是否与 `docs/architecture/frontend-workbench-design.md` 一致。

Expected:

1. `03 Overview` 与 `04 Map` 页面可直接用于业务演示截图。
2. 不存在大段研发说明文压住主任务区域。
3. 页面命名与模块顺序保持统一。

### Task 3: 生成知识生产台、发布中心与运行决策台模块原型

**Files:**

- Reference: `frontend/src/pages/KnowledgePage.jsx`
- Reference: `frontend/src/pages/PublishCenterPage.jsx`
- Reference: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- External Deliverable: `05 Production`
- External Deliverable: `06 Publish`
- External Deliverable: `07 Runtime`

- [ ] **Step 1: 生成知识生产台 2 张页面**

在 `05 Production` 中生成以下页面：

1. `知识生产台 / 模块入口 / Default`
2. `知识生产台 / 材料接入与解析 / Default`

页面必须突出：

1. 模块入口页按“接入任务、待校正材料、领域配置、最近处理记录”组织。
2. 材料接入与解析页采用单条主链路，左侧是接入与解析主流程，右侧是模板规范、识别结果和风险提示。
3. 页面目标是回答“这份材料能不能进入建模”，而不是完整暴露所有技术细节。

- [ ] **Step 2: 生成发布中心 2 张页面**

在 `06 Publish` 中生成以下页面：

1. `发布中心 / 模块入口 / Default`
2. `发布中心 / 发布检查与冻结快照 / Default`

页面必须突出：

1. 模块入口页先回答“哪些东西能发，哪些东西不能发”。
2. 深页以“是否允许发布”的判断面板为重心，把冻结快照、覆盖声明、策略对象和契约兑现作为证据区。
3. 阻断项必须前置，不允许藏在列表深处。

- [ ] **Step 3: 生成运行决策台 2 张页面**

在 `07 Runtime` 中生成以下页面：

1. `运行决策台 / 模块入口 / Default`
2. `运行决策台 / 查询验证与知识包解释 / Default`

页面必须突出：

1. 模块入口页展示可运行场景、最近验证任务、常用查询模板和风险提示。
2. 深页同屏展示输入条件、结果摘要、命中策略、覆盖判断、知识包解释和字段级限制。
3. 页面目标是回答“为什么给出这个结果、边界在哪里”。

### Task 4: 生成审批与导出、监控与审计模块原型

**Files:**

- Reference: `frontend/src/pages/ApprovalExportPage.jsx`
- Reference: `frontend/src/pages/MonitoringAuditPage.jsx`
- External Deliverable: `08 Approval`
- External Deliverable: `09 Monitoring`

- [ ] **Step 1: 生成审批与导出 2 张页面**

在 `08 Approval` 中生成以下页面：

1. `审批与导出 / 模块入口 / Default`
2. `审批与导出 / 审批详情与导出决策 / Default`

页面必须突出：

1. 模块入口页先展示待审批、高敏申请、导出排队和归档记录。
2. 深页围绕单个申请组织，展示申请目的、命中策略、字段风险、机器建议和审批动作。
3. 审批动作区必须可见，但本轮不做点击连线。

- [ ] **Step 2: 生成监控与审计 2 张页面**

在 `09 Monitoring` 中生成以下页面：

1. `监控与审计 / 模块入口 / Default`
2. `监控与审计 / 链路回放与审计检索 / Default`

页面必须突出：

1. 模块入口页展示运行健康、异常告警、发布通过率、审批积压和可回放链路入口。
2. 深页顶部放检索，中部放事件时间线，右侧放风险结论、关联审批单和跳转入口。
3. 页面目标是回答“当时发生了什么，为什么这样判断”。

- [ ] **Step 3: 对 14 张页面执行统一截图走查**

逐模块检查以下问题：

1. 是否都沿用了统一的顶部横向轨道和模块页头。
2. 是否出现“模块入口页像首页、深页像研发内测页”的风格割裂。
3. 指标卡、告警卡、右侧判断区和图谱区是否保持主次分层。
4. 页面顶部说明是否都包含“页面目标、主动作、评审重点”。

Expected:

1. 14 张页面形成统一产品家族，不像多个独立项目拼接。
2. 所有页面都可独立截图进入评审材料。
3. 深页与入口页的视觉语法一致，但任务重心明确不同。

### Task 5: 收口交付状态与阻塞信息

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 回写计划已落地与原型页集已确认**

在 `docs/engineering/current-delivery-status.md` 中新增或更新“全系统产品化升级原型（Figma）”工作项，至少包含以下信息：

1. 来源设计为 `docs/architecture/frontend-workbench-design.md`
2. 来源计划为 `docs/plans/2026-03-30-system-product-prototype-implementation-plan.md`
3. 当前状态为 `implementing（实现中）` 或真实阻塞态
4. 最新完成写明“14 张页面范围、视觉方向、Figma 文件结构已确认并回写主文档”

- [ ] **Step 2: 若 Figma 插件不可用，立即显式写入阻塞**

若执行时发现当前会话没有可用的 `Figma` 写入能力，则在交付状态中显式写明：

1. 阻塞项为“当前会话未暴露 `Figma` 写入工具或插件能力”
2. 下一动作是“在可用 `Figma` MCP 会话中继续按本计划生成 14 张页面”
3. 已完成项仍保留文档回写与计划落地，不回退

- [ ] **Step 3: 完成后准备进入分支收尾**

Expected:

1. 若 `Figma` 文件已生成完成，则工作项可进入 `reviewing（评审验证中）` 或同等原型验收状态。
2. 若 `Figma` 能力不可用，则工作项保持 `blocked（阻塞中）`，但文档和计划必须已是最新。
3. 无论是否阻塞，`docs/engineering/current-delivery-status.md` 都能作为当前唯一真源继续接力。
