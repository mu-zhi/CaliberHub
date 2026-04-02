# 开发手册

> 本手册是团队级 `Development Manual（开发手册）` 主入口，用于统一说明项目开发流程、门禁、模型路由、`Claude Code（代码智能体）` 启动方式、项目级工具入口与维护责任。涉及正式协作规则时，以 [协作工作流](./collaboration-workflow.md) 为准；本手册负责把这些规则整理成可直接执行的开发操作说明。

## 1. 手册定位

1. 本手册面向团队日常开发、联调、评审、交接与工具接入。
2. 本手册是“怎么在这个项目里做开发”的统一入口，不替代正式设计文档、特性文档、实施计划和交付状态真源。
3. 当 [协作工作流](./collaboration-workflow.md)、[当前交付状态](./current-delivery-status.md)、`ai/` 下项目级技能、`scripts/` 下项目级脚本、`tooling/` 下本地工具说明发生变化时，应同步检查本手册。

## 2. 开发真源与入口

### 2.1 文档真源

- 项目正文真源固定在 `docs/`。
- 正式设计主文档固定为：
  - [系统设计](../architecture/system-design.md)
  - [前端工作台设计](../architecture/frontend-workbench-design.md)
  - [特性文档目录](../architecture/features/README.md)
- 当前开发进度、下个阶段工作与交接状态真源固定为 [当前交付状态](./current-delivery-status.md)。
- 方案过程稿与实施计划固定写入 `docs/plans/`，不替代正式设计真源。

### 2.2 六个稳定入口

- `做方案`：先收敛需求、边界与实现路径，不直接编码。
- `写计划`：把确认后的设计拆成可执行小任务。
- `按方案实现`：按特性文档和实施计划落地实现。
- `评审这次改动`：做只读回归评审，优先找问题。
- `同步接口文档`：按实现代码与注解同步 `SpringDoc（接口文档生成框架，SpringDoc OpenAPI）` / `OpenAPI（开放接口描述规范，OpenAPI Specification）`。
- `更新项目文档`：按正文路由和术语规则同步文档。

## 3. 标准开发流程

项目默认采用四段式链路：

1. `Brainstorming（方案脑暴技能）`
2. `Writing Plans（实施计划编写技能）`
3. `执行计划`
4. `完成分支`

### 3.1 方案阶段

- 适用场景：新功能、行为变更、跨模块需求、目标不清晰的任务。
- 产出要求：先通过问答和方案对比收敛设计，再把正式结论回写主文档；过程稿如有必要再写入 `docs/plans/`。
- 硬门禁：未经确认，不进入计划和实现。

### 3.2 计划阶段

- 适用场景：设计已确认，且需要进入实施准备。
- 产出要求：实施计划必须写入 `docs/plans/`，并显式引用对应特性文档。
- 任务粒度：每步控制在 2 至 5 分钟，写清精确文件路径、完整代码、测试命令与预期输出。
- 硬门禁：功能新增、行为变更、场景实现、能力增强类任务，若没有对应特性文档或缺少同步实施计划，不得开工。

### 3.3 实现阶段

- 默认执行方式：`subagent-driven-development（子代理驱动开发技能）`。
- 批量执行场景：用户明确要求一次跑完整个计划，改用 `executing-plans（批量执行计划技能）`。
- 开始前提：必须同时具备对应特性文档和实施计划。
- 实现完成后不直接进测试，先走代码检视。

### 3.4 收尾阶段

- 全部验证通过后，进入 `finishing-a-development-branch（开发分支收尾技能）`。
- 只允许四个收尾选项：
  - 本地合并回基线分支
  - 推送并创建 `PR（合并请求，Pull Request）`
  - 保留当前分支，稍后处理
  - 丢弃本次工作

## 4. 实施门禁

### 4.1 文档门禁

- 先特性文档，再实施计划，再按计划实现。
- 正式约束必须回写仓库文档、脚本、配置或测试；只存在于聊天记录中的规则不生效。
- 设计确认后、实施计划落地后、实现完成后、任务交接前，应同步 [当前交付状态](./current-delivery-status.md)；纯文档同步或纯只读评审场景可在对应记录中说明豁免。

### 4.2 测试门禁

- 所有实现任务默认强制 `TDD（测试驱动开发，Test-Driven Development）`。
- 必须保留 `Red（先写失败测试）` -> `Green（最小实现通过）` -> `Refactor（重构保持绿色）` 的可核验证据。
- 回归测试与 `E2E（端到端，End-to-End）` 验证由 `test-driven-development（测试驱动开发技能）` 统一接入 `ai-regression-testing（AI 回归测试技能）` 和 `e2e-testing（端到端测试技能）`。

### 4.3 评审门禁

- 任务间默认自动请求审查：先做规格符合性检查，再做代码质量检查。
- 实现完成后，必须先经过 `code-reviewing（代码检视技能）`，由 `Claude Code（代码智能体）` 执行只读评审。
- 中高风险改动默认补一次 `change-review（变更评审技能）`。高风险情形包括跨模块联动、公共接口或数据契约变更、导航与布局改动、协作规则调整。
- 进入测试与验收阶段后，由 `feature-test-report（特性测试与验收技能）` 生成或更新测试文档。

## 5. 角色与状态流转

当前项目支持“规则触发的半自动多智能体协作”，固定角色如下：

- `Design Agent（设计智能体）`：负责 `brainstorming`、特性文档缺口巡检与门禁检查。
- `Plan Agent（计划智能体）`：负责实施计划生成与修订。
- `Build Agent（实现智能体）`：负责按计划实现、批量执行和 `TDD（测试驱动开发）` 实施。
- `Review Agent（评审智能体）`：负责任务间审查、代码检视、测试文档与变更评审。

状态驱动规则如下：

- `brainstorming` -> `Design Agent（设计智能体）`
- `planning` 且文档未齐备 -> `Design Agent（设计智能体）`
- `planning` 且文档已齐备 -> `Plan Agent（计划智能体）`
- `implementing` / `fixing` -> `Build Agent（实现智能体）`
- `reviewing` -> `Review Agent（评审智能体）`

### 5.1 接力门禁与状态巡检试运行

- 团队接力默认按三个检查点更新 [当前交付状态](./current-delivery-status.md)：设计确认后补 `来源设计 / 当前状态 / 下一动作`，实施计划落地后补 `来源计划`，实现启动或交接前补 `最新完成 / 下一动作 / 责任人 / 最后更新时间`。
- 这三个检查点属于团队接力门禁，不要求新建额外台账；如发现工作项缺少其中任一检查点，优先回写状态真源，而不是在聊天记录或计划正文里补滚动摘要。
- 状态巡检的默认输入固定为 [当前交付状态](./current-delivery-status.md) 中的 `当前状态` 字段。`planning` 优先推荐 `Design Agent（设计智能体）` 或 `Plan Agent（计划智能体）`，`implementing` / `fixing` 优先推荐 `Build Agent（实现智能体）`，`reviewing` 优先推荐 `Review Agent（评审智能体）`。
- 状态巡检类 `automation（自动化巡检）` 只负责推荐下一角色和下一动作，不直接改 `当前状态`，不直接提交业务代码，也不绕过既有文档、评审和测试门禁。
- 特性缺口巡检结果必须显式拆成三类：`文档缺口`、`计划缺口`、`排期缺口`。其中 `计划缺口` 指“特性文档已存在但缺对应实施计划”，不得并入“已覆盖”或被表述成“后续补计划”。
- 当状态巡检推荐的角色与用户即时指令冲突时，以用户即时指令为准；当推荐结果与主文档、特性文档或实施计划冲突时，先修正文档真源，再继续推进实现。

## 6. 模型与运行时路由

阶段级模型路由真源固定为 [`ai/project/agents/model-routing.json`](/Users/rlc/Code/CaliberHub/ai/project/agents/model-routing.json)。本节只保留团队需要直接执行的约定。

### 6.1 Codex 自动化主链路

| 环节 | 主运行时 / 模型 | 推理强度 | 备选 |
| --- | --- | --- | --- |
| 补特性文档 | `Codex / gpt-5.4` | `xhigh` | `Claude Code / claude-sonnet-4-6-20260218` |
| 特性文档评审 | `Claude Code / claude-sonnet-4-6-20260218` | 默认 | `Codex / gpt-5.4 + xhigh` |
| 写实施计划 | `Codex / gpt-5.4` | `xhigh` | `Claude Code / claude-sonnet-4-6-20260218` |
| 实施计划复核 | `Claude Code / claude-sonnet-4-6-20260218` | 默认 | `Codex / gpt-5.4 + xhigh` |
| 推进开发计划 | `Codex / gpt-5.2-codex` | `high` | `Claude Code / qwen3-coder-plus` |
| 任务间代码审查 / 整体代码检视 | `Claude Code / claude-sonnet-4-6-20260218` | 默认 | `Codex / gpt-5.4 + xhigh` |

推进开发计划时，以下情况默认把 `gpt-5.2-codex` 从 `high` 升级到 `xhigh`：

- 跨 5 个以上文件
- 同一问题两轮修复仍失败
- 涉及公共接口、数据契约或核心状态机
- 需要先读大量上下文再改动

### 6.2 本地 Claude Code 启动方式

本地 `Claude Code（代码智能体）` provider 入口统一通过 `scripts/claude_mux.sh` 管理。私有配置模板位于 [`tooling/claude-providers/README.md`](/Users/rlc/Code/CaliberHub/tooling/claude-providers/README.md)。

统一入口：

```bash
bash scripts/claude_mux.sh <provider> <model> [claude args...]
```

固定快捷入口：

- `bash scripts/claude-dev.sh`：百炼 `qwen3.5-plus`
- `bash scripts/claude-coder.sh`：百炼 `qwen3-coder-plus`
- `bash scripts/claude-plan.sh`：百炼 `glm-5`
- `bash scripts/claude_review.sh`：问问 `claude-sonnet-4-6-20260218`
- `bash scripts/claude-review-deep.sh`：问问 `claude-opus-4-6-20260205`

全局快捷入口：

- `claude1`：百炼 `qwen3.5-plus`
- `claude2`：百炼 `qwen3-coder-plus`
- `claude3`：百炼 `glm-5`
- `claude4`：问问 `claude-sonnet-4-6-20260218`
- `claude5`：问问 `claude-opus-4-6-20260205`

全局命令当前安装在本机 `PATH` 目录 `/Users/rlc/.npm-global/bin`，底层统一转发到 [`scripts/claude_mux.sh`](/Users/rlc/Code/CaliberHub/scripts/claude_mux.sh)。该组命令依赖当前仓库路径与 `tooling/claude-providers/*.env.local` 私有配置保持可用；若仓库搬迁或重建本机环境，需要同步更新对应 wrapper。

使用约束：

- `claude_mux.sh` 只负责本地 `Claude Code（代码智能体）` provider 切换，不替代当前 `Codex（代码智能体）` 主会话。
- 代码检视主入口固定为 `bash scripts/claude_review.sh`。
- 编码备选入口固定为 `bash scripts/claude-coder.sh`。
- 任意目录下的快速启动优先使用 `claude1` 到 `claude5`；需要显式指定 provider + model 或临时追加不同模型时，再回退到 `bash scripts/claude_mux.sh <provider> <model>`。
- 强模型文档 / 计划备选入口可直接使用 `bash scripts/claude_mux.sh wenwen claude-sonnet-4-6-20260218 --print "<prompt>"`。
- 只有 `runtime=codex` 的模型可直接参与 `spawn_agent`；`Claude Code（代码智能体）` 备选通过本地脚本执行。

## 7. 项目级工具与脚本入口

### 7.1 协作与桥接类工具

- [`scripts/chatgpt_browser_bridge.py`](/Users/rlc/Code/CaliberHub/scripts/chatgpt_browser_bridge.py)：桥接已打开的 `ChatGPT（对话式人工智能产品）` 页面，提供 `list-tabs / read / type / send` 四个子命令。
- [`docs/engineering/chatgpt-browser-bridge-capability.md`](/Users/rlc/Code/CaliberHub/docs/engineering/chatgpt-browser-bridge-capability.md)：说明浏览器桥接边界、错误码与安全约束。
- [`scripts/chatgpt_heartbeat_runner.py`](/Users/rlc/Code/CaliberHub/scripts/chatgpt_heartbeat_runner.py)：每 10 分钟轮询当前唯一已打开的 `ChatGPT（对话式人工智能产品）` 标签页；浏览器桥接可读时会落快照，判断“上下文是否已同步 / 当前进行到哪个阶段 / 是否出现新回复”。当检测到新的 `assistant（助手）` 回复时，会前台激活 `Codex（代码智能体）` 桌面应用并继续尝试调用 `Codex CLI（命令行版 Codex）` 生成下一轮跟进消息；若本轮刚成功发送跟进消息，还会进入一个短观察窗口，按秒级轮询继续接住 GPT 的下一次回复，而不必等到下一个 10 分钟周期。这条链路基于轮询，不是浏览器事件订阅。
- [`scripts/chatgpt_accessibility_send.py`](/Users/rlc/Code/CaliberHub/scripts/chatgpt_accessibility_send.py)：当 `System Events（系统事件）` 辅助功能权限可用时，使用剪贴板 + 粘贴发送把文本发到指定 `ChatGPT（对话式人工智能产品）` 会话，不依赖页面 JavaScript 读写。
- [`docs/engineering/chatgpt-heartbeat-launchd.plist`](/Users/rlc/Code/CaliberHub/docs/engineering/chatgpt-heartbeat-launchd.plist)：本机 `launchd（macOS 定时任务守护）` 样板，按 600 秒间隔执行浏览器 GPT 巡检。

浏览器 GPT 协作默认规则固定如下：

1. 当用户提到“和浏览器里的 GPT 继续讨论”“盯着当前 ChatGPT”“有回复就继续推进”这一类意图时，默认行为是持续盯守当前会话，直到细节讨论清楚或双方明确确认“可作为最终稿”。
2. 用户若未特别说明“只读一次”或“只发这一条”，不得把浏览器桥接当成一次性 `read / send` 动作后就停止。
3. 若前台标签页被切到其他网站，但 `Google Chrome（谷歌浏览器）` 中仍存在已打开的 `chatgpt.com` 标签页，巡检与桥接应优先切回该会话再继续，而不是把会话误判为结束。
4. 只有在用户明确说“只读一次”“先别继续追问”“只发这条”时，才降级成单次读取或单次发送模式。
- [`scripts/claude_mux.sh`](/Users/rlc/Code/CaliberHub/scripts/claude_mux.sh)：本地 `Claude Code（代码智能体）` provider + model 统一入口。
- `claude1`：全局 `Claude Code（代码智能体）` 快捷命令，对应百炼 `qwen3.5-plus`。
- `claude2`：全局 `Claude Code（代码智能体）` 快捷命令，对应百炼 `qwen3-coder-plus`。
- `claude3`：全局 `Claude Code（代码智能体）` 快捷命令，对应百炼 `glm-5`。
- `claude4`：全局 `Claude Code（代码智能体）` 快捷命令，对应问问 `claude-sonnet-4-6-20260218`。
- `claude5`：全局 `Claude Code（代码智能体）` 快捷命令，对应问问 `claude-opus-4-6-20260205`。

### 7.2 运行与验证类脚本

- `bash scripts/start_backend.sh`：启动后端，默认端口 `8082`，自动加载 `.env.local`；安全默认值已开启，至少需要在 `.env.local` 中提供 `CALIBER_JWT_SECRET` 与本地联调用账号口令环境变量，可从仓库根目录 `.env.example` 复制模板。
- `cd frontend && npm run dev`：启动前端，默认端口 `5174`。
- `cd frontend && npm run test:e2e:install`：安装 `Playwright（浏览器自动化框架）` 所需的 Chromium 浏览器。
- `cd frontend && npm run test:e2e`：执行浏览器级 `E2E（端到端，End-to-End）` 回归；默认由 `Playwright` 拉起本地前端 `5174`，并在用例内部 mock `API（应用程序接口，Application Programming Interface）` 响应。
- `bash scripts/run_system_test_flow.sh`：执行系统联调主链路验证；默认探活后端 `8082` 与前端 `5174`，可用 `BASE_URL` / `FRONTEND_URL` 覆盖。
- `bash scripts/run_nfr_acceptance_gate.sh`：执行 `NFR（非功能需求，Non-Functional Requirement）` 验收门禁。
- `bash scripts/sync_frontend_dist.sh`：将现有前端静态产物同步到后端，不再负责执行前端构建。

### 7.3 常用命令

```bash
cd backend && mvn -q test
cd backend && mvn -q package
cd frontend && npm run build
bash scripts/start_backend.sh
```

## 8. 交付与验活要求

- 代码变更后，必须重启受影响服务并保持项目处于可验证状态。
- 本地默认联调要求后端 `8082` 与前端 `5174` 同时在线，除非任务明确限定为单端。
- 对外汇报完成前，必须用轻量命令验证服务可用性；涉及接口契约时，追加 `/v3/api-docs` 或相关业务接口校验。
- “按方案实现”类任务收尾时，统一输出四项：

```text
改动摘要：
执行命令：
服务状态：
剩余风险：
```

## 9. 手册维护规则

以下变化会影响团队执行路径，必须在同一轮协作中同步更新本手册；未更新前，视为尚未形成正式项目级规则：

- 新增或下线项目级工具、脚本入口、桥接能力、自动化巡检入口
- 新增或调整固定角色智能体、状态流转规则、门禁顺序
- 新增或调整 `Claude Code（代码智能体）` 启动方式、provider 路由、模型映射
- 新增或调整 `Codex（代码智能体）` 阶段级模型路由、推理强度升级条件
- 新增或调整统一交付格式、验收门禁、服务探活要求

更新本手册时，至少同步检查以下文件是否需要一起修改：

- [`README.md`](/Users/rlc/Code/CaliberHub/README.md)
- [`docs/README.md`](/Users/rlc/Code/CaliberHub/docs/README.md)
- [`AGENTS.md`](/Users/rlc/Code/CaliberHub/AGENTS.md)
- [`docs/engineering/collaboration-workflow.md`](/Users/rlc/Code/CaliberHub/docs/engineering/collaboration-workflow.md)
- [`ai/project/agents/model-routing.json`](/Users/rlc/Code/CaliberHub/ai/project/agents/model-routing.json)
- [`tooling/claude-providers/README.md`](/Users/rlc/Code/CaliberHub/tooling/claude-providers/README.md)
