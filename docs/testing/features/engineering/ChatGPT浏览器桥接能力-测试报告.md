# ChatGPT 浏览器桥接能力测试文档

> 对应特性文档：`docs/engineering/chatgpt-browser-bridge-capability.md`
> 当前阶段：`reviewing（评审验证中）`

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
| TC-01 | 列出已打开 ChatGPT 标签页 | `list-tabs` | 返回 `ok=true` 与标签页数组 | 通过：`python3 scripts/chatgpt_browser_bridge.py list-tabs` 返回 1 个匹配标签页，含 `window_index / tab_index / title / url / active`。 |
| TC-02 | 读取结构化消息 | `read --output json` | 返回 `messages` 或 `bodyText`，且结构稳定 | 通过：`python3 scripts/chatgpt_browser_bridge.py read --output json` 返回当前会话标题、消息列表、`bodyText`、`composer` 与 `sendButtonAvailable`。 |
| TC-03 | 默认输入不发送 | `type --text "bridge smoke"` | 返回输入成功且不触发发送 | 通过：单元测试已覆盖 `type` 命令显式要求 `--text`、默认 `sent=false` 的契约；本轮为避免污染当前会话，豁免浏览器级写入烟测。 |
| TC-04 | 显式发送 | `send` 或 `type --send-after-type` | 仅在显式动作下触发发送 | 通过：单元测试覆盖显式发送确认；浏览器级负向烟测 `python3 scripts/chatgpt_browser_bridge.py send` 在空输入框下稳定返回 `NOTHING_TO_SEND`，未误发送。 |
| TC-05 | 环境失败可收敛 | 关闭标签页或关闭权限 | 返回稳定错误码与错误消息 | 通过：`python3 scripts/chatgpt_browser_bridge.py read --output json --tab-match definitely-not-found` 返回退出码 `3` 与 `TARGET_TAB_NOT_FOUND`。 |

## 5. TDD 与测试命令引用

1. `python3 -m unittest discover -s scripts/tests -p 'test_chatgpt_browser_bridge.py'`
2. `python3 scripts/chatgpt_browser_bridge.py list-tabs`
3. `python3 scripts/chatgpt_browser_bridge.py read --output json`
4. `python3 scripts/chatgpt_browser_bridge.py send`
5. `python3 scripts/chatgpt_browser_bridge.py read --output json --tab-match definitely-not-found`
6. `python3 -m py_compile scripts/chatgpt_browser_bridge.py`

## 6. 缺陷清单

- 当前未发现 `P0 / P1 / P2` 缺陷。
- 残余风险 1：本轮未做浏览器级 `type` 正向烟测，避免在当前真实会话里留下未发送草稿。
- 残余风险 2：按共享流程，本项仍缺一次 `Claude Code` 只读代码检视；当前先以 `Codex` 自检 + 单元测试 + 无副作用烟测收口。

## 7. 放行结论

当前实现已进入 `reviewing（评审验证中）`，单元测试、脚本编译检查、`list-tabs / read / send(empty)` 与负向 `tab-match` 烟测均已通过；在补齐 `Claude Code` 只读检视前，暂不标记为正式放行完成。
