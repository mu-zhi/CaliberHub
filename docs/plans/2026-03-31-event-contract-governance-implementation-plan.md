# 统一接口与事件契约补强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Feature Doc:** `docs/architecture/features/iteration-02-runtime-and-governance/09b-统一接口与事件契约.md`

**Goal:** 在现有 `OpenAPI（开放接口描述规范，OpenAPI Specification）` 文档质量治理之外，补齐正式错误码 / 决策码注册、异步事件顺序契约、幂等去重规则与前端消费映射，避免运行决策、审批导出和监控审计继续各自维护私有码值和事件语义。

**Scope:** 当前轮只覆盖统一契约治理主线，不触碰单个业务场景字段设计；重点收口四块能力：同步接口公共返回块、错误码 / 决策码注册基线、异步事件契约、前端统一消费映射。

**Preconditions:**

1. 以 `docs/architecture/features/iteration-02-runtime-and-governance/09b-统一接口与事件契约.md` 为唯一特性真源。
2. 已有 `docs/plans/2026-03-30-openapi-doc-quality-implementation-plan.md` 继续承接 Swagger / OpenAPI 可读性；本计划只承接其未覆盖的码值注册、事件契约和消费门禁。
3. 当前轮先补测试骨架、注册表与契约断言，再补运行与前端消费接线，不允许先改接口再回补约束。

**Task Split:** 先测后改，按“注册表 -> 事件 DTO 与顺序 -> 接口公共块 -> 前端消费映射 -> 联调验收”推进。

---

## Task 1: 固化错误码、决策码与事件契约测试骨架

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/09b-统一接口与事件契约-测试报告.md`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/contract/ApiCodeRegistryContractTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/contract/AsyncEventContractTest.java`
- Create: `frontend/src/shared/__tests__/contract-status-map.test.js`

- [ ] **Step 1: 新建测试文档骨架**

写入至少以下章节：

```md
# 统一接口与事件契约测试报告

## 1. 测试范围
- 错误码注册表
- 决策码注册表
- 异步事件顺序与最小载荷
- 同步接口公共返回块
- 前端状态 / 错误映射

## 2. 用例清单
- [ ] ApiCodeRegistryContractTest
- [ ] AsyncEventContractTest
- [ ] contract-status-map.test.js
```

- [ ] **Step 2: 先写失败的后端契约测试**

`ApiCodeRegistryContractTest.java` 至少断言：

```java
@Test
void shouldExposeRegisteredErrorCodesOnly() {
    assertThat(ApiErrorCodeRegistry.all())
            .extracting(ApiErrorCode::code)
            .contains("SCENE_NOT_FOUND", "POLICY_DENIED", "PUBLISH_BLOCKED");
}

@Test
void shouldExposeRegisteredDecisionCodesOnly() {
    assertThat(DecisionCodeRegistry.all())
            .extracting(DecisionCode::code)
            .contains("allow", "need_approval", "deny", "clarification", "partial");
}
```

`AsyncEventContractTest.java` 至少断言：

```java
@Test
void shouldKeepStartStageDoneErrorAsFormalEventSequence() {
    assertThat(ImportTaskEventType.orderedFormalTypes())
            .containsExactly("start", "stage", "done", "error");
}
```

- [ ] **Step 3: 先写失败的前端映射测试**

`contract-status-map.test.js` 至少断言：

```js
it("maps registered decision codes to Chinese labels", () => {
  expect(getDecisionLabel("need_approval")).toBe("需要审批");
  expect(getDecisionLabel("deny")).toBe("拒绝返回");
});
```

- [ ] **Step 4: 运行红灯测试**

执行命令：

- `cd backend && mvn -q -Dtest=ApiCodeRegistryContractTest,AsyncEventContractTest test`
- `cd frontend && npm test -- src/shared/__tests__/contract-status-map.test.js`

预期输出：

- 后端测试失败，提示注册表或事件顺序定义不存在 / 不完整。
- 前端测试失败，提示统一映射层不存在或缺少目标码值。

## Task 2: 补齐后端错误码 / 决策码注册表与同步接口公共块

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/contract/ApiErrorCodeRegistry.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/contract/DecisionCodeRegistry.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/CommonResponseMetaDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/advice/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/graphrag/KnowledgePackageDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/export/ExportTaskDTO.java`

- [ ] **Step 1: 引入正式注册表对象**

`ApiErrorCodeRegistry.java` 建议形态：

```java
public final class ApiErrorCodeRegistry {
    public static List<ApiErrorCode> all() { ... }
    public static Optional<ApiErrorCode> find(String code) { ... }
}
```

`DecisionCodeRegistry.java` 建议形态：

```java
public final class DecisionCodeRegistry {
    public static List<DecisionCode> all() { ... }
    public static Optional<DecisionCode> find(String code) { ... }
}
```

- [ ] **Step 2: 把公共返回块收口成可复用 DTO**

同步接口统一返回至少包含：

```java
public record CommonResponseMetaDTO(
        String traceId,
        Long snapshotId,
        Long inferenceSnapshotId,
        String errorCode,
        String decisionCode
) {}
```

- [ ] **Step 3: 让关键 DTO 明确挂公共块**

例如：

```java
public record KnowledgePackageDTO(
        CommonResponseMetaDTO meta,
        KnowledgePackageSceneDTO scene,
        ...
) {}
```

- [ ] **Step 4: 复跑后端契约测试**

执行命令：

- `cd backend && mvn -q -Dtest=ApiCodeRegistryContractTest,AsyncEventContractTest test`

预期输出：

- 注册表断言转绿；如事件契约仍未接好，则只剩事件相关断言失败。

## Task 3: 补齐异步事件 DTO、顺序与幂等去重边界

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/contract/AsyncEventTypeRegistry.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/event/BaseTaskEventDTO.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/event/ImportTaskEventPublisher.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/sse/ImportTaskSseAdapter.java`
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/ImportTaskStreamIntegrationTest.java`

- [ ] **Step 1: 明确正式事件类型与顺序**

至少定义：

```java
public enum AsyncEventType {
    START,
    STAGE,
    DONE,
    ERROR
}
```

并在注册表中固化顺序、是否终止、是否允许重放。

- [ ] **Step 2: 在事件 DTO 中固化最小载荷**

```java
public record BaseTaskEventDTO(
        String eventType,
        String taskType,
        String taskId,
        String eventId,
        String idempotencyKey,
        Instant occurredAt,
        String status,
        String reasonCode
) {}
```

- [ ] **Step 3: 在流式输出适配层补幂等字段**

要求 `ImportTaskSseAdapter` 输出的正式事件同时带 `eventId` 与 `idempotencyKey`，前端可以安全去重。

- [ ] **Step 4: 运行事件链测试**

执行命令：

- `cd backend && mvn -q -Dtest=AsyncEventContractTest,ImportTaskStreamIntegrationTest test`

预期输出：

- 事件顺序、最小载荷和去重键断言通过。

## Task 4: 前端统一消费错误码 / 决策码 / 事件状态

**Files:**

- Create: `frontend/src/shared/contract/statusMaps.js`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/ApprovalWorkbenchPage.jsx`
- Modify: `frontend/src/pages/MonitoringWorkbenchPage.jsx`
- Modify: `frontend/src/pages/knowledge-import-utils.js`

- [ ] **Step 1: 提供统一中文映射层**

`statusMaps.js` 至少导出：

```js
export function getDecisionLabel(code) { ... }
export function getErrorMessage(code) { ... }
export function getEventStatusLabel(code) { ... }
```

- [ ] **Step 2: 替换页面内散落判断**

运行决策、审批与导入页统一消费映射层，不再在页面里直接硬编码英文枚举判断后展示中文。

- [ ] **Step 3: 复跑前端测试**

执行命令：

- `cd frontend && npm test -- src/shared/__tests__/contract-status-map.test.js`

预期输出：

- 映射测试通过，页面不再依赖散落码值字面量。

## Task 5: 联调契约探活与交付收口

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 追加接口 / 事件联调验证**

执行命令：

- `cd backend && mvn -q -Dtest=ApiCodeRegistryContractTest,AsyncEventContractTest,ImportTaskStreamIntegrationTest test`
- `cd backend && mvn -q -DskipTests package`
- `curl -sSf http://127.0.0.1:8082/v3/api-docs | jq '.paths | length'`
- `cd frontend && npm test -- src/shared/__tests__/contract-status-map.test.js`

预期输出：

- 后端契约测试、流式事件测试与打包通过。
- `/v3/api-docs` 正常可读。
- 前端统一映射测试通过。

- [ ] **Step 2: 同步交付状态**

在 `docs/engineering/current-delivery-status.md` 中补：

- 当前状态
- 已完成契约治理范围
- 下一动作
- 阻塞项

预期输出：

- 交付状态与计划、测试文档保持一致。
