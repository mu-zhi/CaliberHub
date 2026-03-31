---
name: chatgpt-browser-bridge
description: "Use when a task needs to read or write an already-open ChatGPT tab in Google Chrome without launching a new browser."
---

# ChatGPT 浏览器桥接（chatgpt-browser-bridge）

## Overview

这个 `skill（技能）` 是对上层 `agent（智能体）` 的正式入口。`python3 scripts/chatgpt_browser_bridge.py ...` 只是内部执行边界，不是面向用户的交互入口。

## Use When

- 任务需要读取已打开 `ChatGPT（对话式人工智能产品）` 页面内容
- 任务需要向已打开 `ChatGPT（对话式人工智能产品）` 页面输入文本
- 明确要求“不新开浏览器”，而是复用当前 `Google Chrome（谷歌浏览器）` 会话
- 用户用自然语言表达“读取当前对话”“向当前页面输入”“复用已打开浏览器”这类意图

## Workflow

1. 先读取 `/Users/rlc/LingChao_Ren/1.2、数据直通车/docs/engineering/chatgpt-browser-bridge-capability.md`，确认本轮边界仍是 `macOS（苹果桌面系统） + Google Chrome（谷歌浏览器） + https://chatgpt.com/`。
2. 先做前置检查：
   - `Google Chrome（谷歌浏览器）` 已运行
   - 已打开目标 `https://chatgpt.com/` 标签页
   - 已开启 `Apple 事件 JavaScript 执行`
3. 默认先执行：
   - `python3 scripts/chatgpt_browser_bridge.py list-tabs`
   - `python3 scripts/chatgpt_browser_bridge.py read --output json`
4. 需要写入时，先执行：
   - `python3 scripts/chatgpt_browser_bridge.py type --text "..." `
5. 只有在明确需要发送时，才执行：
   - `python3 scripts/chatgpt_browser_bridge.py send`
   - 或 `python3 scripts/chatgpt_browser_bridge.py type --text "..." --send-after-type`
6. 调用后只消费标准输出里的统一 `JSON（JavaScript对象表示法，JavaScript Object Notation）`，不要从自然语言错误文本里猜测状态。
7. 除非用户明确要求排障命令，否则不要让用户手动执行上述 `CLI（命令行接口）`；应由当前 `AI（人工智能）` 直接执行 skill 内部命令并返回结果。

## Guardrails

1. 不主动新开浏览器，不接管新的临时会话。
2. 首轮只支持 `https://chatgpt.com/`，不扩展成通用网页桥接器。
3. 默认不自动发送；没有显式发送意图时，只允许 `read` 或 `type`。
4. 若返回 `TARGET_TAB_NOT_FOUND`、`APPLE_EVENTS_JAVASCRIPT_DISABLED`、`PAGE_UNREADABLE`，先修环境，再决定是否重试。
5. 如需把抓取结果写回仓库文档，继续按主文档路由和测试文档门禁执行，不把浏览器抓取结果当作仓库真源。

## Exit Codes

- `0`：成功
- `2`：环境失败
- `3`：未找到目标标签页
- `4`：页面不可读
- `5`：输入框不可用
- `6`：显式发送失败或发送条件不满足
