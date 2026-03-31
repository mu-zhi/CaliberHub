# ChatGPT 浏览器桥接能力 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development（子代理驱动开发技能）` or `executing-plans（批量执行计划技能）` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为仓库内 `skill（技能）` / `agent（智能体）` 落一个稳定的 `ChatGPT Browser Bridge（ChatGPT 浏览器桥接器）`：只面向已打开的 `https://chatgpt.com/` 标签页，提供 `list-tabs / read / type / send` 四个 `CLI（命令行接口，Command Line Interface）` 子命令，输出稳定 `JSON（JavaScript对象表示法，JavaScript Object Notation）`，默认不自动发送。

**Architecture:** 采用“单脚本 + 专用 skill（技能）”双层结构。`scripts/chatgpt_browser_bridge.py` 负责所有脆弱实现细节，包括 `osascript` 调用、目标标签页定位、页面结构读取、输入与发送动作、统一退出码；`ai/skills/chatgpt-browser-bridge/SKILL.md` 只负责触发语、前置检查、推荐命令和失败回退顺序，不承载 DOM（文档对象模型，Document Object Model） 选择器或 `AppleScript（苹果脚本）` 细节。

**Tech Stack:** Python 3 标准库（`argparse / json / subprocess / dataclasses / unittest`）、`osascript`、Markdown 文档体系（`docs/engineering/`、`docs/plans/`、`docs/testing/features/`、`ai/skills/`）

---

## Design Inputs

- `docs/engineering/chatgpt-browser-bridge-capability.md`
- `AGENTS.md`
- `docs/engineering/collaboration-workflow.md`
- `docs/engineering/current-delivery-status.md`
- `ai/README.md`

## File Map

- Create: `docs/testing/features/engineering/ChatGPT浏览器桥接能力-测试报告.md`
- Create: `scripts/chatgpt_browser_bridge.py`
- Create: `scripts/tests/test_chatgpt_browser_bridge.py`
- Create: `ai/skills/chatgpt-browser-bridge/SKILL.md`
- Modify: `AGENTS.md`
- Modify: `README.md`
- Modify: `ai/README.md`
- Modify: `docs/README.md`
- Modify: `docs/engineering/collaboration-workflow.md`
- Modify: `docs/engineering/current-delivery-status.md`

## Scope

- 只支持 `macOS（苹果桌面系统） + Google Chrome（谷歌浏览器） + Apple 事件 JavaScript 执行已开启`
- 只支持 `https://chatgpt.com/`
- 只支持“已打开标签页”，不新开浏览器
- 默认输出 `JSON（JavaScript对象表示法，JavaScript Object Notation）`
- 默认不发送，发送必须显式调用 `send` 或 `--send-after-type`
- 首轮不做通用网页桥接、文件上传、模型切换和多站点 provider（站点适配层）

---

## Command Contract

### Stable Commands

```bash
python3 scripts/chatgpt_browser_bridge.py list-tabs
python3 scripts/chatgpt_browser_bridge.py read --output json
python3 scripts/chatgpt_browser_bridge.py type --text "你好"
python3 scripts/chatgpt_browser_bridge.py type --text "你好" --send-after-type
python3 scripts/chatgpt_browser_bridge.py send
```

### Success Envelope

```json
{
  "ok": true,
  "command": "read",
  "url": "https://chatgpt.com/c/...",
  "title": "方案细化与实施问题",
  "data": {}
}
```

### Error Envelope

```json
{
  "ok": false,
  "command": "read",
  "error": {
    "code": "TARGET_TAB_NOT_FOUND",
    "message": "未找到已打开的 https://chatgpt.com/ 标签页。"
  }
}
```

### Exit Codes

- `0`：成功
- `2`：环境前置失败，例如 `Google Chrome（谷歌浏览器）` 未运行或 `osascript` 不可用
- `3`：未找到目标标签页
- `4`：页面结构不可读或命中错误页
- `5`：输入框不可用
- `6`：发送条件不满足或显式发送失败

---

## Task 1: 先固化测试骨架与验收文档

**Files:**

- Create: `docs/testing/features/engineering/ChatGPT浏览器桥接能力-测试报告.md`
- Create: `scripts/tests/test_chatgpt_browser_bridge.py`

- [ ] **Step 1: 创建测试文档骨架**

在 `docs/testing/features/engineering/ChatGPT浏览器桥接能力-测试报告.md` 中写入以下内容：

```md
# ChatGPT 浏览器桥接能力测试文档

> 对应特性文档：`docs/engineering/chatgpt-browser-bridge-capability.md`
> 当前阶段：待进入 `reviewing（评审验证中）`

## 1. 测试目标

验证 `ChatGPT Browser Bridge（ChatGPT 浏览器桥接器）` 能在不新开浏览器的前提下，稳定定位已打开的 `https://chatgpt.com/` 标签页，返回统一 `JSON（JavaScript对象表示法，JavaScript Object Notation）`，并支持安全输入。

## 2. 测试范围

本轮覆盖：

1. `list-tabs / read / type / send` 四个子命令的输出契约
2. 环境前置检查与错误码
3. 默认不发送的安全边界
4. `skill（技能）` 调用说明与文档导航同步

本轮不覆盖：

1. Safari / Edge / Brave
2. 任意网站桥接
3. 文件上传、模型切换、多模态输入

## 3. 测试环境

1. 操作系统：`macOS（苹果桌面系统）`
2. 浏览器：`Google Chrome（谷歌浏览器）`
3. 页面前置：已登录 `ChatGPT（对话式人工智能产品）` 且已打开目标标签页
4. 权限前置：已开启 `Apple 事件 JavaScript 执行`

## 4. 测试案例

| 编号 | 用例 | 输入 | 预期输出 | 实际结果 |
| --- | --- | --- | --- | --- |
| TC-01 | 列出已打开 ChatGPT 标签页 | `list-tabs` | 返回 `ok=true` 与标签页数组 | 待回填 |
| TC-02 | 读取结构化消息 | `read --output json` | 返回 `messages` 或 `bodyText`，且结构稳定 | 待回填 |
| TC-03 | 默认输入不发送 | `type --text "bridge smoke"` | 返回输入成功且不触发发送 | 待回填 |
| TC-04 | 显式发送 | `send` 或 `type --send-after-type` | 仅在显式动作下触发发送 | 待回填 |
| TC-05 | 环境失败可收敛 | 关闭标签页或关闭权限 | 返回稳定错误码与错误消息 | 待回填 |

## 5. TDD 与测试命令引用

待在实现完成并进入 `reviewing（评审验证中）` 后回填：

1. `python3 -m unittest discover -s scripts/tests -p 'test_chatgpt_browser_bridge.py'`
2. `python3 scripts/chatgpt_browser_bridge.py list-tabs`
3. `python3 scripts/chatgpt_browser_bridge.py read --output json`

## 6. 缺陷清单

进入 `reviewing（评审验证中）` 后按 `P0 / P1 / P2` 回填。

## 7. 放行结论

当前阶段尚未进入 `reviewing（评审验证中）`，暂不提供放行结论。
```

- [ ] **Step 2: 创建失败优先的单元测试**

在 `scripts/tests/test_chatgpt_browser_bridge.py` 中写入以下测试目标：

1. `list-tabs` 成功输出必须包含 `ok / command / data.tabs`
2. `read` 成功输出必须包含 `ok / command / url / title / data`
3. `type` 默认不发送，且必须要求 `--text`
4. `send` 只能通过显式命令触发
5. 环境失败返回预期退出码和 `error.code`

建议测试结构如下：

```python
import importlib.util
import json
import pathlib
import unittest
from unittest import mock


MODULE_PATH = pathlib.Path(__file__).resolve().parents[2] / "scripts" / "chatgpt_browser_bridge.py"


def load_module():
    spec = importlib.util.spec_from_file_location("chatgpt_browser_bridge", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ChatGptBrowserBridgeCliTest(unittest.TestCase):
    def test_read_success_envelope(self):
        module = load_module()
        with mock.patch.object(module, "handle_read", return_value={"messages": [], "composer": {"available": True}}):
            exit_code, payload = module.dispatch(["read"])
        self.assertEqual(exit_code, 0)
        self.assertTrue(payload["ok"])
        self.assertEqual(payload["command"], "read")
        self.assertIn("data", payload)

    def test_type_requires_text(self):
        module = load_module()
        exit_code, payload = module.dispatch(["type"])
        self.assertEqual(exit_code, 2)
        self.assertFalse(payload["ok"])
        self.assertEqual(payload["error"]["code"], "TEXT_REQUIRED")
```

- [ ] **Step 3: 运行 Red（先写失败测试）**

Run: `python3 -m unittest discover -s scripts/tests -p 'test_chatgpt_browser_bridge.py'`

Expected:

- FAIL
- 失败原因是脚本尚未创建，或 `dispatch / handle_read` 等目标接口未实现

---

## Task 2: 实现稳定 CLI（命令行接口）骨架与环境前置检查

**Files:**

- Create: `scripts/chatgpt_browser_bridge.py`
- Test: `scripts/tests/test_chatgpt_browser_bridge.py`

- [ ] **Step 1: 落命令解析、统一结果信封与退出码**

在 `scripts/chatgpt_browser_bridge.py` 中实现以下固定常量与入口：

```python
DEFAULT_URL_PREFIX = "https://chatgpt.com/"

EXIT_OK = 0
EXIT_ENVIRONMENT = 2
EXIT_TAB_NOT_FOUND = 3
EXIT_PAGE_UNREADABLE = 4
EXIT_COMPOSER_UNAVAILABLE = 5
EXIT_SEND_FAILED = 6
```

并实现：

1. `build_success(command, url, title, data)`
2. `build_error(command, code, message, exit_code)`
3. `dispatch(argv)`：供单元测试直接调用，返回 `(exit_code, payload)`
4. `main()`：负责打印 `JSON（JavaScript对象表示法，JavaScript Object Notation）` 并 `sys.exit(exit_code)`

- [ ] **Step 2: 收口环境前置检查**

实现以下检查函数：

1. `ensure_osascript_available()`
2. `ensure_chrome_running()`
3. `ensure_chatgpt_tab_exists()`
4. `run_osascript(script: str) -> str`

要求：

1. 所有底层异常都转成统一 `BridgeCommandError`
2. 失败时必须带稳定 `error.code`
3. 不在脚本内部 `print` 零散日志；默认只输出统一 `JSON（JavaScript对象表示法，JavaScript Object Notation）`

- [ ] **Step 3: 运行阶段性测试**

Run: `python3 -m unittest discover -s scripts/tests -p 'test_chatgpt_browser_bridge.py'`

Expected:

- 仍可能 FAIL
- 但失败点应从“模块不存在/入口不存在”收敛到具体业务子命令尚未实现

---

## Task 3: 实现 ChatGPT 页面适配层

**Files:**

- Modify: `scripts/chatgpt_browser_bridge.py`
- Test: `scripts/tests/test_chatgpt_browser_bridge.py`

- [ ] **Step 1: 实现 `list-tabs` 与目标标签页定位**

实现：

1. `handle_list_tabs(args)`
2. `find_chatgpt_tabs()`
3. `select_target_tab(tab_match=None)`

要求：

1. 默认按 `https://chatgpt.com/` 前缀匹配
2. 返回 `window_index / tab_index / title / url / active`
3. 不新开标签页或浏览器

- [ ] **Step 2: 实现 `read` 的结构化提取**

实现 `handle_read(args)`，统一返回：

```json
{
  "messages": [
    { "index": 1, "role": "user", "text": "..." },
    { "index": 2, "role": "assistant", "text": "..." }
  ],
  "bodyText": "...",
  "composer": {
    "available": true,
    "mode": "textarea",
    "currentText": ""
  }
}
```

要求：

1. 优先读取 `data-message-author-role`
2. 如消息结构缺失，可回退返回 `bodyText`
3. 必须识别输入框可用性与当前文本
4. 命中 `chrome-error://chromewebdata/` 或错误页文案时返回 `PAGE_UNREADABLE`

- [ ] **Step 3: 实现 `type` 与 `send`**

实现：

1. `handle_type(args)`
2. `handle_send(args)`

规则：

1. `type` 必须要求 `--text`
2. `type` 默认只输入，不发送
3. `type --send-after-type` 允许在输入后触发发送
4. `send` 先找发送按钮点击，再回退到键盘 `Enter（回车发送）`
5. 输入或发送失败必须返回明确错误码，不能让调用方从自然语言里猜

- [ ] **Step 4: 运行 Green（最小实现通过）**

Run: `python3 -m unittest discover -s scripts/tests -p 'test_chatgpt_browser_bridge.py'`

Expected:

- PASS
- `list-tabs / read / type / send` 的契约测试通过

---

## Task 4: 固化 skill（技能）与仓库导航

**Files:**

- Create: `ai/skills/chatgpt-browser-bridge/SKILL.md`
- Modify: `AGENTS.md`
- Modify: `README.md`
- Modify: `ai/README.md`
- Modify: `docs/README.md`
- Modify: `docs/engineering/collaboration-workflow.md`

- [ ] **Step 1: 写专用 skill 正文**

在 `ai/skills/chatgpt-browser-bridge/SKILL.md` 中写清：

1. 触发语：例如“抓取已打开的 `ChatGPT（对话式人工智能产品）` 页面”“读取当前 ChatGPT 对话”“向已打开 ChatGPT 输入内容”
2. 前置检查：`Google Chrome（谷歌浏览器）` 正在运行、已登录 `ChatGPT（对话式人工智能产品）`、`Apple 事件 JavaScript 执行` 已开启
3. 推荐命令：`list-tabs / read / type / send`
4. 失败回退顺序：先查权限，再查目标标签页，再查错误页

- [ ] **Step 2: 同步共享规则与导航**

在下列文件中补充新能力：

1. `AGENTS.md`：补充相关技能与自动触发短语
2. `docs/engineering/collaboration-workflow.md`：补充这是一个项目级浏览器桥接 skill（技能），用于内部 `agent（智能体）` 调用
3. `README.md`、`docs/README.md`、`ai/README.md`：补足导航入口，避免新增文档和 skill（技能） 成为隐藏资产

- [ ] **Step 3: 运行文档导航校验**

Run: `rg -n "chatgpt-browser-bridge|ChatGPT 浏览器桥接|chatgpt_browser_bridge.py" AGENTS.md README.md docs/README.md ai/README.md docs/engineering/collaboration-workflow.md ai/skills/chatgpt-browser-bridge/SKILL.md`

Expected:

- 六个文件都能命中新能力或脚本路径

---

## Task 5: 做本地烟测、回填测试文档与状态真源

**Files:**

- Modify: `docs/testing/features/engineering/ChatGPT浏览器桥接能力-测试报告.md`
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 运行无副作用烟测**

Run:

1. `python3 scripts/chatgpt_browser_bridge.py list-tabs`
2. `python3 scripts/chatgpt_browser_bridge.py read --output json`

Expected:

1. `list-tabs` 返回至少一个 `https://chatgpt.com/` 标签页
2. `read` 返回 `messages` 或 `bodyText`，并且 `composer.available=true`

- [ ] **Step 2: 运行受控输入烟测**

在确认目标标签页允许临时写入输入框后执行：

1. `python3 scripts/chatgpt_browser_bridge.py type --text "Codex bridge smoke input"`
2. `python3 scripts/chatgpt_browser_bridge.py read --output json`

Expected:

1. `type` 成功，且返回 `sent=false`
2. 第二次 `read` 中的 `composer.currentText` 能看到 `Codex bridge smoke input`

说明：本轮烟测默认不触发真实发送；若必须验证 `send`，应在专门的可丢弃 `ChatGPT（对话式人工智能产品）` 对话中执行，并在测试文档中注明。

- [ ] **Step 3: 回填测试文档结果**

将 `docs/testing/features/engineering/ChatGPT浏览器桥接能力-测试报告.md` 更新为至少包含：

1. 已执行命令
2. `TC-01 ~ TC-05` 的实际结果
3. 已知限制：仅支持 `macOS（苹果桌面系统） + Google Chrome（谷歌浏览器） + 已打开 ChatGPT 标签页`

- [ ] **Step 4: 更新交付状态**

在 `docs/engineering/current-delivery-status.md` 中将本工作项从 `planning（规划中）` 推进到对应真实状态，并补齐：

1. 最新完成
2. 下一动作
3. 阻塞项
4. 最后更新时间

- [ ] **Step 5: 执行代码检视链路**

按顺序执行：

1. `requesting-code-review（请求代码审查技能）`
2. `code-reviewing（代码检视技能）`

Expected:

1. 若发现 `P0 / P1`，先回到对应任务修复后再送检
2. 无阻塞问题后才允许进入 `reviewing（评审验证中）` 或分支收尾
