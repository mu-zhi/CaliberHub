---
name: feature-test-report
description: "用于生成并校验特性级测试文档，结合回归测试与 E2E 测试能力，作为退出 reviewing 的门禁。"
---

# 特性测试与验收（feature-test-report）

## Use when

- 工作项进入 `reviewing`
- 需要为特性文档产出测试文档
- 需要判断是否可从 `reviewing` 退出

## Workflow

1. 读取对应特性文档与 `docs/testing/features/README.md`。
2. 在 `docs/testing/features/` 下生成或更新对应测试文档。
3. 测试文档至少包含：
   - 对应特性文档路径
   - 验收范围与不覆盖项
   - 测试案例（输入、预期输出、实际结果）
   - TDD 命令或测试文件路径引用
   - 缺陷清单（P0/P1/P2 + 状态）
   - 最终结论
4. 若改动涉及 API 或关键逻辑，读取 `../ai-regression-testing/SKILL.md`。
5. 若改动涉及跨页面主流程或前后端联调，读取 `../e2e-testing/SKILL.md`。
6. 在退出 `reviewing` 前检查测试文档完备性及缺陷清单与结论一致性。

## Exit rules

1. 存在未关闭 `P0/P1` 时，不允许退出 `reviewing`。
2. 存在 `P2 open` 时，只允许使用“含 P2 遗留缺陷”的通过结论。
3. 修复后必须在原测试文档中补记回归验证记录。

## Guardrails

1. 读取 `ai-regression-testing` 时：本项目后端测试命令以 `cd backend && mvn -q test` 为准；`Vitest（测试框架）`、`Supabase（后端即服务）`、sandbox 模式内容不适用。
2. 读取 `e2e-testing` 时：默认访问地址应以 `http://127.0.0.1:5173`（前端）和 `http://127.0.0.1:8080`（后端）为准；若 `Playwright（浏览器自动化框架）` 未就绪，在测试文档中注明“E2E 暂缓，待 Playwright 配置后补”。
