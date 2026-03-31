# 运行决策台场景覆盖与澄清分支 v2 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在运行决策台补齐多场景命中与澄清分支的候选方案、合并提示和运行态契约覆盖，并把 `runtimeMode` / `degradeReasonCodes` / `planCandidates` / `mergeHints` 固化到可回归测试里。

**Architecture:** 后端继续作为知识包契约真源，前端只负责把 `clarification` 分支展开为可读、可操作的知识拆解卡片。本次只补运行决策台的视图表达和测试覆盖，不改运行推理主链路、场景召回规则或数据模型边界。

**Tech Stack:** React、Vitest、jsdom、Spring Boot Test、MockMvc

---

### Task 1: 锁定当前特性状态与计划入口

**Files:**
- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `docs/plans/2026-03-31-runtime-coverage-v2-implementation-plan.md`

- [ ] **Step 1: 写入新的工作项引用并确认现状**

```markdown
| 运行检索与知识包更多场景覆盖测试 | 运行决策台前端收口 | [运行决策与知识包生成](../architecture/features/iteration-02-runtime-and-governance/08-runtime-decision-and-knowledge-package.md) | [2026-03-31-runtime-coverage-v2-implementation-plan.md](../plans/2026-03-31-runtime-coverage-v2-implementation-plan.md) | 待更新：`docs/testing/features/iteration-02-runtime-and-governance/11-runtime-retrieval-and-clarification-test-report.md` | `planning（规划中）` | 当前已确认特性就绪，无阻塞；下一步补前端场景覆盖与澄清分支断言。 | 进入 TDD，先补红测再补实现。 | 运行决策台现有链路已通，缺口仅在候选方案与合并提示展示。 | 无 | Codex（实现） | 2026-03-31 |
```

- [ ] **Step 2: 运行文档定位命中确认**

Run:
```bash
rg -n "运行检索与知识包更多场景覆盖测试|2026-03-31-runtime-coverage-v2-implementation-plan|运行决策与知识包生成" docs/engineering/current-delivery-status.md docs/plans/2026-03-31-runtime-coverage-v2-implementation-plan.md
```

Expected:
```text
current-delivery-status.md 命中 1 条工作项
本计划文件命中 1 处引用
```

- [ ] **Step 3: 记录问题清单与风险**

问题清单：
- 当前状态文档里已有旧一轮运行检索工作项，必须避免把 `v2` 覆盖成重复口径。

风险评估：
- 0.2 - 仅文档同步，不影响运行链路。

---

### Task 2: 先写会失败的前端红测

**Files:**
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.test.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`

- [ ] **Step 1: 在 SSR 断言里加入候选方案与合并提示**

```jsx
const CLARIFICATION_RESULT_V2 = {
  decision: "clarification_only",
  reasonCode: "MULTI_SCENE_AMBIGUOUS",
  runtimeMode: "CLARIFICATION",
  degradeReasonCodes: ["MULTI_SCENE_AMBIGUOUS"],
  clarification: {
    summary: "当前问题同时命中两个场景，请拆分后分别检索",
    sceneCandidates: [
      { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询", snapshotId: 42 },
      { sceneId: 2, sceneCode: "SCN_PAYROLL_BATCH", sceneTitle: "代发批次结果查询", snapshotId: 43 },
    ],
    planCandidates: [
      { sceneCode: "SCN_PAYROLL_DETAIL", planId: 11, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" },
      { sceneCode: "SCN_PAYROLL_BATCH", planId: 12, planCode: "PLAN_PAYROLL_BATCH", planName: "代发批次结果方案" },
    ],
    subQuestions: ["按协议号查询代发明细", "按公司户查询代发批次结果"],
    mergeHints: ["请先选择「代发明细查询」或「代发批次结果查询」，再分别提交运行请求"],
    clarificationQuestions: ["本次是查询明细还是批次结果？"],
  },
  trace: { traceId: "trace_clar_002", snapshotId: null, inferenceSnapshotId: 99 },
  risk: { riskLevel: "MEDIUM", riskReasons: ["跨场景多意图"] },
};
```

```jsx
expect(html).toContain("代发明细方案");
expect(html).toContain("代发批次结果方案");
expect(html).toContain("合并提示");
expect(html).toContain("请先选择「代发明细查询」或「代发批次结果查询」");
```

- [ ] **Step 2: 在交互测试里把澄清分支改成多候选场景样例**

```jsx
const CLARIFICATION_RESULT_V2 = {
  decision: "clarification_only",
  reasonCode: "MULTI_SCENE_AMBIGUOUS",
  runtimeMode: "CLARIFICATION",
  degradeReasonCodes: ["MULTI_SCENE_AMBIGUOUS"],
  clarification: {
    summary: "当前问题同时命中两个场景，请拆分后分别检索",
    sceneCandidates: [
      { sceneId: 1, sceneCode: "SCN_PAYROLL_DETAIL", sceneTitle: "代发明细查询", snapshotId: 42 },
      { sceneId: 2, sceneCode: "SCN_PAYROLL_BATCH", sceneTitle: "代发批次结果查询", snapshotId: 43 },
    ],
    planCandidates: [
      { sceneCode: "SCN_PAYROLL_DETAIL", planId: 11, planCode: "PLAN_PAYROLL_DETAIL", planName: "代发明细方案" },
      { sceneCode: "SCN_PAYROLL_BATCH", planId: 12, planCode: "PLAN_PAYROLL_BATCH", planName: "代发批次结果方案" },
    ],
    subQuestions: ["按协议号查询代发明细", "按公司户查询代发批次结果"],
    mergeHints: ["请先选择「代发明细查询」或「代发批次结果查询」"],
    clarificationQuestions: ["本次是查询明细还是批次结果？"],
  },
  trace: { traceId: "trace_clar_002", snapshotId: null, inferenceSnapshotId: 99 },
  risk: { riskLevel: "MEDIUM", riskReasons: ["跨场景多意图"] },
};
```

```jsx
expect(screen.getByText("候选方案")).toBeTruthy();
expect(screen.getByText("代发明细方案")).toBeTruthy();
expect(screen.getByText("代发批次结果方案")).toBeTruthy();
expect(screen.getByText("合并提示")).toBeTruthy();
```

- [ ] **Step 3: 运行测试确认先失败**

Run:
```bash
cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx
```

Expected:
```text
FAIL
Unable to find text "候选方案" 或 "合并提示"
```

- [ ] **Step 4: 记录问题清单与风险**

问题清单：
- 当前前端澄清卡没有展示 `planCandidates` 与 `mergeHints`，测试必须先把这个缺口暴露出来。

风险评估：
- 0.6 - 测试 fixture 和页面渲染需要同口径，否则会出现“测试写对了但页面和契约不一致”的漂移。

---

### Task 3: 补前端澄清卡实现

**Files:**
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`

- [ ] **Step 1: 增加计划候选与合并提示渲染**

```jsx
{(result.clarification.planCandidates || []).length ? (
  <div>
    <h4>候选方案</h4>
    <ul>
      {(result.clarification.planCandidates || []).map((item) => (
        <li key={`${item.sceneCode || "scene"}-${item.planId || item.planCode}`}>
          {item.planName || item.planCode}
          {item.sceneCode ? ` · ${item.sceneCode}` : ""}
        </li>
      ))}
    </ul>
  </div>
) : null}
{(result.clarification.mergeHints || []).length ? (
  <div>
    <h4>合并提示</h4>
    <ul>
      {(result.clarification.mergeHints || []).map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  </div>
) : null}
```

- [ ] **Step 2: 保持澄清卡其余结构不变，只扩展展示面**

Run:
```bash
cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx
```

Expected:
```text
PASS
```

- [ ] **Step 3: 记录问题清单与风险**

问题清单：
- 需要确认 `planCandidates` 的展示顺序是否始终与后端返回顺序一致。

风险评估：
- 0.4 - 仅前端结构扩展，若 DOM 结构变化过大可能影响既有 SSR/交互断言。

---

### Task 4: 用后端契约测试把澄清字段钉住

**Files:**
- Modify: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/KnowledgePackageApiIntegrationTest.java`
- Modify: `docs/testing/features/iteration-02-runtime-and-governance/11-runtime-retrieval-and-clarification-test-report.md`

- [ ] **Step 1: 给澄清场景增加 planCandidates 与 mergeHints 断言**

```java
mockMvc.perform(post("/api/graphrag/query")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                  "queryText": "查询公司户最近一年代发批次和协议号明细",
                  "requestedFields": ["公司户", "协议号", "金额"],
                  "purpose": "代发多场景回放",
                  "operator": "support"
                }
                """))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.decision").value("clarification_only"))
    .andExpect(jsonPath("$.clarification.planCandidates[0].planCode").value(fixture.detailPlanCode))
    .andExpect(jsonPath("$.clarification.planCandidates[1].planCode").value(fixture.batchPlanCode))
    .andExpect(jsonPath("$.clarification.mergeHints[0]").value("请先选择「代发明细查询」或「代发批次结果查询」，再分别提交运行请求"));
```

- [ ] **Step 2: 将本轮新增覆盖写回测试报告**

```markdown
## 七、本轮 v2 覆盖增量

- 新增澄清分支候选方案展示：`planCandidates`
- 新增澄清分支合并提示展示：`mergeHints`
- 保持 `runtimeMode` / `degradeReasonCodes` / `trace.inferenceSnapshotId` 既有断言
```

- [ ] **Step 3: 运行后端契约回归**

Run:
```bash
cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test
```

Expected:
```text
BUILD SUCCESS
```

- [ ] **Step 4: 记录问题清单与风险**

问题清单：
- 后端澄清契约虽然已有 `planCandidates`，但仍需要回归测试明确锁定字段顺序与文案。

风险评估：
- 0.3 - 集成测试依赖真实种子数据，若样例数据变动会导致断言失配。

---

### Task 5: 收口状态与验收

**Files:**
- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `docs/testing/features/iteration-02-runtime-and-governance/11-runtime-retrieval-and-clarification-test-report.md`

- [ ] **Step 1: 把工作项状态改成已实现并写清下一动作**

```markdown
| 运行检索与知识包更多场景覆盖测试 | 运行决策台前端收口 | [运行决策与知识包生成](../architecture/features/iteration-02-runtime-and-governance/08-runtime-decision-and-knowledge-package.md) | [2026-03-31-runtime-coverage-v2-implementation-plan.md](../plans/2026-03-31-runtime-coverage-v2-implementation-plan.md) | [11-runtime-retrieval-and-clarification-test-report.md](../testing/features/iteration-02-runtime-and-governance/11-runtime-retrieval-and-clarification-test-report.md) | `reviewing（测试与评审中）` | 前端澄清卡已补齐候选方案与合并提示，后端契约断言已更新。 | 进入验收命令与结果记录，随后再做分支收尾。 | 场景覆盖与澄清分支的 v2 断言已闭环。 | 无 | Codex（实现） | 2026-03-31 |
```

- [ ] **Step 2: 在测试报告里补 v2 验收条目**

```markdown
## 七、v2 覆盖增量

1. 澄清卡新增候选方案 `planCandidates`
2. 澄清卡新增合并提示 `mergeHints`
3. 后端契约通过 `KnowledgePackageApiIntegrationTest` 锁定字段
```

- [ ] **Step 3: 跑最终验收命令**

Run:
```bash
cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx
cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test
cd frontend && npm run build
```

Expected:
```text
PASS
BUILD SUCCESS
vite build / build successful
```

- [ ] **Step 4: 记录问题清单与风险**

问题清单：
- 最终验收必须确认前端 build 不会因为新澄清卡结构引入样式或导入错误。

风险评估：
- 0.2 - 变更范围集中在运行决策台单页，回归面有限。

---

## 验收命令

1. `cd frontend && npm test -- src/pages/KnowledgePackageWorkbenchPage.test.jsx src/pages/KnowledgePackageWorkbenchPage.interaction.test.jsx`
2. `cd backend && mvn -q -Dtest=KnowledgePackageApiIntegrationTest test`
3. `cd frontend && npm run build`

## 预期结果

- `KnowledgePackageWorkbenchPage` 的 SSR 与交互测试全部通过。
- `KnowledgePackageApiIntegrationTest` 的澄清分支字段断言全部通过。
- `frontend build` 成功，运行决策台不影响其它页面。
