#!/usr/bin/env python3
"""Conservative Chinese localization for AI skill/agent descriptors.

This script intentionally updates only two areas:
1. YAML frontmatter `description`
2. The first markdown H1 heading

It leaves file paths, slugs, code blocks, inline code, and all other content
untouched. The goal is to improve readability without breaking discovery or
tooling that depends on stable English identifiers.
"""

from __future__ import annotations

import argparse
import dataclasses
import re
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
SKILL_GLOB = ROOT / "ai" / "skills"
AGENT_GLOB = ROOT / "ai" / "agents"

H1_RE = re.compile(r"^#\s+(.+)$", re.M)
CJK_RE = re.compile(r"[\u4e00-\u9fff]")
ASCII_RE = re.compile(r"[A-Za-z]")


TOKEN_MAP = {
    "agent": "智能体",
    "agentic": "智能体式",
    "ai": "AI",
    "android": "Android",
    "api": "API",
    "article": "文章",
    "architecture": "架构",
    "autonomous": "自主",
    "backend": "后端",
    "blueprint": "蓝图",
    "bun": "Bun",
    "cache": "缓存",
    "caliber": "口径",
    "carrier": "承运商",
    "change": "变更",
    "claude": "Claude",
    "clean": "整洁",
    "clickhouse": "ClickHouse",
    "coding": "编码",
    "compact": "压缩",
    "compliance": "合规",
    "compose": "Compose",
    "construction": "构建",
    "content": "内容",
    "continuous": "持续",
    "coroutines": "协程",
    "cost": "成本",
    "cpp": "C++",
    "crosspost": "跨平台分发",
    "customs": "关务",
    "data": "数据",
    "database": "数据库",
    "deep": "深度",
    "demand": "需求",
    "deployment": "部署",
    "design": "设计",
    "devfleet": "DevFleet",
    "django": "Django",
    "di": "依赖注入",
    "dmux": "dmux",
    "doc": "文档",
    "docker": "Docker",
    "documentation": "文档",
    "drawio": "Draw.io",
    "e2e": "E2E",
    "ecc": "ECC",
    "energy": "能源",
    "engineering": "工程",
    "enterprise": "企业级",
    "eval": "评测",
    "exa": "Exa",
    "exception": "异常",
    "exposed": "Exposed",
    "fal": "fal.ai",
    "first": "优先",
    "flows": "Flow",
    "foundation": "基础模型",
    "frontend": "前端",
    "glass": "玻璃",
    "go": "Go",
    "golang": "Go",
    "governance": "治理",
    "guidelines": "指南",
    "harness": "执行器",
    "hash": "哈希",
    "inventory": "库存",
    "investor": "投资人",
    "io": "I/O",
    "iterative": "迭代",
    "java": "Java",
    "jpa": "JPA",
    "ktor": "Ktor",
    "kotlin": "Kotlin",
    "laravel": "Laravel",
    "learning": "学习",
    "liquid": "液态",
    "llm": "LLM",
    "logistics": "物流",
    "loop": "循环",
    "loops": "循环",
    "management": "管理",
    "market": "市场",
    "materials": "材料",
    "mcp": "MCP",
    "media": "媒体",
    "migrations": "迁移",
    "models": "模型",
    "multiplatform": "多平台",
    "nanoclaw": "NanoClaw",
    "nextjs": "Next.js",
    "nonconformance": "不合格",
    "nutrient": "Nutrient",
    "on": "端侧",
    "ops": "运维",
    "optimizer": "优化",
    "organizer": "整理",
    "outreach": "外联",
    "patterns": "模式",
    "perl": "Perl",
    "persistence": "持久化",
    "planning": "规划",
    "plankton": "Plankton",
    "postgres": "PostgreSQL",
    "procurement": "采购",
    "processing": "处理",
    "production": "生产",
    "project": "项目",
    "prompt": "提示词",
    "protocol": "协议",
    "python": "Python",
    "quality": "质量",
    "ralphinho": "Ralphinho",
    "redteam": "红队",
    "regex": "正则",
    "relationship": "关系",
    "research": "研究",
    "retrieval": "检索",
    "returns": "退货",
    "reverse": "逆向",
    "review": "评审",
    "runtime": "运行时",
    "rust": "Rust",
    "scan": "扫描",
    "scraper": "采集",
    "search": "搜索",
    "security": "安全",
    "server": "服务端",
    "skills": "技能",
    "scheduling": "排程",
    "springboot": "Spring Boot",
    "standards": "规范",
    "stocktake": "盘点",
    "strategic": "战略性",
    "structured": "结构化",
    "swift": "Swift",
    "swiftui": "SwiftUI",
    "tdd": "TDD",
    "team": "团队",
    "testing": "测试",
    "text": "文本",
    "translate": "翻译",
    "trade": "贸易",
    "turbopack": "Turbopack",
    "update": "更新",
    "verification": "验证",
    "video": "视频",
    "videodb": "VideoDB",
    "visa": "签证",
    "workflow": "工作流",
    "workflows": "工作流",
    "writing": "写作",
    "x": "X",
}


AGENT_TITLE_MAP = {
    "architect": "架构设计专家",
    "build-error-resolver": "构建错误修复器",
    "chief-of-staff": "个人幕僚长",
    "code-reviewer": "代码评审专家",
    "cpp-build-resolver": "C++ 构建错误修复器",
    "cpp-reviewer": "C++ 代码评审专家",
    "database-reviewer": "数据库评审专家",
    "doc-updater": "文档与 Codemap 更新专家",
    "docs-lookup": "文档检索专家",
    "e2e-runner": "E2E 测试执行器",
    "go-build-resolver": "Go 构建错误修复器",
    "go-reviewer": "Go 代码评审专家",
    "harness-optimizer": "智能体执行器优化专家",
    "java-build-resolver": "Java 构建错误修复器",
    "java-reviewer": "Java 代码评审专家",
    "kotlin-build-resolver": "Kotlin 构建错误修复器",
    "kotlin-reviewer": "Kotlin 代码评审专家",
    "loop-operator": "循环运维专家",
    "planner": "规划专家",
    "python-reviewer": "Python 代码评审专家",
    "refactor-cleaner": "重构与死代码清理专家",
    "rust-build-resolver": "Rust 构建错误修复器",
    "rust-reviewer": "Rust 代码评审专家",
    "security-reviewer": "安全评审专家",
    "tdd-guide": "TDD 指导专家",
}

AGENT_DESC_MAP = {
    "architect": "软件架构与技术决策专家。用于新功能规划、大型重构、系统扩展性分析和架构取舍。",
    "build-error-resolver": "构建与 TypeScript 错误修复专家。用于快速定位并以最小改动修复构建失败和类型错误。",
    "chief-of-staff": "个人沟通幕僚代理。负责统一梳理邮件、Slack、LINE、Messenger 等多渠道沟通并生成后续动作。",
    "code-reviewer": "代码评审专家。用于在改动后主动检查质量、安全性、可维护性和潜在回归。",
    "cpp-build-resolver": "C++ 构建错误修复专家。用于处理 CMake、编译、链接和模板相关构建问题。",
    "cpp-reviewer": "C++ 代码评审专家。聚焦内存安全、现代 C++ 惯用法、并发与性能风险。",
    "database-reviewer": "数据库评审专家。聚焦 PostgreSQL 查询优化、模式设计、安全性与性能问题。",
    "doc-updater": "文档与 Codemap 更新专家。用于维护文档、生成 Codemap，并同步 README 与指南。",
    "docs-lookup": "文档检索专家。用于获取库、框架和 API 的最新官方文档与示例。",
    "e2e-runner": "端到端测试执行专家。用于生成、维护并运行 E2E 测试，优先使用浏览器代理，必要时回退到 Playwright。",
    "go-build-resolver": "Go 构建错误修复专家。用于处理构建失败、go vet 问题和编译告警。",
    "go-reviewer": "Go 代码评审专家。聚焦惯用 Go 写法、并发模式、错误处理与性能。",
    "harness-optimizer": "智能体执行器优化专家。用于改进本地 agent harness 的稳定性、成本和吞吐表现。",
    "java-build-resolver": "Java 构建错误修复专家。用于处理 Java、Maven、Gradle 和 Spring Boot 构建问题。",
    "java-reviewer": "Java 代码评审专家。聚焦分层架构、JPA、安全性、并发和 Spring Boot 工程质量。",
    "kotlin-build-resolver": "Kotlin 构建错误修复专家。用于处理 Gradle、依赖和 Kotlin 编译问题。",
    "kotlin-reviewer": "Kotlin 代码评审专家。聚焦惯用写法、协程安全、Compose 实践和 Android/KMP 风险。",
    "loop-operator": "自主循环运维专家。用于监控智能体循环任务，并在停滞或异常时安全介入。",
    "planner": "规划专家。用于复杂功能、架构调整和重构工作的实施计划设计。",
    "python-reviewer": "Python 代码评审专家。聚焦 Pythonic 写法、类型标注、安全性和性能。",
    "refactor-cleaner": "重构与死代码清理专家。用于识别重复、无用代码和可安全收敛的重构点。",
    "rust-build-resolver": "Rust 构建错误修复专家。用于处理 cargo 构建失败、借用检查和依赖问题。",
    "rust-reviewer": "Rust 代码评审专家。聚焦所有权、生命周期、错误处理、unsafe 使用和惯用模式。",
    "security-reviewer": "安全评审专家。用于识别输入处理、认证、接口和敏感数据相关的安全风险。",
    "tdd-guide": "TDD 指导专家。用于推动测试先行的开发方式，并确保改动具备充分覆盖。",
}

SKILL_TITLE_OVERRIDES = {
    "agent-harness-construction": "智能体执行器构建",
    "agentic-engineering": "智能体式工程",
    "ai-first-engineering": "AI 优先工程",
    "ai-regression-testing": "AI 回归测试",
    "api-design": "API 设计",
    "article-writing": "文章写作",
    "autonomous-loops": "自主循环架构",
    "blueprint": "蓝图规划",
    "bun-runtime": "Bun 运行时",
    "caliber-redteam-review": "口径红队评审",
    "change-review": "变更评审",
    "claude-api": "Claude API",
    "claude-devfleet": "Claude DevFleet 编排",
    "configure-ecc": "ECC 配置",
    "content-engine": "内容引擎",
    "continuous-agent-loop": "持续智能体循环",
    "continuous-learning": "持续学习",
    "continuous-learning-v2": "持续学习 v2",
    "cost-aware-llm-pipeline": "成本感知 LLM 管线",
    "crosspost": "跨平台分发",
    "data-scraper-agent": "数据采集智能体",
    "database-migrations": "数据库迁移",
    "deep-research": "深度研究",
    "dmux-workflows": "dmux 工作流",
    "doc-organizer": "文档整理器",
    "documentation-lookup": "文档查询",
    "drawio": "Draw.io 图示",
    "enterprise-agent-ops": "企业级智能体运维",
    "eval-harness": "评测框架",
    "exa-search": "Exa 搜索",
    "fal-ai-media": "fal.ai 媒体生成",
    "frontend-design-governance": "前端设计治理",
    "frontend-slides": "前端演示文稿",
    "investor-materials": "投资材料",
    "investor-outreach": "投资人外联",
    "iterative-retrieval": "迭代检索",
    "liquid-glass-design": "Liquid Glass 设计",
    "market-research": "市场研究",
    "mcp-server-patterns": "MCP Server 模式",
    "nanoclaw-repl": "NanoClaw REPL",
    "nextjs-turbopack": "Next.js Turbopack",
    "nutrient-document-processing": "Nutrient 文档处理",
    "plankton-code-quality": "Plankton 代码质量",
    "project-guidelines-example": "项目指南示例",
    "prompt-optimizer": "提示词优化",
    "ralphinho-rfc-pipeline": "Ralphinho RFC 管线",
    "regex-vs-llm-structured-text": "正则与 LLM 结构化文本",
    "search-first": "先搜索后实现",
    "security-review": "安全评审",
    "security-scan": "安全扫描",
    "skill-stocktake": "技能盘点",
    "strategic-compact": "战略性压缩",
    "tdd-workflow": "TDD 工作流",
    "team-builder": "团队构建器",
    "update-project-docs": "更新项目文档",
    "verification-loop": "验证闭环",
    "video-editing": "视频编辑",
    "videodb": "VideoDB",
    "visa-doc-translate": "签证文档翻译",
    "x-api": "X API",
}

SKILL_DESC_OVERRIDES = {
    "agent-harness-construction": "用于设计和优化智能体执行器的动作空间、工具定义与观测格式，以提升任务完成率。",
    "agentic-engineering": "提供智能体式工程执行方法，包括评测优先、任务拆解与成本感知模型路由。",
    "ai-first-engineering": "总结 AI 优先团队的工程协作模式与落地方法。",
    "ai-regression-testing": "提供 AI 辅助开发场景下的回归测试策略与质量防护方法。",
    "api-design": "提供生产级 REST API 的设计模式、契约约定与常见取舍。",
    "article-writing": "用于生成长篇文章、指南、教程和其他需要稳定文风的写作内容。",
    "autonomous-loops": "总结从简单串行流程到 RFC 驱动 DAG 的自主智能体循环架构与模式。",
    "blueprint": "将目标拆解为多阶段、多会话、多代理可执行蓝图的规划技能。",
    "bun-runtime": "提供 Bun 作为运行时、包管理器、构建器和测试器的使用建议。",
    "caliber-redteam-review": "已停用的口径红队评审技能，保留为历史存档说明。",
    "change-review": "用于只读评审现有改动，优先识别回归风险、契约漂移、缺失验证与缺失文档。",
    "claude-api": "提供 Anthropic Claude API 的使用模式、流式处理、工具调用和 SDK 实践。",
    "claude-devfleet": "用于通过 Claude DevFleet 编排多智能体编码任务与并行执行。",
    "configure-ecc": "用于引导安装和配置 Everything Claude Code 技能与规则集。",
    "content-engine": "用于构建适配不同平台的内容生产体系与内容复用流程。",
    "continuous-agent-loop": "提供持续自主智能体循环的质量门、评测与恢复控制模式。",
    "continuous-learning": "用于从 Claude Code 会话中提取可复用模式并沉淀为新技能。",
    "continuous-learning-v2": "提供基于观察与置信度评分的持续学习机制，并支持项目级隔离。",
    "cost-aware-llm-pipeline": "用于按任务复杂度进行 LLM 路由、预算控制、重试与缓存优化。",
    "crosspost": "用于将内容按平台特性改写后分发到多个社交平台。",
    "data-scraper-agent": "用于构建自动化公共数据采集智能体，并支持定时运行与反馈学习。",
    "database-migrations": "提供数据库 schema 变更、数据迁移、回滚和零停机迁移实践。",
    "deep-research": "用于进行多来源深度研究、证据整合和带引用的研究输出。",
    "dmux-workflows": "提供使用 dmux 协调多智能体并行会话的工作流模式。",
    "doc-organizer": "用于文档编写、整理、去重、迁移和信息架构优化。",
    "documentation-lookup": "通过最新官方文档检索结果回答库、框架和 API 的使用问题。",
    "drawio": "用于创建、编辑和复刻 Draw.io 图示，并支持设计系统化约束。",
    "enterprise-agent-ops": "提供长生命周期智能体任务的运维、观测、安全边界与生命周期管理实践。",
    "eval-harness": "提供面向 Claude Code 会话的正式评测框架与评测驱动开发方法。",
    "exa-search": "提供基于 Exa 的网页、代码、公司与人物神经搜索工作流。",
    "fal-ai-media": "通过 fal.ai 统一处理图像、视频和音频生成任务。",
    "frontend-design-governance": "用于约束数据直通车前端页面、导航、中文界面口径与设计留痕。",
    "frontend-slides": "用于创建动画丰富的 HTML 演示文稿或将 PPT 转换为网页演示。",
    "investor-materials": "用于制作和更新融资相关的演示稿、备忘录和财务材料。",
    "investor-outreach": "用于撰写面向投资人的冷启动外联、跟进和更新沟通内容。",
    "iterative-retrieval": "提供逐步收敛上下文的检索方法，用于解决子代理上下文不足问题。",
    "liquid-glass-design": "提供 iOS 26 Liquid Glass 设计系统的视觉与实现模式。",
    "market-research": "用于执行市场研究、竞品分析和带来源引用的商业调研。",
    "mcp-server-patterns": "提供基于 Node/TypeScript SDK 构建 MCP Server 的模式与实践。",
    "nanoclaw-repl": "用于操作和扩展 NanoClaw v2 这一零依赖、会话感知的 REPL。",
    "nextjs-turbopack": "提供 Next.js 16+ 与 Turbopack 的使用建议、缓存机制和取舍说明。",
    "nutrient-document-processing": "用于通过 Nutrient DWS API 处理、转换、OCR、签署和填写文档。",
    "plankton-code-quality": "提供通过 Plankton 在写入时自动执行格式化、lint 和质量修复的模式。",
    "project-guidelines-example": "提供一个基于真实项目的项目级技能模板示例。",
    "prompt-optimizer": "用于分析原始提示词、识别缺口并输出可直接使用的优化版提示词。",
    "ralphinho-rfc-pipeline": "提供 RFC 驱动、多智能体 DAG 执行与质量门编排模式。",
    "regex-vs-llm-structured-text": "提供在结构化文本处理中选择正则或 LLM 的决策框架。",
    "search-first": "提供先调研再编码的工作流，用于先找现成工具、模式和实现经验。",
    "security-review": "用于在认证、输入处理、接口和敏感能力开发时执行安全评审。",
    "security-scan": "用于扫描 Claude Code 配置中的安全风险、误配置和注入问题。",
    "skill-stocktake": "用于盘点并评估已安装技能与命令的质量状态。",
    "strategic-compact": "用于在合适阶段建议人工执行上下文压缩，而不是任意自动压缩。",
    "tdd-workflow": "提供测试驱动开发流程，并强调单元、集成和端到端覆盖。",
    "team-builder": "用于浏览、挑选并组合代理团队，再并行派发任务。",
    "update-project-docs": "用于在方案或规则变化后同步项目文档、术语表和导航入口。",
    "verification-loop": "提供 Claude Code 会话级的完整验证闭环。",
    "video-editing": "提供 AI 辅助视频剪辑、结构整理和增强制作流程。",
    "videodb": "用于理解、检索、编辑和处理视频与音频内容。",
    "visa-doc-translate": "用于将签证申请材料翻译成英文并生成双语 PDF。",
    "x-api": "提供 X/Twitter API 的发帖、时间线、搜索与分析集成方法。",
}


SKILL_SUFFIX_RULES = [
    ("-coding-standards", "编码规范", "提供{domain}的编码规范、工程约定与实践建议。"),
    ("-clean-architecture", "整洁架构", "提供{domain}的整洁架构分层、边界设计与实现模式。"),
    ("-patterns", "模式", "提供{domain}的开发模式、工程约定与最佳实践。"),
    ("-testing", "测试", "提供{domain}的测试策略、工具用法与验证实践。"),
    ("-security", "安全", "提供{domain}的安全最佳实践与防护要点。"),
    ("-verification", "验证", "提供{domain}的验证闭环、发布前检查与质量门禁。"),
    ("-tdd", "TDD", "提供{domain}的测试驱动开发流程与实践。"),
    ("-design", "设计", "提供{domain}的设计原则、实现约束与实践方法。"),
    ("-workflows", "工作流", "提供{domain}相关的工作流模式与执行方法。"),
    ("-workflow", "工作流", "提供{domain}相关的工作流模式与执行方法。"),
]


@dataclasses.dataclass
class UpdateResult:
    path: Path
    changed: bool
    description_changed: bool
    heading_changed: bool


def has_cjk(text: str) -> bool:
    return bool(CJK_RE.search(text))


def needs_chinese_localization(text: str) -> bool:
    cjk_count = len(CJK_RE.findall(text))
    ascii_count = len(ASCII_RE.findall(text))
    if cjk_count == 0:
        return True
    return ascii_count > cjk_count * 2


def escape_yaml_double(text: str) -> str:
    return text.replace("\\", "\\\\").replace('"', '\\"')


def dequote(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and ((value[0] == value[-1] == '"') or (value[0] == value[-1] == "'")):
        return value[1:-1]
    return value


def split_frontmatter(text: str) -> tuple[list[str] | None, list[str]]:
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return None, lines
    try:
        end_idx = next(i for i in range(1, len(lines)) if lines[i].strip() == "---")
    except StopIteration:
        return None, lines
    return lines[1:end_idx], lines[end_idx + 1 :]


def get_frontmatter_value(frontmatter: list[str], key: str) -> str | None:
    for idx, line in enumerate(frontmatter):
        if not line.startswith(f"{key}:"):
            continue
        raw = line.split(":", 1)[1].strip()
        if raw in {">", ">-", "|", "|-"}:
            values: list[str] = []
            for next_line in frontmatter[idx + 1 :]:
                if next_line.startswith("  "):
                    values.append(next_line[2:])
                elif not next_line.strip():
                    values.append("")
                else:
                    break
            return " ".join(v.strip() for v in values if v.strip()).strip()
        return dequote(raw)
    return None


def replace_frontmatter_description(frontmatter: list[str], new_desc: str) -> tuple[list[str], bool]:
    new_lines: list[str] = []
    i = 0
    changed = False
    while i < len(frontmatter):
        line = frontmatter[i]
        if not line.startswith("description:"):
            new_lines.append(line)
            i += 1
            continue

        current = get_frontmatter_value(frontmatter, "description") or ""
        if current == new_desc:
            return frontmatter, False

        new_lines.append(f'description: "{escape_yaml_double(new_desc)}"')
        changed = True
        i += 1

        raw = line.split(":", 1)[1].strip()
        if raw in {">", ">-", "|", "|-"}:
            while i < len(frontmatter):
                next_line = frontmatter[i]
                if next_line.startswith("  ") or not next_line.strip():
                    i += 1
                    continue
                break
        break

    new_lines.extend(frontmatter[i:])
    return new_lines, changed


def replace_or_insert_h1(body_lines: list[str], new_title: str) -> tuple[list[str], bool]:
    expected_line = f"# {new_title}"
    for idx, line in enumerate(body_lines):
        if line.startswith("# "):
            if line == expected_line and idx <= 2:
                return body_lines, False

            new_body = body_lines[:]
            new_body.pop(idx)

            insert_at = 0
            while insert_at < len(new_body) and not new_body[insert_at].strip():
                insert_at += 1
            new_body[insert_at:insert_at] = [expected_line, ""]
            return new_body, True

    insert_at = 0
    while insert_at < len(body_lines) and not body_lines[insert_at].strip():
        insert_at += 1
    prefix = body_lines[:insert_at]
    suffix = body_lines[insert_at:]
    new_body = prefix + [expected_line, ""] + suffix
    return new_body, True


def slug_to_cn_phrase(slug: str) -> str:
    pieces: list[str] = []
    for token in slug.split("-"):
        pieces.append(TOKEN_MAP.get(token, token))
    phrase = "".join(pieces).strip()
    return phrase or slug


def title_for_skill(slug: str) -> str:
    if slug in SKILL_TITLE_OVERRIDES:
        return f"{SKILL_TITLE_OVERRIDES[slug]}（{slug}）"
    for suffix, label, _ in SKILL_SUFFIX_RULES:
        if slug.endswith(suffix):
            domain_slug = slug[: -len(suffix)]
            domain = slug_to_cn_phrase(domain_slug)
            return f"{domain}{label}（{slug}）"
    return f"{slug_to_cn_phrase(slug)}技能（{slug}）"


def description_for_skill(slug: str) -> str:
    if slug in SKILL_DESC_OVERRIDES:
        return SKILL_DESC_OVERRIDES[slug]
    for suffix, _, template in SKILL_SUFFIX_RULES:
        if slug.endswith(suffix):
            domain_slug = slug[: -len(suffix)]
            domain = slug_to_cn_phrase(domain_slug)
            return template.format(domain=domain)
    title = SKILL_TITLE_OVERRIDES.get(slug, slug_to_cn_phrase(slug))
    return f"提供{title}相关的方法、流程与实践说明。"


def title_for_agent(slug: str) -> str:
    title = AGENT_TITLE_MAP.get(slug, f"{slug_to_cn_phrase(slug)}代理")
    return f"{title}（{slug}）"


def description_for_agent(slug: str) -> str:
    return AGENT_DESC_MAP.get(slug, f"{AGENT_TITLE_MAP.get(slug, slug_to_cn_phrase(slug))}。用于相关任务的分析、评审或问题处理。")


def iter_targets() -> Iterable[tuple[Path, str, str]]:
    for path in sorted(SKILL_GLOB.glob("*/SKILL.md")):
        yield path, "skill", path.parent.name
    for path in sorted(AGENT_GLOB.glob("*.md")):
        if path.name == "AGENTS.md":
            continue
        yield path, "agent", path.stem


def update_file(path: Path, kind: str, slug: str, apply_changes: bool) -> UpdateResult:
    original = path.read_text(encoding="utf-8")
    frontmatter, body = split_frontmatter(original)
    if frontmatter is None:
        raise ValueError(f"{path} does not start with valid frontmatter")

    current_description = get_frontmatter_value(frontmatter, "description") or ""
    target_description = description_for_skill(slug) if kind == "skill" else description_for_agent(slug)
    target_title = title_for_skill(slug) if kind == "skill" else title_for_agent(slug)

    description_changed = False
    heading_changed = False

    new_frontmatter = frontmatter
    if needs_chinese_localization(current_description):
        new_frontmatter, description_changed = replace_frontmatter_description(frontmatter, target_description)

    new_body = body
    new_body, heading_changed = replace_or_insert_h1(body, target_title)

    changed = description_changed or heading_changed
    if apply_changes and changed:
        rebuilt = ["---", *new_frontmatter, "---", *new_body]
        path.write_text("\n".join(rebuilt) + "\n", encoding="utf-8")
    return UpdateResult(path, changed, description_changed, heading_changed)


def verify_preserved_fields(path: Path, before: str, after: str) -> list[str]:
    issues: list[str] = []
    for key in ("name", "tools", "model", "origin"):
        before_frontmatter, _ = split_frontmatter(before)
        after_frontmatter, _ = split_frontmatter(after)
        before_value = get_frontmatter_value(before_frontmatter or [], key)
        after_value = get_frontmatter_value(after_frontmatter or [], key)
        if before_value != after_value:
            issues.append(f"{path}: preserved field changed -> {key}")
    return issues


def main() -> int:
    parser = argparse.ArgumentParser(description="Localize AI skill/agent descriptors into Chinese.")
    parser.add_argument("--apply", action="store_true", help="Write changes back to files.")
    args = parser.parse_args()

    before_map: dict[Path, str] = {}
    for path, _, _ in iter_targets():
        before_map[path] = path.read_text(encoding="utf-8")

    results = [update_file(path, kind, slug, apply_changes=args.apply) for path, kind, slug in iter_targets()]

    changed = [r for r in results if r.changed]
    desc_count = sum(1 for r in changed if r.description_changed)
    heading_count = sum(1 for r in changed if r.heading_changed)

    issues: list[str] = []
    if args.apply:
        for path, before in before_map.items():
            after = path.read_text(encoding="utf-8")
            issues.extend(verify_preserved_fields(path, before, after))

    print(f"targets={len(results)} changed={len(changed)} description_updates={desc_count} heading_updates={heading_count}")
    for result in changed:
        changed_parts = []
        if result.description_changed:
            changed_parts.append("description")
        if result.heading_changed:
            changed_parts.append("h1")
        print(f"{result.path.relative_to(ROOT)} -> {','.join(changed_parts)}")

    if issues:
        print("verification_failed")
        for issue in issues:
            print(issue)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
