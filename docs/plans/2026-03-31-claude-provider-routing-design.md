# Claude Provider Routing Design

## 背景

当前项目内已经存在两类 `Claude Code（代码智能体）` 模型出口：

1. 百炼 `API（应用程序编程接口，Application Programming Interface）`
2. 问问 `API（应用程序编程接口，Application Programming Interface）`

现状问题有两个：

1. 默认 `claude` 已绑定百炼，但缺少稳定的“按 provider（模型提供方）+ model（模型名）”切换入口。
2. 代码检视已经接入单独的问问 `Claude 4.6` 启动脚本，但它还是单点脚本，不足以承载后续多模型、多额度、多备用链路的统一调度。

本轮目标不是替换现有默认配置，而是在当前仓库内新增一套**项目级统一入口**，让百炼和问问同时可用，并且把“主模型 / 备选模型 / 升降级触发条件”写成可执行约定。

## 目标

1. 保留用户级 `~/.claude/settings.json` 现状，不覆盖当前默认 `claude` 出口。
2. 在仓库内新增 `scripts/claude_mux.sh`，统一承载 provider 切换。
3. 为高频场景提供快捷脚本，减少手工记忆成本。
4. 真实密钥只保留在本机私有 `.env.local`，模板文件可以进仓库。
5. 明确模型使用约定，包括主模型、备选模型、推理强度与触发条件。

## 设计

### 1. 统一入口

新增 `scripts/claude_mux.sh`，固定调用方式：

```bash
bash scripts/claude_mux.sh <provider> <model> [claude args...]
```

运行逻辑：

1. 校验 `provider` 是否属于受支持列表：`bailian` / `wenwen`
2. 读取 `tooling/claude-providers/<provider>.env.local`
3. 将对应 provider 的环境变量注入当前进程
4. 统一追加 `--setting-sources project,local`
5. 统一执行 `claude --model <model> ...`

这条入口只影响当前进程，不写回用户级配置，也不篡改全局 `claude` 默认行为。

### 2. Provider 私有配置

目录统一收口为：

- `tooling/claude-providers/bailian.env.example`
- `tooling/claude-providers/wenwen.env.example`
- `tooling/claude-providers/bailian.env.local`
- `tooling/claude-providers/wenwen.env.local`

约定：

1. `.env.example` 进入仓库
2. `.env.local` 只保留在本机，并加入 `.gitignore`
3. 每个 provider 文件只定义一组基础连接信息，不写死具体业务场景模型
4. 百炼 provider 允许使用 `Auth Token（认证令牌）`，问问 provider 使用 `API Key（接口密钥）`

### 3. 快捷脚本

为了减少高频命令长度，提供以下包装脚本：

- `scripts/claude-dev.sh`
- `scripts/claude-coder.sh`
- `scripts/claude-plan.sh`
- `scripts/claude-review.sh`
- `scripts/claude-review-deep.sh`

这些脚本只负责把固定场景映射到 `claude_mux.sh`，不重复 provider 解析逻辑。注意：这些快捷脚本是**本地 Claude Code provider 入口**，不是当前 Codex 主工作流的全部模型入口。

### 4. 模型使用约定

#### Codex 主工作流

这部分约定用于当前 Codex 会话与子代理，不通过 `claude_mux.sh` 启动：

- 日常编码
  - 主模型：`gpt-5.2-codex`
  - 主推理强度：`high`
  - 备选模型：`qwen3-coder-plus`
- 代码检视
  - 主模型：`claude-sonnet-4-6-20260218`
  - 备选模型：`gpt-5.4`
  - 备选推理强度：`xhigh`
- 特性文档产出
  - 主模型：`gpt-5.4`
  - 主推理强度：`xhigh`
  - 备选模型：`claude-sonnet-4-6-20260218`
- 特性评审
  - 主模型：`claude-sonnet-4-6-20260218`
  - 备选模型：`gpt-5.4`
  - 备选推理强度：`xhigh`

#### 本地 Claude Code provider 入口

统一入口脚本只负责切换百炼 / 问问的本地 provider，并提供以下高频快捷方式：

- `claude-dev.sh` -> `bailian qwen3.5-plus`
- `claude-coder.sh` -> `bailian qwen3-coder-plus`
- `claude-plan.sh` -> `bailian glm-5`
- `claude_review.sh` -> `wenwen claude-sonnet-4-6-20260218`
- `claude-review-deep.sh` -> `wenwen claude-opus-4-6-20260205`

### 5. 升降级规则

1. 主模型超时、额度不足或 provider 不可用时，立即切备选，不重复撞同一出口。
2. 主模型结果不稳时，优先提升同模型推理强度，再切备选。
3. 代码检视若与实现模型结论冲突，复检必须换家，不使用同 provider 重试。

## 文档落点

这类约定属于项目级工具链和协作执行约束，不放到 `AGENTS.md` 的最高规则层。仓库内只保留：

1. 启动脚本
2. 私有配置模板
3. 工具说明文档
4. 交付状态中的一条事实记录

## 验收标准

1. `bailian` 与 `wenwen` 均可通过统一入口启动
2. 快捷脚本都能落到正确的 provider 与 model
3. 私有密钥不会出现在可提交文件中
4. 文档明确说明主模型、备选模型和使用方法
