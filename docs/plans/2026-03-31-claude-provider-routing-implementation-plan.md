# Claude Provider Routing Implementation Plan

> **For agentic workers:** 默认按 `subagent-driven-development（子代理驱动开发技能）` 执行；进入实现前先跑 `test-driven-development（测试驱动开发技能）`，实现完成后必须走 `code-reviewing（代码检视技能）` 与 `feature-test-report（特性测试与验收技能）`。

**Goal:** 为项目内 `Claude Code（代码智能体）` 提供统一的 provider + model 切换入口，同时保留百炼默认开发出口，并补齐问问 `Claude 4.6` 代码检视快捷入口与模型使用说明。

**Architecture:** 采用“一个多路由脚本 + 多个场景包装脚本 + 双 provider 私有配置模板”的结构。`scripts/claude_mux.sh` 负责 provider 解析、环境变量注入与 `claude` 启动，快捷脚本只负责固定场景映射；工具文档承担模型使用约定与启动说明。

**Tech Stack:** `bash（命令行脚本语言，Bash）`、`Claude Code（代码智能体）`、项目级 `.claude/settings.local.json`、本机私有 `.env.local`

---

### Task 1: 先写统一入口的失败测试

**Files:**
- Modify: `scripts/tests/test_claude_review.sh`
- Test: `scripts/tests/test_claude_review.sh`

- [ ] **Step 1: 扩展测试，先要求 `claude_mux.sh` 存在并支持 `bailian` / `wenwen`**

在 `scripts/tests/test_claude_review.sh` 中新增两组断言：

```bash
test -x "$ROOT_DIR/scripts/claude_mux.sh"
PATH="$FAKE_BIN_DIR:$PATH" \
CLAUDE_CAPTURE_FILE="$CAPTURE_FILE" \
CLAUDE_PROVIDER_DIR="$TMP_DIR/providers" \
"$ROOT_DIR/scripts/claude_mux.sh" bailian gpt-5.2-codex --print "dev prompt"
grep -Fq 'ANTHROPIC_BASE_URL=https://bailian.example.invalid' "$CAPTURE_FILE"
grep -Fq 'ARG=gpt-5.2-codex' "$CAPTURE_FILE"
```

- [ ] **Step 2: 运行测试，确认当前失败**

Run: `bash scripts/tests/test_claude_review.sh`

Expected: FAIL，提示 `scripts/claude_mux.sh` 不存在或 provider 配置目录不存在。

### Task 2: 实现统一入口与快捷脚本

**Files:**
- Create: `scripts/claude_mux.sh`
- Create: `scripts/claude-dev.sh`
- Create: `scripts/claude-coder.sh`
- Create: `scripts/claude-plan.sh`
- Create: `scripts/claude-review-deep.sh`
- Modify: `scripts/claude_review.sh`

- [ ] **Step 1: 写 `claude_mux.sh` 最小实现**

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"
PROVIDER_DIR="${CLAUDE_PROVIDER_DIR:-$PROJECT_ROOT/tooling/claude-providers}"

PROVIDER="${1:-}"
MODEL="${2:-}"
shift 2 || true

case "$PROVIDER" in
  bailian|wenwen) ;;
  *) echo "Unsupported provider: $PROVIDER" >&2; exit 1 ;;
esac

ENV_FILE="$PROVIDER_DIR/$PROVIDER.env.local"
test -f "$ENV_FILE" || { echo "Provider config not found: $ENV_FILE" >&2; exit 1; }

set -a
. "$ENV_FILE"
set +a

export ANTHROPIC_API_KEY="$CLAUDE_PROVIDER_API_KEY"
export ANTHROPIC_BASE_URL="$CLAUDE_PROVIDER_BASE_URL"
export ANTHROPIC_MODEL="$MODEL"
unset ANTHROPIC_AUTH_TOKEN

exec claude --setting-sources "project,local" --model "$MODEL" "$@"
```

- [ ] **Step 2: 用包装脚本固定高频场景**

例如 `scripts/claude-dev.sh`：

```bash
#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec "$SCRIPT_DIR/claude_mux.sh" bailian gpt-5.2-codex "$@"
```

其他脚本映射如下：

- `claude-coder.sh` -> `bailian qwen3-coder-plus`
- `claude-plan.sh` -> `bailian glm-5`
- `claude_review.sh` -> `wenwen claude-sonnet-4-6-20260218`
- `claude-review-deep.sh` -> `wenwen claude-opus-4-6-20260205`

- [ ] **Step 3: 重新运行测试，确认通过**

Run: `bash scripts/tests/test_claude_review.sh`

Expected: PASS，无输出，退出码 `0`

### Task 3: 补齐 provider 模板与忽略规则

**Files:**
- Create: `tooling/claude-providers/README.md`
- Create: `tooling/claude-providers/bailian.env.example`
- Create: `tooling/claude-providers/wenwen.env.example`
- Modify: `.gitignore`
- Modify: `tooling/README.md`

- [ ] **Step 1: 新增模板文件**

`tooling/claude-providers/bailian.env.example`

```bash
CLAUDE_PROVIDER_AUTH_TOKEN=replace-with-bailian-auth-token
CLAUDE_PROVIDER_BASE_URL=https://coding.dashscope.aliyuncs.com/apps/anthropic
```

`tooling/claude-providers/wenwen.env.example`

```bash
CLAUDE_PROVIDER_API_KEY=replace-with-wenwen-key
CLAUDE_PROVIDER_BASE_URL=https://breakout.wenwen-ai.com
```

- [ ] **Step 2: 更新 `.gitignore`**

新增：

```gitignore
tooling/claude-providers/*.env.local
```

- [ ] **Step 3: 更新工具说明**

在 `tooling/README.md` 与 `tooling/claude-providers/README.md` 中写清：

1. 统一入口命令
2. 快捷脚本
3. 私有配置路径
4. Codex 主工作流模型约定与本地 Claude provider 快捷脚本边界

- [ ] **Step 4: 检查私有文件未进入版本控制**

Run: `git status --short --ignored tooling/claude-providers`

Expected: `.env.example` 为未跟踪或已跟踪文件，`.env.local` 显示为 `!!` ignored。

### Task 4: 同步项目状态与最终验证

**Files:**
- Modify: `docs/engineering/current-delivery-status.md`

- [ ] **Step 1: 在交付状态中补一条“Claude provider 路由与 review 入口治理”记录**

更新字段：

```text
当前状态：reviewing（评审验证中）或 done（完成）
最新完成：统一入口、快捷脚本、私有模板与使用约定已落地
下一动作：用新入口完成一次真实代码检视烟测
```

- [ ] **Step 2: 运行最终验证**

Run:

```bash
bash -n scripts/claude_mux.sh scripts/claude-dev.sh scripts/claude-coder.sh scripts/claude-plan.sh scripts/claude_review.sh scripts/claude-review-deep.sh
bash scripts/tests/test_claude_review.sh
bash scripts/claude_review.sh --help | sed -n '1,10p'
```

Expected:

1. 所有脚本语法检查通过
2. 测试通过
3. `claude_review.sh --help` 能正确透传到 `claude`
