# AGENTS

## 作用范围

- 本仓库是文档优先项目，同时保留可运行的 `frontend（前端）` / `backend（后端）` 构建、测试与联调命令。
- 根目录 `AGENTS.md` 只保留团队共享协作规则：项目边界、强制验证、文档同步、技能触发与交付格式。
- 详细协作协议以 [`docs/engineering/collaboration-workflow.md`](docs/engineering/collaboration-workflow.md) 为准；个人提示词习惯、模型偏好或临时实验配置不进入共享文件。
- 团队级开发流程、`Claude Code（代码智能体）` 启动方式、阶段模型映射、项目级工具入口与手册维护规则统一收口到 [`docs/engineering/development-manual.md`](docs/engineering/development-manual.md)。

## 共享协作协议

- 默认协作采用四段式基础工作流：`Brainstorming（方案脑暴技能）` -> `Writing Plans（实施计划编写技能）` -> `执行计划` -> `完成分支`。
- 当前项目支持“规则触发的半自动多智能体协作”：由状态流转优先推荐或唤起固定角色智能体，但保留人工覆盖权；固定角色包括 `Design Agent（设计智能体）`、`Plan Agent（计划智能体）`、`Build Agent（实现智能体）`、`Review Agent（评审智能体）`。
- 当前开发进度与下个阶段工作统一维护在 [`docs/engineering/current-delivery-status.md`](docs/engineering/current-delivery-status.md)；它是团队内部日常推进与任务接力的唯一真源，`README.md`、`docs/plans/`、正式设计正文与聊天消息只保留链接，不并行维护滚动状态摘要。
- `Brainstorming（方案脑暴技能）`：`AI（人工智能）` 先理解需求，通过一问一答收敛目标、边界、约束与成功标准，提出 2 至 3 种方案并分段让用户确认；确认后的正式设计内容优先回写项目主文档，`docs/plans/` 仅承接过程稿或中间设计稿。
- `Writing Plans（实施计划编写技能）`：把已确认设计拆成 2 至 5 分钟级别的极小任务；每个任务必须写清精确文件路径、完整代码、测试命令与预期输出；实施计划统一落到 `docs/plans/`。自本轮原则调整起，新增或重写特性文档时，必须在同一轮协作中同步产出对应实施计划，不允许先只落特性文档、后补计划。
- `执行计划`：默认优先 `subagent-driven-development（子代理驱动开发技能）`；如果用户明确选择批量执行，改用 `executing-plans（批量执行计划技能）`。
- `完成分支`：全部验证通过后，进入 `finishing-a-development-branch（开发分支收尾技能）`，只提供“本地合并 / 推送并创建 PR（合并请求，Pull Request）/ 保留分支 / 丢弃工作”四个选项。
- 中高风险实施任务在最终交付前应补一次 `change-review（变更评审技能）`；跨模块改动、公共接口或数据契约调整、前端导航或布局变更、文档拓扑调整默认视为至少中风险。

## 强制实施约束

- 所有功能开发、缺陷修复、行为变更与重构，默认先过 `Brainstorming（方案脑暴技能）`，未经确认不直接实现。
- 涉及功能新增、行为变更、场景实现或能力增强的开发任务，必须先把当前能力沉到对应特性文档，并在产出特性文档的同一轮协作中同步产出对应实施计划，最后按计划实现；缺少特性文档或实施计划时，不得开工；若无法判断当前任务是否触发该门禁，默认按触发处理。
- 所有实现任务强制执行 `TDD（测试驱动开发，Test-Driven Development）`，必须完整经历 `Red（先写失败测试）` -> `Green（最小实现通过）` -> `Refactor（重构保持绿色）`，并且必须看到测试先失败；相关回归测试与 `E2E（端到端，End-to-End）` 验证通过 `test-driven-development（测试驱动开发技能）` 统一接入 `ai-regression-testing（AI 回归测试技能）` 与 `e2e-testing（端到端测试技能）`。
- 任务执行期间默认“任务间自动请求审查”：每个任务完成后先做规格符合性检查，再做代码质量检查，必要时执行 `requesting-code-review（请求代码审查技能）`；未关闭问题前不进入下一个任务。
- 执行前默认先检查可用技能，并在合适时机自动触发；如果存在更贴切的项目技能或专题技能，先按技能约束执行。
- 实现完成后，不直接进入测试；必须先进入 `code-reviewing（代码检视技能）`，由 `Claude Code` 做只读代码检视，`Codex` 根据问题修复并复检通过后，才进入 `feature-test-report（特性测试与验收技能）` 所承载的测试文档与验收阶段。
- 设计确认后、实施计划落地后、实现完成后、任务交接前，必须同步 [`docs/engineering/current-delivery-status.md`](docs/engineering/current-delivery-status.md)；未同步视为未完成交接。
- 仓库是 `AI（人工智能）` 与团队共享执行上下文的唯一真源；凡影响实现、评审、验收或交接的正式约束，必须回写仓库文档、脚本、配置或测试。仅存在于聊天记录、口头说明或临时备注中的约束，不视为已生效约束。
- 新增或调整项目级工具、桥接能力、脚本入口、模型路由、固定角色智能体、自动化规则或统一交付格式时，必须在同一轮协作中同步更新 [`docs/engineering/development-manual.md`](docs/engineering/development-manual.md)；未更新前，视为尚未形成正式项目级规则。
- 实施计划中的测试入口、`Mock（模拟对象）`、纯逻辑抽离、测试文档骨架、脚本与断言工具等“脚手架任务”，必须先于对应业务任务出现；默认不新增额外标签，但要求在执行顺序上先固化边界、契约与反馈回路，再展开业务实现。
- 能通过 `lint（静态检查）`、结构校验脚本、测试、`CI（持续集成，Continuous Integration）` 或目录约束表达的规则，应逐步转为可执行门禁；在对应基础设施未就绪前，暂按“文档约束 + 人工检查点”执行，并在相关状态文档或评审记录中显式写明仍处于人工门禁阶段。
- 新增目录、共享组件、公共接口、页面主路径、数据契约、导航结构与协作规则时，必须同步补齐可复用的 `Golden Path（黄金路径样例）` 样例或模板；存量约束按优先级逐步补齐，当前默认从“数据地图高可见层首个真实执行样本”开始试点。
- 任何例外处理都必须在对应计划、评审或状态文档中写明豁免原因、临时边界、回收时机与责任人；未写明的例外，不允许长期存在。

## 六个稳定入口

- `做方案`：执行 `Brainstorming（方案脑暴技能）`，输出设计并优先回写正式主文档；如需保留过程稿，再写入 `docs/plans/`，不直接编码。
- `写计划`：执行 `Writing Plans（实施计划编写技能）`，把设计拆成可执行小任务并保存到 `docs/plans/`。
- `按方案实现`：在已确认设计和计划的前提下执行计划；默认优先 `subagent-driven-development（子代理驱动开发技能）`，用户明确要求批量执行时使用 `executing-plans（批量执行计划技能）`。
- `评审这次改动`：执行只读评审，优先找回归风险、契约漂移、缺失验证与缺失文档。
- `同步接口文档`：以实现代码与注解为真源，同步 `SpringDoc（接口文档生成框架，SpringDoc OpenAPI）` / `OpenAPI（开放接口描述规范，OpenAPI Specification）` 输出。
- `更新项目文档`：按 `README.md` 的 12 条规则路由文档增量，并同步术语表、主文档、协作文档与导航。

## 已知工作流

- “更新项目文档”场景：遵循 [`README.md`](README.md) 中“项目文档同步更新规范（12条，生效版）”以及 [`docs/engineering/collaboration-workflow.md`](docs/engineering/collaboration-workflow.md) 中的共享协作协议。
- 团队成员需要了解“本项目怎么开发、不同动作用什么模型、`Claude Code（代码智能体）` 怎么启动、有哪些项目级工具入口”时，统一先读 [`docs/engineering/development-manual.md`](docs/engineering/development-manual.md)。
- 项目正式设计内容的默认路由固定为：前端页面结构、导航、交互、状态表达和前端评审留痕写入 [`docs/architecture/frontend-workbench-design.md`](docs/architecture/frontend-workbench-design.md)；方案对象、运行主线、接口契约和治理边界写入 [`docs/architecture/system-design.md`](docs/architecture/system-design.md)；已经从主方案拆出的场景级特性文档写入 [`docs/architecture/features/README.md`](docs/architecture/features/README.md) 所在目录。特性文档是主方案的展开，不是并行真源；同一需求若同时影响两类内容，必须拆分回写，不等待用户提醒。
- 团队内部“当前开发进度 / 下个阶段工作”的默认路由固定为 [`docs/engineering/current-delivery-status.md`](docs/engineering/current-delivery-status.md)；它只承载当前事实、下一动作、阻塞项与任务接力，不替代正式设计文档和实施计划。
- 文档更新时，英文术语、缩写、变量名、常量名、指标名都要附中文解释；若术语表缺项，先补 [`docs/glossary.md`](docs/glossary.md)。
- 协作工作流基线固定在 [`docs/engineering/collaboration-workflow.md`](docs/engineering/collaboration-workflow.md)。
- `docs/plans/` 主要用于过程稿与实施计划；正式设计内容优先维护在项目主文档。实施计划建议命名为 `YYYY-MM-DD-<topic>-implementation-plan.md`。

## 相关技能

- `brainstorming（方案脑暴技能）`：所有新功能、行为变更、组件设计或复杂修复，先做方案澄清、方案对比与设计确认。
- `writing-plans（实施计划编写技能）`：把确认后的设计拆成极小任务，并写出完整代码、命令与预期输出。
- `subagent-driven-development（子代理驱动开发技能）`：默认推荐的计划执行方式；主 `AI（人工智能）` 派发子代理逐任务实施，并在每个任务后做双阶段审查。
- `executing-plans（批量执行计划技能）`：当用户明确要求批量执行或独立会话执行时使用，按计划逐步落地并定期做检查点。
- `test-driven-development（测试驱动开发技能）`：对实现阶段施加 `TDD（测试驱动开发，Test-Driven Development）` 硬约束，必须见到失败测试。
- `requesting-code-review（请求代码审查技能）`：在任务之间自动发起代码审查，防止问题跨任务累积。
- `code-reviewing（代码检视技能）`：在实现完成后、进入测试前，由 `Claude Code` 执行只读代码检视；未通过时由 `Codex` 修复并再次送检。
- `feature-doc-authoring（特性文档校验技能）`：在特性文档进入实施计划前执行最低完备性检查，阻止占位特性文档进入开发序列。
- `feature-doc-coverage-mapping（主方案能力映射技能）`：把主方案、前端主方案与特性文档目录比对成“原子能力项 -> 特性文档 + 实施计划”映射清单，识别 `已覆盖 / 部分覆盖 / 缺失`；其中“已有特性文档但缺对应实施计划”必须显式标记为缺口，并在现有文档接不住时建议新增专题文档。
- `feature-test-report（特性测试与验收技能）`：在 `reviewing（测试与评审中）` 阶段生成并校验测试文档，作为退出测试阶段的门禁。
- `semi-automatic-multi-agent-orchestration（半自动多智能体编排技能）`：按 `current-delivery-status.md` 的状态流转推荐或唤起固定角色智能体，统一收口到设计、计划、实现、评审四类角色。
- `finishing-a-development-branch（开发分支收尾技能）`：测试全部通过后，统一进入分支收尾与集成选项。
- `change-review（变更评审技能）`：做只读回归评审，关注回归风险、契约漂移、缺失验证与缺失文档。
- `chatgpt-browser-bridge（ChatGPT 浏览器桥接技能）`：在 `macOS（苹果桌面系统） + Google Chrome（谷歌浏览器）` 上桥接已打开的 `ChatGPT（对话式人工智能产品）` 标签页；对上层以 `skill（技能）` 触发，对内才调用 `list-tabs / read / type / send` 脚本命令。
- `update-project-docs（项目文档同步技能）`：方案或规则变更后同步主文档、术语表、README 导航与协作入口。
- `springdoc-api-sync（接口文档同步技能）`：接入 `SpringDoc（接口文档生成框架，SpringDoc OpenAPI）` / `Swagger（接口文档生态）`，减少实现与接口文档漂移。
- `frontend-design-governance（前端设计治理技能）`：约束前端页面、导航、中文界面口径与设计留痕。
- `web-design-guidelines（Web 设计规范评审技能）`：执行 `UI（用户界面，User Interface）` / `UX（用户体验，User Experience）` 审查与可访问性检查。
- `vercel-react-best-practices（React / Next 最佳实践技能）`：用于 React / Next.js 性能与工程实践校验。
- `vercel-composition-patterns（React 组件组合技能）`：用于 React 组件 API 和组合模式治理。
- `ai-regression-testing（AI 回归测试技能）`：为接口、关键业务逻辑和缺陷修复补回归测试策略，由 `test-driven-development（测试驱动开发技能）` 与 `feature-test-report（特性测试与验收技能）` 统一接入。
- `e2e-testing（端到端测试技能）`：为跨页面主流程、关键交互和联调场景补 `Playwright（浏览器自动化框架）` 验证策略，由 `test-driven-development（测试驱动开发技能）` 与 `feature-test-report（特性测试与验收技能）` 统一接入。

## 自动触发规则

- 用户提到“做方案”、新功能、行为变更、需求收敛、方案设计、实现路径选择时，先执行 `brainstorming（方案脑暴技能）`，未经确认不进入实现。
- 用户提到“把方案拆成特性”“主方案还没进入特性文档”“feature coverage”“特性拆分”“方案能力映射”“哪些能力还没拆出来”时，先执行 `feature-doc-coverage-mapping（主方案能力映射技能）`；该巡检除了识别缺失特性文档，还必须识别“已有特性文档但缺实施计划”的缺口；如果发现缺失项，再决定补现有特性文档、同步补实施计划，还是新增专题文档。
- 特性文档初稿形成后、用户提到“特性文档检查”“特性门禁”“feature doc review”“校验特性文档”时，执行 `feature-doc-authoring（特性文档校验技能）`；未通过时不进入 `writing-plans（实施计划编写技能）`。
- 用户提到“写计划”、实施计划、任务拆分、逐步执行清单时，执行 `writing-plans（实施计划编写技能）`，并把结果写入 `docs/plans/`。
- 用户提到“按方案实现”、执行计划、开始实施时：若任务适合逐任务推进，默认执行 `subagent-driven-development（子代理驱动开发技能）`；若用户明确要求批量执行、独立会话执行或一次跑完整个计划，则执行 `executing-plans（批量执行计划技能）`。
- 用户提到“多智能体协作”“自动创建智能体”“半自动编排”“多 agent”时，执行 `semi-automatic-multi-agent-orchestration（半自动多智能体编排技能）`；此后按状态流转优先采用固定角色智能体：`brainstorming -> Design Agent`、`planning -> Plan Agent`、`implementing/fixing -> Build Agent`、`reviewing -> Review Agent`。
- 进入任何实现阶段时，自动执行 `test-driven-development（测试驱动开发技能）`；没有先失败的测试，不写生产代码。
- 在 `subagent-driven-development（子代理驱动开发技能）` 或 `executing-plans（批量执行计划技能）` 的任务推进过程中，自动执行 `requesting-code-review（请求代码审查技能）` 或同等审查动作，作为任务间审查门禁。
- 实现任务完成后、进入测试前，自动执行 `code-reviewing（代码检视技能）`；代码检视未通过时，由 `Codex` 修复后再次进入 `code-reviewing（代码检视技能）`，复检通过后才允许进入测试。
- 用户提到“测试文档”“验收报告”“test report”“进入验收”“退出 reviewing”或工作项状态切换为 `reviewing（测试与评审中）` 时，执行 `feature-test-report（特性测试与验收技能）`。
- 当实现完成且测试全部通过时，自动进入 `finishing-a-development-branch（开发分支收尾技能）`。
- 用户提到“已打开的 ChatGPT”“不要新开浏览器”“抓取当前 ChatGPT 对话”“向当前 ChatGPT 页面输入”“浏览器桥接”时，执行 `ai/skills/chatgpt-browser-bridge/SKILL.md`，并由 `AI（人工智能）` 自行调用内部脚本，不要求用户手动运行命令。若请求语义包含“继续讨论”“有回复就继续”“盯着当前会话”“持续推进”，默认进入持续盯守模式，直到细节讨论清楚；只有用户明确要求“只读一次”或“只发一条”时才降级为单次动作。
- 用户提到 `Swagger`、`SpringDoc`、`OpenAPI`、`同步接口文档`、`接口文档自动生成`、`文档与实现漂移`、`/v3/api-docs`、`swagger-ui` 时，执行 `ai/project/agents/skills/springdoc-api-sync/SKILL.md`。
- 用户提到 `前端`、`页面`、`界面`、`UI`、`交互`、`导航`、`样式`、`图谱`、`组件` 时，执行 `ai/skills/frontend-design-governance/SKILL.md`。
- 用户提到 `质询`、`口径文档提问`、`对抗评审`、`红队评审`、`challenge case` 时，不自动执行 `ai/skills/caliber-redteam-review/SKILL.md`；当前技能停用，需先确认恢复范围与输出路径。
- 用户提到 `评审UI`、`可访问性`、`UX审计`、`设计规范`、`best practices` 时，执行 `ai/project/agents/skills/web-design-guidelines/SKILL.md`。
- 用户提到 `评审这次改动`、`review this change`、`代码评审`、`变更评审`、`帮我看这次改动` 时，执行 `ai/skills/change-review/SKILL.md`。
- 如果请求同时涉及前端评审与文档同步，执行顺序为 `web-design-guidelines` -> `frontend-design-governance` -> `change-review` -> `update-project-docs`。
- 如果请求同时涉及前端实现与文档同步，执行顺序为 `frontend-design-governance` -> `update-project-docs`。
- 如果请求同时涉及“主方案能力映射”和“特性文档门禁检查”，执行顺序为 `feature-doc-coverage-mapping` -> `feature-doc-authoring` -> `writing-plans`。
- 如果请求同时涉及实现、代码检视与测试验收，执行顺序为 `test-driven-development` -> `requesting-code-review` -> `code-reviewing` -> `feature-test-report`。

## 常用命令

- 前端开发：`cd frontend && npm run dev`（默认端口 `5174`）
- 前端构建：`cd frontend && npm run build`
- 后端启动：`bash scripts/start_backend.sh`（默认端口 `8082`，自动加载 `.env.local`）
- 后端测试：`cd backend && mvn -q test`
- 后端打包：`cd backend && mvn -q package`
- 前端静态产物同步到后端：`bash scripts/sync_frontend_dist.sh`

## 运行与交付要求

- 代码变更后，必须重启受影响服务并保持项目处于可验证状态。
- 本地默认联调要求 `backend（后端）` 的 `8082` 与 `frontend（前端）` 的 `5174` 同时在线，除非任务明确限定为单端。
- 对外汇报完成前，必须用轻量命令验证服务可用性；涉及接口契约时，追加 `/v3/api-docs` 或相关业务接口校验。
- “按方案实现”类任务收尾时，统一输出四项：`改动摘要`、`执行命令`、`服务状态`、`剩余风险`；如果进入 `finishing-a-development-branch（开发分支收尾技能）`，则在四项之后继续给出标准分支处理选项。
