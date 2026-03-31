---
name: test-driven-development
description: "用于实现阶段执行严格的 Red-Green-Refactor，并按场景接入回归测试与 E2E 测试能力；没有先失败的测试不写生产代码。"
---

# 测试驱动开发（test-driven-development）

## Workflow

1. 先定义本轮最小可验证行为。
2. 先写失败测试，保留失败证据。
3. 只写让测试通过的最小实现。
4. 测试全绿后再做重构。
5. 若改动影响 API、关键业务逻辑或已修复缺陷，读取 `../ai-regression-testing/SKILL.md`。
6. 若改动影响跨页面主流程、关键交互或前后端联调，读取 `../e2e-testing/SKILL.md`。

## Evidence

Keep evidence for:

- 失败测试命令
- 通过测试命令
- 若适用，新增的回归测试或 E2E 测试范围

## Guardrails

1. 没有失败测试，不写生产代码。
2. 不用“应该能过”替代真实执行结果。
3. 回归缺陷优先补回归测试，再修代码。
4. 读取 `ai-regression-testing` 时：本项目后端为 `Spring Boot（应用框架）` + `Maven（项目构建工具）`，测试命令以 `cd backend && mvn -q test` 为准；skill 内的 `Vitest（测试框架）`、`NextRequest（Next.js 请求对象）`、sandbox 模式示例均不适用。
5. 读取 `e2e-testing` 时：本项目前端运行在 `http://127.0.0.1:5173`，后端运行在 `http://127.0.0.1:8080`，需双端同时在线；若 `Playwright（浏览器自动化框架）` 尚未安装或配置，`E2E（端到端，End-to-End）` 验证可暂缓，并在测试文档中写明豁免原因与补充计划。
