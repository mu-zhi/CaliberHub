# AI Assets

`ai/` 收口仓库内所有 AI 协作资产。

## 目录说明

- `agents/`：通用代理说明。
- `contexts/`：上下文模板。
- `hooks/`：Hook 配置与说明。
- `rules/`：规则集与工程守护约束。
- `skills/`：可复用技能。
- `project/`：项目级代理与 Superpowers 相关资产。

当前项目级 `skill（技能）` 已包含 `chatgpt-browser-bridge（ChatGPT 浏览器桥接技能）`，用于在已打开的 `Google Chrome（谷歌浏览器）` `ChatGPT（对话式人工智能产品）` 标签页上执行稳定读写桥接，并在用户明确授权时进入“持续盯守并自动跟进”模式；对上层以自然语言意图和 `skill（技能）` 触发，对内才调用底层脚本。

项目正文不得与这些资产混写；凡是路径、触发语或协作规则变化，应同步更新根 `AGENTS.md` 和 `docs/engineering/collaboration-workflow.md`。
