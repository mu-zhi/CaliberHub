# 监控审计与影响分析 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `11-监控审计与影响分析.md` 从样例页收口成正式工作台能力，提供可追溯的监控摘要、审计时间轴和影响分析入口。

**Architecture:** 后端新增“监控总览 + 审计追踪”只读聚合接口，继续复用现有 `ImpactQueryAppService` 和 `AuditEventMapper` 作为查询底座；前端 `MonitoringAuditPage` 改为消费正式接口而不是硬编码样例数据，并通过既有 `Workbench Context Package（工作台上下文包）` 维持回放跳转。

**Tech Stack:** Spring Boot、Spring Web、Spring Data JPA、React、Vitest、OpenAPI、Vite

---

## 设计输入

- `docs/architecture/features/iteration-02-runtime-and-governance/11-监控审计与影响分析.md`
- `docs/architecture/frontend-workbench-design.md`
- `docs/plans/2026-04-04-feature-doc-coverage-and-gate-audit.md`

## File Map

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/MonitoringAuditController.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/MonitoringOverviewDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/AuditTraceTimelineDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/MonitoringAuditQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/AuditEventMapper.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MonitoringAuditApiIntegrationTest.java`
- Modify: `frontend/src/api/contracts.ts`
- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`
- Create: `frontend/src/pages/MonitoringAuditPage.test.jsx`
- Create: `docs/testing/features/iteration-02-runtime-and-governance/11-监控审计与影响分析-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

## 前置依赖

- 已存在的 `AuditEventMapper`、`GraphAuditEventAppService` 和 `ImpactQueryAppService`
- 已落地的 `workbenchContext.ts` / `workbenchContextReceivers.ts`
- `/v3/api-docs` 作为前端正式契约真源

### Task 1: 先补接口与页面失败测试

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/MonitoringAuditApiIntegrationTest.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiDocumentationIntegrationTest.java`
- Create: `frontend/src/pages/MonitoringAuditPage.test.jsx`

- [ ] **Step 1: 写后端接口失败测试**

在 `MonitoringAuditApiIntegrationTest.java` 中新增两个测试：

```java
@Test
void shouldReturnMonitoringOverview() throws Exception {
    String token = loginAndGetToken("support", "support123");

    mockMvc.perform(get("/api/monitoring/overview").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alerts").isArray())
            .andExpect(jsonPath("$.metrics.sceneHitRate").exists())
            .andExpect(jsonPath("$.traceLinks").isArray());
}

@Test
void shouldReturnAuditTraceTimeline() throws Exception {
    String token = loginAndGetToken("support", "support123");

    mockMvc.perform(get("/api/monitoring/audit-traces/{traceId}", "trace_runtime_20260327_07")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").value("trace_runtime_20260327_07"))
            .andExpect(jsonPath("$.events").isArray());
}
```

- [ ] **Step 2: 把新路径补进 OpenAPI 文档测试**

在 `OpenApiDocumentationIntegrationTest.java` 追加断言：

```java
.andExpect(jsonPath("$.paths['/api/monitoring/overview'].get.tags", hasItem("监控与审计")))
.andExpect(jsonPath("$.paths['/api/monitoring/overview'].get.summary").value("查询监控总览"))
.andExpect(jsonPath("$.paths['/api/monitoring/audit-traces/{traceId}'].get.summary").value("查询审计追踪时间轴"))
```

- [ ] **Step 3: 写前端页面失败测试**

在 `frontend/src/pages/MonitoringAuditPage.test.jsx` 写入：

```jsx
it("renders monitoring overview from formal api instead of hard-coded sample rows", async () => {
  vi.mock("../api/client", () => ({
    apiRequest: vi.fn()
      .mockResolvedValueOnce({
        metrics: { sceneHitRate: "93%", blockedPublishCount: 2, pendingApprovals: 2, graphTimeoutCount: 0 },
        alerts: [{ title: "发布阻断", detail: "字典治理对象缺失", tone: "warn" }],
        traceLinks: [{ traceId: "trace_runtime_20260327_07", approvalTicket: "APR-20260327-001" }],
      })
      .mockResolvedValueOnce({
        traceId: "trace_runtime_20260327_07",
        events: ["请求进入", "场景命中", "策略决策"],
      }),
  }));
});
```

- [ ] **Step 4: 运行测试验证 Red**

Run:

- `cd backend && mvn -q -Dtest=MonitoringAuditApiIntegrationTest,OpenApiDocumentationIntegrationTest test`
- `cd frontend && npm test -- src/pages/MonitoringAuditPage.test.jsx src/pages/WorkbenchContextPages.test.jsx`

Expected:

- 后端因缺少 `/api/monitoring/overview` 和 `/api/monitoring/audit-traces/{traceId}` 失败
- 前端因 `MonitoringAuditPage` 仍使用硬编码数组失败

### Task 2: 实现后端监控总览与审计追踪只读接口

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/MonitoringAuditController.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/MonitoringOverviewDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/AuditTraceTimelineDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/MonitoringAuditQueryAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/module/dao/mapper/graphrag/AuditEventMapper.java`

- [ ] **Step 1: 扩充审计查询能力**

在 `AuditEventMapper.java` 增加聚合查询入口：

```java
List<AuditEventPO> findTop20ByOrderByCreatedAtDesc();
List<AuditEventPO> findTop20ByTraceIdOrderByCreatedAtAsc(String traceId);
long countByStatus(String status);
```

- [ ] **Step 2: 新增监控聚合查询服务**

在 `MonitoringAuditQueryAppService.java` 中组织 DTO：

```java
public MonitoringOverviewDTO loadOverview() {
    List<AuditEventPO> latestEvents = auditEventMapper.findTop20ByOrderByCreatedAtDesc();
    return MonitoringOverviewDTO.from(latestEvents, impactQueryAppService.sceneImpact(...));
}

public AuditTraceTimelineDTO loadTrace(String traceId) {
    return AuditTraceTimelineDTO.from(traceId, auditEventMapper.findTop20ByTraceIdOrderByCreatedAtAsc(traceId));
}
```

- [ ] **Step 3: 暴露正式接口**

在 `MonitoringAuditController.java` 中新增：

```java
@RestController
@RequestMapping("/api/monitoring")
class MonitoringAuditController {
    @GetMapping("/overview")
    ResponseEntity<MonitoringOverviewDTO> overview() { ... }

    @GetMapping("/audit-traces/{traceId}")
    ResponseEntity<AuditTraceTimelineDTO> trace(@PathVariable String traceId) { ... }
}
```

- [ ] **Step 4: 运行后端测试验证 Green**

Run:

- `cd backend && mvn -q -Dtest=MonitoringAuditApiIntegrationTest,OpenApiDocumentationIntegrationTest test`

Expected:

- PASS
- `/v3/api-docs` 出现 2 个新的监控与审计路径

### Task 3: 前端改为消费正式接口并保留上下文回放链路

**Files:**

- Modify: `frontend/src/api/contracts.ts`
- Modify: `frontend/src/pages/MonitoringAuditPage.jsx`
- Modify: `frontend/src/pages/WorkbenchContextPages.test.jsx`
- Create: `frontend/src/pages/MonitoringAuditPage.test.jsx`

- [ ] **Step 1: 补前端契约路径**

在 `API_CONTRACTS` 中新增：

```ts
monitoringOverview: "/api/monitoring/overview",
monitoringAuditTrace: "/api/monitoring/audit-traces",
```

- [ ] **Step 2: 用真实接口替换样例数组**

在 `MonitoringAuditPage.jsx` 中改成：

```jsx
useEffect(() => {
  apiRequest(API_CONTRACTS.monitoringOverview).then(setOverview);
  apiRequest(`${API_CONTRACTS.monitoringAuditTrace}/${DEFAULT_TRACE_ID}`).then(setTraceTimeline);
}, []);
```

并保留：

```jsx
const runtimeReplayHref = buildWorkbenchHref("/runtime", { ... });
const mapReplayHref = buildWorkbenchHref("/map", { ... });
```

- [ ] **Step 3: 跑前端验证**

Run:

- `cd frontend && npm test -- src/pages/MonitoringAuditPage.test.jsx src/pages/WorkbenchContextPages.test.jsx`
- `cd frontend && npm run build`

Expected:

- PASS
- `MonitoringAuditPage` 渲染正式接口返回内容，`/monitoring` 页面仍输出 `ctx` 链接

### Task 4: 收口测试文档与状态真源

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/11-监控审计与影响分析-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 生成测试报告骨架**

在测试文档中至少写清：

```md
- 对应特性文档：`docs/architecture/features/iteration-02-runtime-and-governance/11-监控审计与影响分析.md`
- 对应实施计划：`docs/plans/2026-04-04-monitoring-audit-impact-analysis-implementation-plan.md`
- 关键命令：
  - `cd backend && mvn -q -Dtest=MonitoringAuditApiIntegrationTest,OpenApiDocumentationIntegrationTest test`
  - `cd frontend && npm test -- src/pages/MonitoringAuditPage.test.jsx src/pages/WorkbenchContextPages.test.jsx`
  - `cd frontend && npm run build`
```

- [ ] **Step 2: 更新交付状态**

在 `docs/engineering/current-delivery-status.md` 中把该工作项从计划态推进到实现态，并记录下一动作是“接入真实监控摘要与审计追踪接口”。
