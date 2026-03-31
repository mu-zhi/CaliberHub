# 运维验收与上线保障实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Feature Doc:** `docs/architecture/features/iteration-02-runtime-and-governance/11b-运维验收与上线保障.md`

**Goal:** 把上线门禁、样板回放、`Runbook（运行手册）`、切换步骤和回退验证落成可执行基线，确保候选快照在进入正式发布前有统一的准备动作、统一的阻断标准和统一的回退后验证。

**Scope:** 当前轮只补“上线资格与回退保障”这条横切治理能力，不修改业务场景口径；重点建设 `NFR` 门禁、样板回放编排、`Runbook` 目录、发布前检查脚本和回退后验证清单。

**Preconditions:**

1. 以 `docs/architecture/features/iteration-02-runtime-and-governance/11b-运维验收与上线保障.md` 为唯一特性真源。
2. 与 `07-发布检查、灰度发布与回滚.md` 分工固定为：`07` 负责页面执行与快照切换动作，`11b` 负责上线资格、回放基线、`Runbook` 和回退验证门禁。
3. 当前轮先固化测试文档与门禁脚本，再接入发布流程；不允许先把发布按钮接入再反补上线标准。

**Task Split:** 先文档化门禁，再脚本化检查，再接入后端聚合读接口和发布准备页，最后完成回放与回退演练验证。

---

## Task 1: 建立测试文档、Runbook 骨架与发布准备清单

**Files:**

- Create: `docs/testing/features/iteration-02-runtime-and-governance/11b-运维验收与上线保障-测试报告.md`
- Create: `docs/operations/runbooks/README.md`
- Create: `docs/operations/runbooks/release-blocking-alerts.md`
- Create: `docs/operations/runbooks/rollback-verification.md`
- Create: `scripts/release/check_release_readiness.sh`

- [ ] **Step 1: 新建测试文档骨架**

至少包含：

```md
# 运维验收与上线保障测试报告

## 1. 测试范围
- NFR 门禁
- 样板回放集
- 发布准备检查
- 回退触发条件
- 回退后验证

## 2. 用例清单
- [ ] ReleaseReadinessScriptTest
- [ ] ReplayReadinessIntegrationTest
- [ ] RollbackVerificationChecklistTest
```

- [ ] **Step 2: 新建 Runbook 目录骨架**

`docs/operations/runbooks/README.md` 至少列出：

- 阻断发布告警处置
- 灰度冻结
- 快照回退
- 回退后验证

- [ ] **Step 3: 新建发布准备脚本骨架**

`check_release_readiness.sh` 初版至少做以下检查位：

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "[check] test docs present"
echo "[check] replay suite configured"
echo "[check] rollback runbook present"
echo "[check] current delivery status linked"
```

- [ ] **Step 4: 运行骨架脚本**

执行命令：

- `bash scripts/release/check_release_readiness.sh`

预期输出：

- 脚本成功执行，但会明确暴露后续待接入的真实检查位。

## Task 2: 固化 NFR 门禁与回放清单的后端契约

**Files:**

- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/release/ReleaseReadinessScriptTest.java`
- Create: `backend/src/test/java/com/cmbchina/datadirect/caliber/release/ReplayReadinessIntegrationTest.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/release/ReleaseReadinessDTO.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/query/release/ReleaseReadinessQueryService.java`

- [ ] **Step 1: 先写失败的 NFR / 回放契约测试**

`ReleaseReadinessScriptTest.java` 至少断言：

```java
@Test
void shouldRequireNfrReplayAndRollbackSections() {
    ReleaseReadinessDTO dto = releaseReadinessQueryService.loadCurrentReadiness();
    assertThat(dto.nfrChecks()).isNotEmpty();
    assertThat(dto.replaySuites()).isNotEmpty();
    assertThat(dto.rollbackChecks()).isNotEmpty();
}
```

- [ ] **Step 2: 定义上线准备聚合 DTO**

```java
public record ReleaseReadinessDTO(
        List<ReadinessCheckDTO> nfrChecks,
        List<ReplaySuiteDTO> replaySuites,
        List<RollbackCheckDTO> rollbackChecks,
        String overallStatus
) {}
```

- [ ] **Step 3: 运行红灯测试**

执行命令：

- `cd backend && mvn -q -Dtest=ReleaseReadinessScriptTest,ReplayReadinessIntegrationTest test`

预期输出：

- 失败，提示发布准备聚合服务和 DTO 尚未存在。

## Task 3: 接入上线准备聚合查询与发布准备页消费

**Files:**

- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/ReleaseReadinessController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/OpenApiGroupedControllerConfig.java`
- Create: `frontend/src/pages/release-readiness-adapter.js`
- Modify: `frontend/src/pages/PublishWorkbenchPage.jsx`
- Modify: `frontend/src/pages/PublishWorkbenchPage.test.jsx`

- [ ] **Step 1: 暴露发布准备聚合接口**

建议接口：

```java
@GetMapping("/api/release/readiness")
public ReleaseReadinessDTO loadReadiness() { ... }
```

- [ ] **Step 2: 在发布中心增加“上线准备 / 回退预案”标签区**

`PublishWorkbenchPage.jsx` 至少渲染：

- 强制 `NFR` 检查
- 样板回放清单
- 回退触发条件
- `Runbook` 快捷入口

- [ ] **Step 3: 复跑前后端测试**

执行命令：

- `cd backend && mvn -q -Dtest=ReleaseReadinessScriptTest,ReplayReadinessIntegrationTest test`
- `cd frontend && npm test -- src/pages/PublishWorkbenchPage.test.jsx`

预期输出：

- 后端发布准备聚合断言通过。
- 前端能渲染上线准备区，不再只展示候选发布信息。

## Task 4: 把样板回放和回退后验证脚本化

**Files:**

- Create: `scripts/release/run_release_replay_suite.sh`
- Create: `scripts/release/verify_rollback_recovery.sh`
- Modify: `scripts/release/check_release_readiness.sh`
- Modify: `docs/operations/runbooks/rollback-verification.md`

- [ ] **Step 1: 增加样板回放脚本**

脚本至少分三类输出：

- 运行时知识包样板
- 高敏审批 / 拒绝样板
- 数据地图 / 发布状态联动样板

- [ ] **Step 2: 增加回退后验证脚本**

脚本至少检查：

- `/v3/api-docs` 可用
- 核心样板回放恢复
- 审计链路未断
- 版本展示与读源一致

- [ ] **Step 3: 运行脚本烟测**

执行命令：

- `bash scripts/release/run_release_replay_suite.sh`
- `bash scripts/release/verify_rollback_recovery.sh`

预期输出：

- 脚本要么通过，要么给出明确失败项；不能只输出笼统成功 / 失败。

## Task 5: 联调发布准备门禁并同步交付状态

**Files:**

- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 运行完整发布准备验证**

执行命令：

- `cd backend && mvn -q -Dtest=ReleaseReadinessScriptTest,ReplayReadinessIntegrationTest test`
- `cd frontend && npm test -- src/pages/PublishWorkbenchPage.test.jsx`
- `bash scripts/release/check_release_readiness.sh`
- `bash scripts/release/run_release_replay_suite.sh`
- `bash scripts/release/verify_rollback_recovery.sh`

预期输出：

- 后端测试、前端测试和三条脚本检查全部通过，且每条检查都能定位失败项。

- [ ] **Step 2: 同步状态真源**

在 `docs/engineering/current-delivery-status.md` 中补：

- 当前工作项状态
- 已落地门禁
- 待补外部依赖
- 下一动作

预期输出：

- 交付状态与实施计划、测试文档、`Runbook` 一致。
