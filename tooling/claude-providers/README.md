# Claude Providers

本目录只管理**本地 `Claude Code（代码智能体）` provider 配置**，用于在项目内切换：

1. 百炼 `API（应用程序编程接口，Application Programming Interface）`
2. 问问 `API（应用程序编程接口，Application Programming Interface）`

它不替代当前 Codex 会话本身的模型能力；`GPT-5.2 Codex`、`GPT-5.4` 等主模型仍由当前 Codex / 子代理体系承担。

## 文件说明

- `bailian.env.example`：百炼配置模板
- `wenwen.env.example`：问问配置模板
- `*.env.local`：本机私有配置，已加入 `.gitignore`
- [`scripts/claude_mux.sh`](../../scripts/claude_mux.sh)：统一 provider + model 启动入口

## 初始化

```bash
cp tooling/claude-providers/bailian.env.example tooling/claude-providers/bailian.env.local
cp tooling/claude-providers/wenwen.env.example tooling/claude-providers/wenwen.env.local
```

然后分别填入本机私有凭证。

## 使用方法

统一入口：

```bash
bash scripts/claude_mux.sh <provider> <model> [claude args...]
```

示例：

```bash
bash scripts/claude_mux.sh bailian qwen3-coder-plus --print "帮我梳理这段实现"
bash scripts/claude_mux.sh wenwen claude-sonnet-4-6-20260218 --print "评审这次改动"
```

快捷脚本：

- `bash scripts/claude-dev.sh`：百炼 `qwen3.5-plus`
- `bash scripts/claude-coder.sh`：百炼 `qwen3-coder-plus`
- `bash scripts/claude-plan.sh`：百炼 `glm-5`
- `bash scripts/claude_review.sh`：问问 `claude-sonnet-4-6-20260218`
- `bash scripts/claude-review-deep.sh`：问问 `claude-opus-4-6-20260205`

全局命令：

- `claude1`：百炼 `qwen3.5-plus`
- `claude2`：百炼 `qwen3-coder-plus`
- `claude3`：百炼 `glm-5`
- `claude4`：问问 `claude-sonnet-4-6-20260218`
- `claude5`：问问 `claude-opus-4-6-20260205`

这些命令安装在本机 `PATH` 目录 `/Users/rlc/.npm-global/bin`，适合在任意目录直接启动；底层仍统一转发到 [`scripts/claude_mux.sh`](../../scripts/claude_mux.sh)，因此依赖当前仓库路径与 `tooling/claude-providers/*.env.local` 私有配置。

全局命令示例：

```bash
claude2 --print "帮我梳理这段实现"
claude4 --print "评审这次改动"
```

## 模型使用约定

### Codex 自动化主工作流

这部分不是通过 `claude_mux.sh` 启动，而是当前 Codex / 子代理协作时的默认约定：

| 自动化环节 | 主运行时 / 模型 | 主推理强度 | 备选 | 备注 |
| --- | --- | --- | --- | --- |
| 补特性文档 | `Codex / gpt-5.4` | `xhigh` | `Claude Code / claude-sonnet-4-6-20260218` | 自动链路：`feature-doc-coverage-mapping -> feature-doc-authoring（authoring） -> feature-doc-authoring（gate）` |
| 特性文档评审 | `Claude Code / claude-sonnet-4-6-20260218` | 默认 | `Codex / gpt-5.4 + xhigh` | 优先 `review` |
| 写实施计划 | `Codex / gpt-5.4` | `xhigh` | `Claude Code / claude-sonnet-4-6-20260218` | 自动化允许直接生成 `docs/plans/*.md` |
| 实施计划复核 | `Claude Code / claude-sonnet-4-6-20260218` | 默认 | `Codex / gpt-5.4 + xhigh` | 优先 `review` |
| 推进开发计划 | `Codex / gpt-5.2-codex` | `high` | `Claude Code / qwen3-coder-plus` | 跨 `5` 个以上文件、连续失败、公共契约或核心状态机时升 `xhigh` |
| 任务间代码审查 / 整体代码检视 | `Claude Code / claude-sonnet-4-6-20260218` | 默认 | `Codex / gpt-5.4 + xhigh` | 优先 `review` skill |

### 本地 Claude Code provider 入口

本地 `Claude Code` 入口用于：

1. 使用百炼中的 `Qwen / GLM` 作为补位或独立只读会话
2. 使用问问中的 `Claude 4.6` 执行代码检视或强模型文档备选链路

常见回退入口：

- 代码检视主入口：`bash scripts/claude_review.sh`
- 编码备选入口：`bash scripts/claude-coder.sh`
- 强模型文档 / 计划备选入口：

```bash
bash scripts/claude_mux.sh wenwen claude-sonnet-4-6-20260218 --print "<prompt>"
```

- `bash scripts/claude-plan.sh` 仍可作为本地百炼 `GLM-5` 规划补位会话，但它不是当前自动化链路的主模型入口。

### 升降级规则

1. 主模型额度不足、超时或 provider 不可用时，立即切备选，不重复撞同一出口。
2. 结果不稳时，优先提升同模型推理强度，再切备选。
3. 代码检视与实现结论冲突时，复检必须换家。
4. 只有 `Codex` 可用模型才会直接用于 `spawn_agent`；`Claude Code` 与 `Qwen` 备选通过本地脚本调用，不直接传给 `spawn_agent`。

## 设计约束

- `claude_mux.sh` 统一追加 `--setting-sources project,local`
- 用户级 `~/.claude/settings.json` 不被改写
- 私有密钥不进仓库
