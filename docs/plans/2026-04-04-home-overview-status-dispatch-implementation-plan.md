# 首页总览与状态分发 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `12a-首页总览与状态分发.md` 从静态首页收口成正式的状态聚合与续办分发页，提供真实的首页聚合接口、继续处理上下文恢复和首屏风险 / 待办分发。

**Architecture:** 后端新增首页总览只读聚合接口，统一返回状态卡、重点风险、我的待办和最近处理中对象；前端 `HomePage` 改为消费正式聚合接口与 `Workbench Context Package（工作台上下文包）`，不再直接从场景列表反推首页指标。

**Tech Stack:** Spring Boot、React、React Router、Vitest、OpenAPI、Vite

---

## 设计输入

- `docs/architecture/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发.md`
- `docs/plans/2026-03-30-home-overview-hero-copy-trim-implementation-plan.md`
- `docs/plans/2026-04-04-feature-doc-coverage-and-gate-audit.md`

## File Map

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/HomeOverviewController.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/HomeOverviewDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/HomeOverviewQueryAppService.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/HomeOverviewApiIntegrationTest.java`
- Modify: `frontend/src/api/contracts.ts`
- Modify: `frontend/src/pages/HomePage.jsx`
- Modify: `frontend/src/pages/HomePage.test.jsx`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

## 前置依赖

- 已有 `buildWorkbenchHref` 与 `readValidatedWorkbenchContext`
- 已有首页渲染与 Hero 文案减重测试
- 现有 `HomePage.jsx` 已具备卡片结构，可承接正式数据

### Task 1: 先补首页聚合接口和前端失败测试

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/HomeOverviewApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
- Modify: `frontend/src/pages/HomePage.test.jsx`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`

- [ ] **Step 1: 写后端首页聚合接口失败测试**

在 `HomeOverviewApiIntegrationTest.java` 中新增：

```java
@Test
void shouldReturnHomeOverviewAggregate() throws Exception {
    String token = loginAndGetToken("support", "support123");

    mockMvc.perform(get("/api/home/overview").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCards").isArray())
            .andExpect(jsonPath("$.riskItems").isArray())
            .andExpect(jsonPath("$.todoItems").isArray())
            .andExpect(jsonPath("$.resumeItem").exists());
}
```

- [ ] **Step 2: 在 OpenAPI 文档测试中追加首页路径**

追加断言：

```java
.andExpect(jsonPath("$.paths['/api/home/overview'].get.tags", hasItem("首页总览")))
.andExpect(jsonPath("$.paths['/api/home/overview'].get.summary").value("查询首页总览聚合数据"))
```

- [ ] **Step 3: 把首页测试改成依赖正式聚合接口**

在 `HomePage.test.jsx` 中把 mock 合同改成：

```jsx
vi.mock("../api/contracts", () => ({
  API_CONTRACTS: {
    homeOverview: "/home/overview",
  },
}));
```

并断言：

```jsx
expect(apiRequest).toHaveBeenCalledWith(API_CONTRACTS.homeOverview);
```

- [ ] **Step 4: 运行测试验证 Red**

Run:

- `cd backend && mvn -q -Dtest=HomeOverviewApiIntegrationTest,OpenApiDocumentationIntegrationTest test`
- `cd frontend && npm test -- src/pages/HomePage.test.jsx src/pages/WorkbenchPages.render.test.jsx`

Expected:

- 后端因缺少 `/api/home/overview` 失败
- 前端因 `HomePage` 仍请求 `API_CONTRACTS.scenes` 失败

### Task 2: 实现首页聚合接口

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/HomeOverviewController.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/HomeOverviewDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/HomeOverviewQueryAppService.java`

- [ ] **Step 1: 定义首页聚合 DTO**

在 `HomeOverviewDTO.java` 中定义最小结构：

```java
public record HomeOverviewDTO(
        List<StatusCardDTO> statusCards,
        List<RiskItemDTO> riskItems,
        List<TodoItemDTO> todoItems,
        ResumeItemDTO resumeItem,
        String updatedAt
) {}
```

- [ ] **Step 2: 新增首页聚合查询服务**

在 `HomeOverviewQueryAppService.java` 中组合首页数据：

```java
public HomeOverviewDTO load() {
    return new HomeOverviewDTO(
        buildStatusCards(),
        buildRiskItems(),
        buildTodoItems(),
        buildResumeItem(),
        OffsetDateTime.now().toString()
    );
}
```

- [ ] **Step 3: 暴露正式接口**

在 `HomeOverviewController.java` 中新增：

```java
@RestController
@RequestMapping("/api/home")
class HomeOverviewController {
    @GetMapping("/overview")
    ResponseEntity<HomeOverviewDTO> overview() {
        return ResponseEntity.ok(homeOverviewQueryAppService.load());
    }
}
```

- [ ] **Step 4: 运行后端验证**

Run:

- `cd backend && mvn -q -Dtest=HomeOverviewApiIntegrationTest,OpenApiDocumentationIntegrationTest test`

Expected:

- PASS
- `/v3/api-docs` 出现首页总览聚合接口

### Task 3: 首页切换到正式聚合数据与续办上下文

**Files:**

- Modify: `frontend/src/api/contracts.ts`
- Modify: `frontend/src/pages/HomePage.jsx`
- Modify: `frontend/src/pages/HomePage.test.jsx`
- Modify: `frontend/src/pages/WorkbenchPages.render.test.jsx`

- [ ] **Step 1: 补前端契约路径**

在 `API_CONTRACTS` 中新增：

```ts
homeOverview: "/api/home/overview",
```

- [ ] **Step 2: 用首页聚合结果替换场景列表反推**

在 `HomePage.jsx` 中调整加载逻辑：

```jsx
const result = await apiRequest(API_CONTRACTS.homeOverview);
setOverview(result);
```

并把“继续处理”入口改成基于接口返回的 `resumeItem.ctx`：

```jsx
const resumeHref = buildWorkbenchHref(result.resumeItem.path, result.resumeItem.ctx);
```

- [ ] **Step 3: 跑前端验证**

Run:

- `cd frontend && npm test -- src/pages/HomePage.test.jsx src/pages/WorkbenchPages.render.test.jsx`
- `cd frontend && npm run build`

Expected:

- PASS
- 首页状态卡、风险区、待办区和续办入口来自正式聚合结果

### Task 4: 收口测试报告与状态真源

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 生成首页测试报告骨架**

至少写入：

```md
- 对应特性文档：`docs/architecture/features/iteration-02-runtime-and-governance/12a-首页总览与状态分发.md`
- 对应实施计划：`docs/plans/2026-04-04-home-overview-status-dispatch-implementation-plan.md`
- 验证命令：
  - `cd backend && mvn -q -Dtest=HomeOverviewApiIntegrationTest,OpenApiDocumentationIntegrationTest test`
  - `cd frontend && npm test -- src/pages/HomePage.test.jsx src/pages/WorkbenchPages.render.test.jsx`
  - `cd frontend && npm run build`
```

- [ ] **Step 2: 更新交付状态**

在 `docs/engineering/current-delivery-status.md` 中记录首页从“文案减重”升级为“正式首页聚合与续办分发”专项计划，下一动作是接入真实聚合接口与续办上下文。
