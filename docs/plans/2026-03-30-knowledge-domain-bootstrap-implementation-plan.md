# 知识生产业务领域初始化修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让知识生产台中的“重载业务领域”在领域表为空时，能够把 `01-当前业务侧的业务场景分类` 的一级分类初始化为可选业务领域，并保持幂等。

**Architecture:** 后端新增“从业务分类树引导初始化业务领域”的命令入口，统一给启动兜底和前端按钮复用；前端把“重载业务领域”从单纯刷新列表改成“先尝试初始化，再重新拉取列表”。测试按 `TDD（测试驱动开发，Test-Driven Development）` 执行，先补后端接口红测，再补前端动作红测，最后做联调验证和文档回写。

**Tech Stack:** `Spring Boot`、`MockMvc`、`React`、`Vitest`

---

### Task 1: 同步特性文档与交付状态

**Files:**
- Modify: `docs/architecture/features/iteration-01-knowledge-production/03-资产建模与治理对象编辑.md`
- Modify: `docs/engineering/current-delivery-status.md`
- Create: `docs/plans/2026-03-30-knowledge-domain-bootstrap-implementation-plan.md`

- [ ] **Step 1: 回写特性约束**

```md
11. “知识生产台 -> 场景编辑与发布”中的 `业务领域` 选择器在检测到领域表为空时，必须允许用户通过“重载业务领域”从 `01-当前业务侧的业务场景分类.md` 的一级分类初始化默认领域。
12. 初始化只创建一级 `Domain（业务领域）`，不把二级、三级分类直接写入领域表，并保持幂等。
```

- [ ] **Step 2: 记录在途工作**

Run: `rg -n "知识生产业务领域初始化修复" docs/engineering/current-delivery-status.md`
Expected: 命中新增加的工作项标题，说明该修复已进入当前在途列表。

- [ ] **Step 3: 保存实施计划**

Run: `test -f docs/plans/2026-03-30-knowledge-domain-bootstrap-implementation-plan.md && echo ok`
Expected: 输出 `ok`

### Task 2: 后端补齐业务领域初始化接口

**Files:**
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/adapter/web/DomainController.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/service/command/DomainCommandAppService.java`
- Modify: `backend/src/main/java/com/cmbchina/datadirect/caliber/infrastructure/common/bootstrap/DomainBootstrapInitializer.java`
- Create: `backend/src/main/java/com/cmbchina/datadirect/caliber/application/api/dto/response/DomainBootstrapResultDTO.java`
- Test: `backend/src/test/java/com/cmbchina/datadirect/caliber/adapter/web/DomainApiIntegrationTest.java`

- [ ] **Step 1: 写后端红测**

```java
@Test
void shouldBootstrapDomainsFromBusinessCategories() throws Exception {
    String token = loginAndGetToken("support", "support123");

    mockMvc.perform(post("/api/domains/bootstrap-from-categories")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)))
            .andExpect(jsonPath("$.domains[?(@.domainName == '零售基础业务')]").isArray())
            .andExpect(jsonPath("$.domains[?(@.domainName == '公司业务')]").isArray());
}
```

- [ ] **Step 2: 运行红测并确认失败**

Run: `cd backend && mvn -q -Dtest=DomainApiIntegrationTest#shouldBootstrapDomainsFromBusinessCategories test`
Expected: FAIL，原因是 `/api/domains/bootstrap-from-categories` 尚未提供。

- [ ] **Step 3: 写最小实现**

```java
@PostMapping("/bootstrap-from-categories")
public ResponseEntity<DomainBootstrapResultDTO> bootstrapFromCategories() {
    return ResponseEntity.ok(domainCommandAppService.bootstrapFromBusinessCategories(SecurityOperator.currentOperator(null)));
}
```

```java
public DomainBootstrapResultDTO bootstrapFromBusinessCategories(String operator) {
    List<TopicNode> roots = businessCategoryTreeProvider.roots();
    int createdCount = 0;
    for (int index = 0; index < roots.size(); index += 1) {
        TopicNode root = roots.get(index);
        String domainCode = "CATEGORY_" + normalizeCode(root.code().isBlank() ? root.id() : root.code());
        if (caliberDomainSupport.existsByDomainCode(domainCode)) {
            continue;
        }
        caliberDomainSupport.save(CaliberDomain.create(
                domainCode,
                root.name(),
                "从业务场景分类一级目录初始化",
                "",
                "",
                (index + 1) * 10,
                normalizeOperator(operator)
        ));
        createdCount += 1;
    }
    List<DomainDTO> domains = domainAssembler.toDTOList(caliberDomainSupport.findAllOrderBySortOrder());
    return new DomainBootstrapResultDTO(createdCount, domains.size(), domains);
}
```

```java
@Override
public void run(ApplicationArguments args) {
    domainCommandAppService.bootstrapFromBusinessCategories("system");
}
```

- [ ] **Step 4: 运行后端绿测**

Run: `cd backend && mvn -q -Dtest=DomainApiIntegrationTest#shouldBootstrapDomainsFromBusinessCategories test`
Expected: PASS

### Task 3: 前端把“重载业务领域”接到初始化链路

**Files:**
- Modify: `frontend/src/pages/KnowledgePage.jsx`
- Create: `frontend/src/pages/knowledge-domain-actions.js`
- Test: `frontend/src/pages/knowledge-domain-actions.test.js`

- [ ] **Step 1: 写前端红测**

```js
import { describe, expect, it, vi } from "vitest";
import { reloadDomainsWithBootstrap } from "./knowledge-domain-actions";

describe("reloadDomainsWithBootstrap", () => {
  it("bootstraps and reloads when domain list is empty", async () => {
    const loadDomains = vi
      .fn()
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([{ id: 1, domainName: "零售基础业务" }]);
    const bootstrapDomains = vi.fn().mockResolvedValue({ createdCount: 5, totalCount: 5 });

    const result = await reloadDomainsWithBootstrap({ loadDomains, bootstrapDomains });

    expect(bootstrapDomains).toHaveBeenCalledTimes(1);
    expect(loadDomains).toHaveBeenCalledTimes(2);
    expect(result.domains).toEqual([{ id: 1, domainName: "零售基础业务" }]);
    expect(result.bootstrapped).toBe(true);
  });
});
```

- [ ] **Step 2: 运行红测并确认失败**

Run: `cd frontend && npm test -- src/pages/knowledge-domain-actions.test.js`
Expected: FAIL，原因是 `reloadDomainsWithBootstrap` 尚不存在。

- [ ] **Step 3: 写最小实现并接入页面**

```js
export async function reloadDomainsWithBootstrap({ loadDomains, bootstrapDomains }) {
  const domains = await loadDomains();
  if (domains.length > 0) {
    return { domains, bootstrapped: false, bootstrapResult: null };
  }
  const bootstrapResult = await bootstrapDomains();
  const nextDomains = await loadDomains();
  return { domains: nextDomains, bootstrapped: true, bootstrapResult };
}
```

```js
async function handleReloadDomains() {
  setDomainLoading(true);
  setError("");
  try {
    const result = await reloadDomainsWithBootstrap({
      loadDomains,
      bootstrapDomains: () =>
        apiRequest("/api/domains/bootstrap-from-categories", {
          method: "POST",
          token,
        }),
    });
    if (result.domains.length > 0) {
      setPreprocessMeta(result.bootstrapped ? "已按业务分类初始化业务领域，并完成刷新。" : `业务领域刷新完成，共 ${result.domains.length} 个可选项。`);
      return;
    }
    setError("当前无可选业务领域，请点击\"去业务领域管理\"先创建业务领域。");
  } finally {
    setDomainLoading(false);
  }
}
```

- [ ] **Step 4: 运行前端绿测**

Run: `cd frontend && npm test -- src/pages/knowledge-domain-actions.test.js`
Expected: PASS

### Task 4: 回归验证与交付收口

**Files:**
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 跑本次变更相关测试**

Run: `cd backend && mvn -q -Dtest=DomainApiIntegrationTest test`
Expected: PASS

Run: `cd frontend && npm test -- src/pages/knowledge-domain-actions.test.js src/pages/KnowledgePage.render.test.jsx`
Expected: PASS

- [ ] **Step 2: 构建前端**

Run: `cd frontend && npm run build`
Expected: PASS，允许仅存在既有 chunk size warning。

- [ ] **Step 3: 同步交付状态为实现完成**

```md
最新完成：已打通“重载业务领域 -> 业务分类初始化 -> 刷新领域下拉”链路。
下一动作：进入代码检视与最终验收。
```

