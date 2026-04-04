# ChatGPT Browser Bridge Watch Auto Reply Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 `chatgpt-browser-bridge（ChatGPT 浏览器桥接技能）` 内补齐“持续盯守当前会话并继续回复”的自动发送守护模式，并为重复消费、重复回路和失败停机补工程护栏。

**Architecture:** 保持 `ai/skills/chatgpt-browser-bridge/SKILL.md` 为唯一正式入口，继续让 `scripts/chatgpt_browser_bridge.py` 只负责底层页面读写，把持续守护、自动发送、发送确认和停机状态集中到 `scripts/chatgpt_heartbeat_runner.py`。文档真源同步更新到 `docs/engineering/`，测试优先覆盖守护状态机与桥接模式契约。

**Tech Stack:** `Python（编程语言）`、`unittest（Python 单元测试框架）`、`argparse（命令行参数解析库）`、`Google Chrome（谷歌浏览器）` `AppleScript（苹果脚本）` 桥接

---

> 来源设计：`docs/engineering/chatgpt-browser-bridge-capability.md`

### Task 1: 更新正式文档与技能契约

**Files:**
- Modify: `docs/engineering/chatgpt-browser-bridge-capability.md`
- Modify: `docs/engineering/development-manual.md`
- Modify: `ai/README.md`
- Modify: `docs/engineering/current-delivery-status.md`
- Modify: `ai/skills/chatgpt-browser-bridge/SKILL.md`

- [ ] **Step 1: 补 skill 文案中的守护模式定义**

```md
## Default Mode

1. 只要用户是在“和浏览器里的 GPT 持续讨论方案 / 持续协商细节 / 盯着回复继续推进”的语境下触发本技能，默认行为就是持续盯守当前 `ChatGPT（对话式人工智能产品）` 会话，而不是只做一次 `read` 或只发一条消息就停。
2. 当用户已明确授权“继续讨论 / 持续盯守 / 自动推进”后，后续跟进消息视为已授权直接发送，不再逐轮回到终端等待人工确认。
3. 即使处于自动推进模式，也必须在桥接失败、发送失败、发送未确认、重复回路保护触发或达到强制上限时停机。
```

- [ ] **Step 2: 校验文档引用命中**

Run: `rg -n "持续盯守|自动跟进|重复回路|发送未确认" docs/engineering/chatgpt-browser-bridge-capability.md docs/engineering/development-manual.md ai/README.md ai/skills/chatgpt-browser-bridge/SKILL.md docs/engineering/current-delivery-status.md`
Expected: 命中能力文档、开发手册、AI 资产入口、skill 文案与交付状态

### Task 2: 先补失败测试锁定守护模式边界

**Files:**
- Modify: `scripts/tests/test_chatgpt_heartbeat_runner.py`
- Modify: `scripts/tests/test_chatgpt_browser_bridge.py`

- [ ] **Step 1: 为守护脚本写失败测试，锁定重复回路保护**

```python
    def test_should_stop_for_duplicate_reply_loop(self):
        previous_state = {
            "last_sent_text": "请补充验收条件",
            "loop_guard_hits": 1,
        }
        self.assertTrue(
            self.module.should_stop_for_loop_guard(
                previous_state,
                "请补充验收条件",
                similarity_threshold=0.95,
                max_hits=2,
            )
        )
```

- [ ] **Step 2: 为守护脚本写失败测试，锁定连续失败停机**

```python
    def test_mark_send_failure_increments_consecutive_failures(self):
        state = self.module.mark_send_failure(
            {"consecutive_failures": 1},
            "send_unconfirmed",
            max_failures=2,
        )
        self.assertEqual(state["consecutive_failures"], 2)
        self.assertEqual(state["runner_state"], "stalled_max_failures")
```

- [ ] **Step 3: 为 skill / 桥接契约写失败测试，锁定自动推进授权文案**

```python
    def test_type_requires_text(self):
        module = load_module()
        exit_code, payload = module.dispatch(["type"])
        self.assertEqual(exit_code, module.EXIT_ENVIRONMENT)
        self.assertEqual(payload["error"]["code"], "TEXT_REQUIRED")
```

- [ ] **Step 4: 运行脚本测试，确认先失败**

Run: `python3 -m unittest scripts.tests.test_chatgpt_heartbeat_runner scripts.tests.test_chatgpt_browser_bridge`
Expected: FAIL，提示 `should_stop_for_loop_guard` / `mark_send_failure` 尚未实现

### Task 3: 实现守护模式的状态与停机护栏

**Files:**
- Modify: `scripts/chatgpt_heartbeat_runner.py`

- [ ] **Step 1: 添加重复回路判断与失败计数辅助函数**

```python
def text_similarity(left: str, right: str) -> float:
    left_normalized = normalize_message_text(left)
    right_normalized = normalize_message_text(right)
    if not left_normalized and not right_normalized:
        return 1.0
    if not left_normalized or not right_normalized:
        return 0.0
    return SequenceMatcher(None, left_normalized, right_normalized).ratio()


def should_stop_for_loop_guard(
    previous_state: dict[str, Any],
    next_reply_text: str,
    *,
    similarity_threshold: float,
    max_hits: int,
) -> bool:
    similarity = text_similarity(previous_state.get("last_sent_text", ""), next_reply_text)
    hits = int(previous_state.get("loop_guard_hits", 0))
    return similarity >= similarity_threshold and hits + 1 >= max_hits
```

- [ ] **Step 2: 在发送前接入重复回路保护**

```python
    if should_stop_for_loop_guard(
        previous_state,
        reply_text,
        similarity_threshold=args.loop_similarity_threshold,
        max_hits=args.max_loop_guard_hits,
    ):
        state["runner_state"] = "stalled_loop_guard"
        state["last_stalled_reason"] = "loop_guard"
        state["loop_guard_hits"] = int(previous_state.get("loop_guard_hits", 0)) + 1
        save_json(state_path, state)
        append_log(log_path, "loop-guard-triggered")
        return state, False
```

- [ ] **Step 3: 在发送失败和发送未确认分支统一累计连续失败次数**

```python
def mark_send_failure(
    state: dict[str, Any],
    reason: str,
    *,
    max_failures: int,
) -> dict[str, Any]:
    next_state = dict(state)
    failures = int(next_state.get("consecutive_failures", 0)) + 1
    next_state["consecutive_failures"] = failures
    next_state["last_stalled_reason"] = reason
    next_state["runner_state"] = (
        "stalled_max_failures" if failures >= max_failures else "stalled_send_failed"
    )
    return next_state
```

- [ ] **Step 4: 成功发送后清空失败计数并刷新守护状态**

```python
    state["consecutive_failures"] = 0
    state["loop_guard_hits"] = 0
    state["runner_state"] = infer_runner_state(state, confirmed_tracking, args.stalled_after_seconds)
```

- [ ] **Step 5: 为新护栏开放最少参数**

```python
    parser.add_argument("--max-consecutive-failures", type=int, default=2)
    parser.add_argument("--loop-similarity-threshold", type=float, default=0.95)
    parser.add_argument("--max-loop-guard-hits", type=int, default=2)
```

- [ ] **Step 6: 运行脚本测试，确认转绿**

Run: `python3 -m unittest scripts.tests.test_chatgpt_heartbeat_runner scripts.tests.test_chatgpt_browser_bridge`
Expected: PASS

### Task 4: 收口 skill 行为与本机烟测说明

**Files:**
- Modify: `ai/skills/chatgpt-browser-bridge/SKILL.md`
- Modify: `docs/engineering/chatgpt-browser-bridge-capability.md`

- [ ] **Step 1: 在 skill 中写清单次模式与守护模式**

```md
## Guardrails

3. 默认不自动发送；没有显式发送意图时，只允许 `read` 或 `type`。但一旦用户明确要求“继续讨论 / 持续盯守 / 自动推进”，后续跟进消息视为已授权发送。
4. 守护模式仍然必须在桥接失败、发送失败、发送未确认、重复回路保护触发或达到强制上限时停机。
```

- [ ] **Step 2: 跑文档命中与脚本测试**

Run: `rg -n "自动推进|loop_guard|max-consecutive-failures|持续盯守" ai/skills/chatgpt-browser-bridge/SKILL.md docs/engineering/chatgpt-browser-bridge-capability.md scripts/chatgpt_heartbeat_runner.py scripts/tests/test_chatgpt_heartbeat_runner.py`
Expected: 命中 skill 文案、能力文档、守护脚本和脚本测试

- [ ] **Step 3: 本机烟测守护模式**

Run: `python3 scripts/chatgpt_heartbeat_runner.py --disable-codex --disable-codex-activate`
Expected: 若当前已打开 `ChatGPT（对话式人工智能产品）` 会话，可成功落 `latest_snapshot.json` 与 `state.json`；若桥接环境缺失，则返回稳定错误码并写日志，不发生重复发送
