#!/usr/bin/env python3
"""Generate challenge cards for caliber docs.

Modes:
1) Legacy mode:
   python3 skills/caliber-redteam-review/scripts/generate_challenge_cases.py \
     --out /tmp/challenge-r1.md

2) R2 matrix mode (recommended):
   python3 skills/caliber-redteam-review/scripts/generate_challenge_cases.py \
     --seed-file skills/caliber-redteam-review/references/seed-questions-100.md \
     --target-count 240 \
     --matrix 6x4x10 \
     --with-answer-direction \
     --with-fail-condition \
     --evidence-mode line_excerpt \
     --evidence-catalog-out skills/caliber-redteam-review/references/evidence-catalog-r2.md \
     --qid-stable true \
     --mapping-primary-unique true \
     --out-main '/tmp/challenge-r2-main.md' \
     --out-mapping '/tmp/challenge-r2-mapping.md'
"""

from __future__ import annotations

import argparse
import pathlib
import re
from collections import Counter
from dataclasses import dataclass
from typing import Dict, List, Optional, Set, Tuple


DEFAULT_SOURCES = [
    "research/source-materials/sql-samples/03-口径文档现状-零售客户信息查询.sql",
    "research/source-materials/sql-samples/04-口径文档现状-零售客户信息变更.sql",
    "research/source-materials/sql-samples/05-口径文档现状-代发明细查询.sql",
    "research/source-materials/01-当前业务侧的业务场景分类.md",
]

DIMENSIONS = [
    "语义歧义",
    "多跳路径",
    "复杂 SQL（结构化查询语言）",
    "治理门禁",
    "审计追溯",
    "评测稳定性",
]

SOURCE_LABELS = {
    DEFAULT_SOURCES[0]: "09-零售客户信息查询",
    DEFAULT_SOURCES[1]: "10-零售客户信息变更",
    DEFAULT_SOURCES[2]: "11-代发明细查询",
    DEFAULT_SOURCES[3]: "12-业务场景分类",
}

SOURCE_ANCHORS: Dict[str, List[str]] = {
    DEFAULT_SOURCES[0]: [
        "业务定义",
        "术语释义",
        "常见数据表",
        "查询户口信息场景",
        "历史开户路径",
        "1G换卡路径",
        "条件占位检查",
        "字段口径一致性",
        "时间边界",
        "敏感字段识别",
    ],
    DEFAULT_SOURCES[1]: [
        "开户行变更记录",
        "客户密码修改",
        "维护日志场景",
        "1G-2G分段",
        "任务实例链路",
        "维护字段明细",
        "来源系统码值",
        "操作类型码值",
        "缺失字段补全",
        "审计追踪补证",
    ],
    DEFAULT_SOURCES[2]: [
        "业务场景描述",
        "协议号查询",
        "批次明细关联",
        "历史表时间段",
        "数据不连续说明",
        "代发明细查询",
        "代发批次查询",
        "增值税场景",
        "空条件阻断",
        "跨表聚合去重",
    ],
    DEFAULT_SOURCES[3]: [
        "一级分类",
        "二级分类",
        "零售基础业务",
        "公司业务",
        "贷款及信用卡业务",
        "外汇及境外机构",
        "财富管理",
        "节点编码",
        "层级合理性",
        "版本管理",
    ],
}

SOURCE_ANCHOR_KEYWORDS: Dict[str, Dict[str, List[str]]] = {
    DEFAULT_SOURCES[0]: {
        "业务定义": ["业务定义", "借记户口", "财富账户", "存单种类", "存折种类"],
        "术语释义": ["术语释义", "卡片等级", "账户分类", "一卡通制卡模式"],
        "常见数据表": ["常见数据表", "涉及到的表名", "T03_CUST_ACT_INF_S", "CST_IDV_BAS_INF_S"],
        "查询户口信息场景": ["查询户口信息", "场景描述", "口径提供人", "SQL 语句"],
        "历史开户路径": ["历史开户", "1G开户信息", "VVCUIFP_LEAN", "20060101之前"],
        "1G换卡路径": ["1G换卡", "STACMB_VVSDCTP", "SPACNO", "SPPSBN"],
        "条件占位检查": ["TRIM(EAC_ID) = ''", "TRIM(CUIDNO)", "IN ('9999')", "参数值"],
        "字段口径一致性": ["SELECT DISTINCT", "客户号", "证件号", "CUST_ID", "EAC_ID"],
        "时间边界": ["20060101之前", "CURRENT_DATE - 1", "CURRENT_DATE - 2", "DW_SNSH_DT"],
        "敏感字段识别": ["CUIDNO", "CUPSWD", "CUPSQR", "证件号", "查询密码", "进帐密码"],
    },
    DEFAULT_SOURCES[1]: {
        "开户行变更记录": ["开户行变更记录", "BCA_APL_DAT", "NLU39_DEE_EAC_TRX_T", "CHG_NEW_BBK"],
        "客户密码修改": ["客户密码修改", "USUSRCHP", "LOG_OWN_CLT", "NLL53_ORTODNDW_F3PVLLOGP"],
        "维护日志场景": ["零售客户维护日志/维护记录", "维护日志", "维护记录", "场景描述"],
        "1G-2G分段": ["1G数据", "2G数据", "2008-2013", "2013年6月以后", "1192"],
        "任务实例链路": ["任务实例号", "PTTSKISTP", "TSK_INST_NBR", "TSK_NBR"],
        "维护字段明细": ["字段维护事件", "PTTSKFLDP", "MNT_DTL", "维护前后值", "INT_RES_VAL"],
        "来源系统码值": ["来源系统", "Src_Sys_Cd", "ADS", "API", "WAS:CIF"],
        "操作类型码值": ["操作类型代码", "Opr_Typ_Cd", "A:新增", "U:修改", "D:删除"],
        "缺失字段补全": ["未提供（待补充）", "中文表名：未提供", "结果字段：未提供", "注意事项：未提供"],
        "审计追踪补证": ["EVT_ID", "记录最后更新版本", "RCD_LATE_UPD_EDTN", "DW_ETL_DT", "审计"],
    },
    DEFAULT_SOURCES[2]: {
        "业务场景描述": ["业务场景描述", "代发数据", "协议号", "代发批次", "代发明细"],
        "协议号查询": ["Step 1", "查询代发协议号", "PROTOCOL_NBR", "AGR_ID", "CARD_NBR"],
        "批次明细关联": ["代发批次", "代发明细", "AGN_BCH_SEQ", "DTL_SEQ_NBR", "AUTO_PAY_ARG_ID"],
        "历史表时间段": ["数据日期范围", "20030923-20041231", "20040707-20071231", "20041213-20131129"],
        "数据不连续说明": ["数据不连续", "缺数", "SDEYDT", "历史库无数据"],
        "代发明细查询": ["Step 2", "代发明细", "T05_AGN_DTL", "OAGN_AGN_DTL_EVT", "TRX_DT"],
        "代发批次查询": ["Step 3", "代发批次", "T05_AGN_BCH_EVT", "AGN_BCH_SEQ", "TOT_CNT"],
        "增值税场景": ["代发增值税", "ADD_VAL_AMOUNT", "FEE_AMOUNT", "NLJ54_AGF_FEE_STD_T"],
        "空条件阻断": ["IN ()", "IN ('')", "TRIM(SDRBAC) = ''", "TRIM(EPXEACNBR) = ''"],
        "跨表聚合去重": ["GROUP BY", "ORDER BY", "SELECT DISTINCT", "UNION", "去重"],
    },
    DEFAULT_SOURCES[3]: {
        "一级分类": ["业务分类", "一、零售基础业务", "二、公司业务", "三、贷款及信用卡业务"],
        "二级分类": ["1.1", "2.1", "3.1", "4.1", "5.1"],
        "零售基础业务": ["零售基础业务", "1.1 普通个人业务", "1.2 资金划转业务"],
        "公司业务": ["二、公司业务", "2.4 代发业务", "2.5 代扣业务"],
        "贷款及信用卡业务": ["三、贷款及信用卡业务", "3.1 个人贷款业务", "3.2 信用卡业务"],
        "外汇及境外机构": ["四、外汇及境外机构", "4.1 个人外汇业务", "4.2 香港一卡通"],
        "财富管理": ["五、财富管理", "5.4 证券业务", "5.6 国债业务"],
        "节点编码": ["1.1.2.5", "2.4.4", "1.2.21", "4.2", "5.4"],
        "层级合理性": ["层级", "1.1.1.1", "2.16", "5.9", "分类"],
        "版本管理": ["持续更新", "维护记录", "下线业务备查", "综合信息"],
    },
}

SOURCE_ENTITY_HINTS: Dict[str, List[str]] = {
    DEFAULT_SOURCES[0]: [
        "LITC_991176.VVCUIFP_LEAN",
        "LGC_EAM.STACMB_VVCUIFP",
        "CUCUNO",
        "CUIDNO",
        "CUSTCD",
        "ACT_CTG_CDIN('RD','RT')",
        "10201/10203/10202",
        "20060101",
        "EAC_RTL_CUST_EAC_INF_S",
        "CST_IDV_BAS_INF_S",
    ],
    DEFAULT_SOURCES[1]: [
        "NDS_VHIS.NLU39_DEE_EAC_TRX_T",
        "PDM_VHIS.T03_CORE_EAC_MDF_OPN_BNK_CTRL_INP",
        "MDF_CTRL_STS_CD",
        "NDS_VHIS.NLJ52_PTTSKISTP",
        "LOG_OWN_CLT",
        "LOG_LOG_TYP",
        "P1/P2/PP/PQ/PW",
        "Opr_Typ_Cd(A/C/D/E/U)",
        "T05_CIF_CUST_INF_MNT_EVT",
        "CUST_ID",
    ],
    DEFAULT_SOURCES[2]: [
        "NDS_VHIS.NLJ54_AGF_PROTOCOL_PAY_CARD_T",
        "PDM_VHIS.T05_AGN_DTL",
        "AGN_BCH_SEQ",
        "AUTO_PAY_ARG_ID",
        "TRX_DT",
        "LGC_EAM.EPHISTRXP1/EPHISTRXP2",
        "PDM_VHIS.T05_AGN_BCH_EVT",
        "代发状态代码(AGN_STS_CD)",
        "2004-2013",
        "代发增值税",
    ],
    DEFAULT_SOURCES[3]: [
        "1.1.2.5 开户行变更",
        "2.4.4 柜台代发",
        "1.2.21 手机号转账预约转账",
        "3.2 信用卡业务",
        "4.2 香港一卡通",
        "5.4 证券业务",
        "一级分类",
        "二级分类",
        "层级合理性",
        "版本管理",
    ],
}

CHAIN_LABELS = [
    "定义核验",
    "边界条件",
    "反例/冲突",
    "多路径或多口径分歧",
    "风险判定",
    "审计可追溯性",
    "性能或稳定性",
    "变更影响",
    "失败处置",
    "Go/No-Go 裁决",
]

DIMENSION_PROMPTS: Dict[str, List[str]] = {
    "语义歧义": [
        "术语定义是否唯一，是否存在同名异义？",
        "码值边界是否完整，新增码值进入流程是什么？",
        "是否存在可构造反例推翻当前定义？",
        "跨文档别名是否导致语义分歧？",
        "语义漂移会触发哪类风险判定？",
        "语义裁决是否可追溯到责任人与时间线？",
        "语义规则在周级回放中是否稳定？",
        "术语变更后影响哪些场景与规则？",
        "若定义冲突无法消解，失败处置是什么？",
        "最终放行条件是否满足统一术语与证据要求？",
    ],
    "多跳路径": [
        "主键链路是否可唯一确定？",
        "路径边界是否含跨域关联限制？",
        "是否存在更优反向路径导致冲突？",
        "多路径候选评分如何裁决？",
        "当前路径是否触发越权或错连风险？",
        "路径选择日志是否可审计回放？",
        "路径策略在历史分段上是否稳定？",
        "上游键规则变更影响哪些路径？",
        "路径证据不足时的失败处置是什么？",
        "Go/No-Go 阶段的路径放行阈值是什么？",
    ],
    "复杂 SQL（结构化查询语言）": [
        "SQL 拆解后的语义单元是否完整？",
        "时间/分区边界是否已显式化？",
        "是否存在反例 SQL 产生相反结果？",
        "多种 SQL 变体口径不一致时如何裁决？",
        "复杂结构是否触发性能或合规风险？",
        "SQL 版本是否具备完整审计链？",
        "执行计划与回放指标是否稳定？",
        "字段或表变更后的影响评估是否完成？",
        "语法/执行失败时回退策略是什么？",
        "最终 Go/No-Go 的 SQL 判定条件是什么？",
    ],
    "治理门禁": [
        "放行前置条件定义是否完备？",
        "边界条件下何时触发 need_approval（需人工复核）？",
        "可构造何种反例应直接 deny（阻断）？",
        "多门禁命中冲突时优先级如何处理？",
        "当前请求风险等级如何判定？",
        "门禁命中记录是否可审计追溯？",
        "门禁在周级评测中是否稳定达标？",
        "规则变更影响面是否完成评估？",
        "误拦截或漏拦截时失败处置是什么？",
        "Go/No-Go 放行结论是否满足所有硬门禁？",
    ],
    "审计追溯": [
        "追踪引用最小字段集是否完整？",
        "时间边界与版本边界是否可还原？",
        "是否存在证据冲突导致审计反例？",
        "多系统审计链路不一致时如何裁决？",
        "审计缺口是否触发风险升级？",
        "责任人与截止时间是否可回放？",
        "审计日志在增量发布后是否稳定？",
        "规则版本变更的影响记录是否齐全？",
        "审计字段缺失时失败处置是什么？",
        "Go/No-Go 是否满足“可追溯+可复核”要求？",
    ],
    "评测稳定性": [
        "评测样本定义是否覆盖当前场景？",
        "阈值边界是否已冻结？",
        "是否存在反例样本导致指标反转？",
        "多策略回放结果分歧如何裁决？",
        "当前波动是否触发风险告警？",
        "指标与规则命中是否可审计关联？",
        "周级趋势是否满足稳定性要求？",
        "场景扩容后评测影响如何评估？",
        "指标跌破阈值时失败处置是什么？",
        "Go/No-Go 是否达到发布稳定性门槛？",
    ],
}

ANSWER_DIRECTION_BY_DIMENSION = {
    "语义歧义": "先给术语或码值的统一定义，再给冲突判定规则，最后给责任人。",
    "多跳路径": "先给候选路径与评分，再解释选择理由，最后说明高风险处理。",
    "复杂 SQL（结构化查询语言）": "先给分解方案，再给校验与回退机制，最后给失败边界。",
    "治理门禁": "先给 allow/need_approval/deny 判定条件，再给规则命中证据。",
    "审计追溯": "先给 trace 与规则命中，再给责任人和时间线。",
    "评测稳定性": "先给指标阈值，再给异常触发动作与回退策略。",
}

FAIL_CONDITION_BY_DIMENSION = {
    "语义歧义": "无法给出统一定义或码值覆盖范围时，判定为补证失败。",
    "多跳路径": "未提供候选路径与放弃理由时，判定为不可裁决。",
    "复杂 SQL（结构化查询语言）": "无分解计划或无校验记录时，判定为驳回。",
    "治理门禁": "缺少规则命中证据或判定边界不清时，判定为驳回。",
    "审计追溯": "trace 不可回放或责任人缺失时，判定为驳回。",
    "评测稳定性": "无阈值、无波动解释、无回退动作时，判定为驳回。",
}

OWNER_BY_DIMENSION = {
    "语义歧义": "业务架构角色",
    "多跳路径": "数据架构角色",
    "复杂 SQL（结构化查询语言）": "数据架构角色",
    "治理门禁": "治理架构角色",
    "审计追溯": "治理架构角色",
    "评测稳定性": "稳定性质询角色",
}


@dataclass
class EvidenceItem:
    evidence_id: str
    source_doc: str
    anchor: str
    start_line: int
    end_line: int
    excerpt: str
    topic_tag: str


@dataclass
class Case:
    question_id: str
    dimension: str
    source_doc: str
    source_label: str
    scenario_anchor: str
    question: str
    answer_direction: str
    required_evidence: str
    fail_condition: str
    severity_default: str
    owner_role: str


@dataclass
class LegacyCase:
    case_id: str
    source_doc: str
    question: str
    risk_type: str
    expected_evidence: str


@dataclass
class LegacyFinding:
    finding_id: str
    severity: str
    source_doc: str
    missing_evidence: str
    impact: str
    owner: str


def normalize(text: str) -> str:
    text = text.lower().strip()
    text = re.sub(r"[`'\"，。！？、：:（）()\[\]{}<>\s]+", "", text)
    return text


def parse_matrix(matrix: str) -> Tuple[int, int, int]:
    m = re.fullmatch(r"(\d+)x(\d+)x(\d+)", matrix.strip())
    if not m:
        raise ValueError(f"invalid matrix format: {matrix}")
    return tuple(map(int, m.groups()))


def parse_bool(value: str) -> bool:
    v = (value or "").strip().lower()
    if v in {"1", "true", "yes", "y", "on"}:
        return True
    if v in {"0", "false", "no", "n", "off"}:
        return False
    raise ValueError(f"invalid bool value: {value}")


def severity_by_slot(slot: int, per_cell: int) -> str:
    if per_cell < 10:
        return "P1"
    if slot < 2:
        return "P0"
    if slot < 6:
        return "P1"
    return "P2"


def load_lines(root: pathlib.Path, source_doc: str) -> List[str]:
    path = root / source_doc
    return path.read_text(encoding="utf-8", errors="ignore").splitlines()


def find_anchor_window(
    lines: List[str],
    source_doc: str,
    anchor: str,
    keyword_hints: Optional[List[str]] = None,
) -> Tuple[int, int]:
    anchor_norm = normalize(anchor)

    hints = list(keyword_hints or [])
    if source_doc in SOURCE_ANCHOR_KEYWORDS:
        hints.extend(SOURCE_ANCHOR_KEYWORDS[source_doc].get(anchor, []))
    hints.append(anchor)

    hint_raws = [h.strip() for h in hints if h and h.strip()]
    hint_norms = [normalize(h) for h in hint_raws if len(normalize(h)) >= 3]

    best_idx = 0
    best_score = -1
    for idx, line in enumerate(lines, 1):
        line_raw = line.lower()
        line_norm = normalize(line)
        start = max(1, idx - 1)
        end = min(len(lines), idx + 1)
        window_text = " ".join(lines[start - 1 : end])
        window_raw = window_text.lower()
        window_norm = normalize(window_text)
        score = 0
        for h in hint_raws:
            h_low = h.lower()
            if len(h_low) < 4:
                continue
            if h_low in line_raw:
                score += 5
            elif h_low in window_raw:
                score += 3
        for h in hint_norms:
            if h in line_norm:
                score += 4
            elif h in window_norm:
                score += 2

        if line.lstrip().startswith("#"):
            score += 1

        if score > best_score:
            best_score = score
            best_idx = idx

    if best_score > 0 and best_idx > 0:
        return max(1, best_idx - 1), min(len(lines), best_idx + 2)

    for idx, line in enumerate(lines, 1):
        if anchor in line:
            return max(1, idx - 1), min(len(lines), idx + 2)
    for idx, line in enumerate(lines, 1):
        if anchor_norm and anchor_norm in normalize(line):
            return max(1, idx - 1), min(len(lines), idx + 2)
    for idx, line in enumerate(lines, 1):
        if line.strip():
            return idx, min(len(lines), idx + 2)
    return 1, 1


def build_excerpt(
    lines: List[str],
    start_line: int,
    end_line: int,
    max_chars: int = 110,
    focus_terms: Optional[List[str]] = None,
) -> str:
    seg = " ".join((lines[i - 1].strip() for i in range(start_line, end_line + 1) if 0 < i <= len(lines)))
    seg = re.sub(r"\s+", " ", seg)
    if len(seg) <= max_chars:
        return seg

    for term in (focus_terms or []):
        if not term:
            continue
        pos = seg.lower().find(term.lower())
        if pos >= 0:
            left = max(0, pos - max_chars // 3)
            right = min(len(seg), left + max_chars)
            cut = seg[left:right]
            if left > 0:
                cut = "..." + cut
            if right < len(seg):
                cut = cut + "..."
            return cut

    if len(seg) > max_chars:
        return seg[: max_chars - 3] + "..."
    return seg


def build_evidence_catalog(root: pathlib.Path, sources: List[str]) -> Tuple[List[EvidenceItem], Dict[Tuple[str, str], EvidenceItem]]:
    items: List[EvidenceItem] = []
    index: Dict[Tuple[str, str], EvidenceItem] = {}

    for src in sources:
        lines = load_lines(root, src)
        label_prefix = SOURCE_LABELS[src].split("-")[0]
        anchors = SOURCE_ANCHORS[src]
        for i, anchor in enumerate(anchors, 1):
            hints = SOURCE_ANCHOR_KEYWORDS.get(src, {}).get(anchor, [])
            start_line, end_line = find_anchor_window(lines, src, anchor, keyword_hints=hints)
            excerpt = build_excerpt(lines, start_line, end_line, focus_terms=[anchor] + hints)
            evid = EvidenceItem(
                evidence_id=f"EVID-{label_prefix}-{i:03d}",
                source_doc=src,
                anchor=anchor,
                start_line=start_line,
                end_line=end_line,
                excerpt=excerpt,
                topic_tag=anchor,
            )
            items.append(evid)
            index[(src, anchor)] = evid

    return items, index


def write_evidence_catalog(out_path: pathlib.Path, items: List[EvidenceItem]) -> None:
    lines: List[str] = []
    lines.append("# R2 证据目录（Evidence Catalog）")
    lines.append("")
    lines.append("> 目标：为 R2-240 题库提供可回放证据锚点（行号+原文摘录）。")
    lines.append("")
    lines.append("| evidence_id | source_doc | anchor | start_line | end_line | excerpt | topic_tag |")
    lines.append("|---|---|---|---:|---:|---|---|")
    for i in items:
        excerpt = i.excerpt.replace("|", "/")
        lines.append(
            f"| {i.evidence_id} | {i.source_doc} | {i.anchor} | {i.start_line} | {i.end_line} | {excerpt} | {i.topic_tag} |"
        )
    lines.append("")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def build_question(dimension: str, source_doc: str, slot: int, anchor: str, entity: str) -> str:
    chain = CHAIN_LABELS[slot % len(CHAIN_LABELS)]
    base = DIMENSION_PROMPTS[dimension][slot % len(DIMENSION_PROMPTS[dimension])]
    return (
        f"{chain}：请基于样本实体 `{entity}`，针对{SOURCE_LABELS[source_doc]}在“{anchor}”锚点回答：{base}"
    )


def format_anchor(anchor: str, evidence: EvidenceItem) -> str:
    return f"{anchor}（L{evidence.start_line}-L{evidence.end_line}）"


def format_evidence(source_doc: str, evidence: EvidenceItem) -> str:
    excerpt = evidence.excerpt.replace('"', '\\"')
    return f"{source_doc}:L{evidence.start_line}-L{evidence.end_line} | excerpt:\"{excerpt}\""


def choose_contrast(index: Dict[Tuple[str, str], EvidenceItem], source_doc: str, slot: int) -> EvidenceItem:
    anchors = SOURCE_ANCHORS[source_doc]
    next_anchor = anchors[(slot + 1) % len(anchors)]
    return index[(source_doc, next_anchor)]


def generate_matrix_cases(
    dimensions: List[str],
    sources: List[str],
    per_cell: int,
    evidence_mode: str,
    evidence_index: Dict[Tuple[str, str], EvidenceItem],
    qid_stable: bool,
) -> List[Case]:
    cases: List[Case] = []
    idx = 1

    _ = qid_stable  # kept for explicit contract; generation is deterministic and stable.

    for dim in dimensions:
        for src in sources:
            entities = SOURCE_ENTITY_HINTS[src]
            anchors = SOURCE_ANCHORS[src]
            for slot in range(per_cell):
                anchor = anchors[slot % len(anchors)]
                entity = entities[slot % len(entities)]
                severity = severity_by_slot(slot, per_cell)
                qid = f"Q{idx:04d}"

                if evidence_mode == "line_excerpt":
                    main_e = evidence_index[(src, anchor)]
                    scenario_anchor = format_anchor(anchor, main_e)
                    required = format_evidence(src, main_e)
                    if severity in {"P0", "P1"}:
                        contrast = choose_contrast(evidence_index, src, slot)
                        required += f" || contrast:{format_evidence(src, contrast)}"
                else:
                    scenario_anchor = anchor
                    required = f"{src}#{anchor}"

                question = build_question(dim, src, slot, anchor, entity)
                cases.append(
                    Case(
                        question_id=qid,
                        dimension=dim,
                        source_doc=src,
                        source_label=SOURCE_LABELS[src],
                        scenario_anchor=scenario_anchor,
                        question=question,
                        answer_direction=ANSWER_DIRECTION_BY_DIMENSION[dim],
                        required_evidence=required,
                        fail_condition=FAIL_CONDITION_BY_DIMENSION[dim],
                        severity_default=severity,
                        owner_role=OWNER_BY_DIMENSION[dim],
                    )
                )
                idx += 1

    return cases


def classify_source(seed: str) -> str:
    s = seed
    if any(k in s for k in ["代发", "批次", "协议号", "batch", "amt", "代扣"]):
        return DEFAULT_SOURCES[2]
    if any(k in s for k in ["维护日志", "modify", "变更", "operator", "log", "1G", "2G", "开户行变更"]):
        return DEFAULT_SOURCES[1]
    if any(k in s for k in ["分类", "主题节点", "业务场景", "层级", "节点"]):
        return DEFAULT_SOURCES[3]
    return DEFAULT_SOURCES[0]


def classify_dimension(seed: str) -> str:
    s = seed.lower()
    if any(k in s for k in ["码值", "术语", "含义", "取值", "unknown", "未知", "语义"]):
        return "语义歧义"
    if any(k in s for k in ["关联", "join", "多表", "多跳", "映射", "主键", "派生"]):
        return "多跳路径"
    if any(k in s for k in ["窗口", "group by", "order by", "union", "子查询", "sql", "分区", "性能"]):
        return "复杂 SQL（结构化查询语言）"
    if any(k in s for k in ["门禁", "放行", "阻断", "need_approval", "deny", "脱敏", "越权"]):
        return "治理门禁"
    if any(k in s for k in ["审计", "trace", "版本", "追溯", "verified_at", "change_summary"]):
        return "审计追溯"
    if any(k in s for k in ["评测", "回放", "阈值", "误拦截", "稳定", "波动", "告警"]):
        return "评测稳定性"
    return "治理门禁"


def parse_seed_questions(seed_path: pathlib.Path) -> List[Tuple[int, str]]:
    text = seed_path.read_text(encoding="utf-8", errors="ignore")
    seeds: List[Tuple[int, str]] = []
    for line in text.splitlines():
        m = re.match(r"\s*(\d+)\.\s*(.+)\s*$", line)
        if m:
            seeds.append((int(m.group(1)), m.group(2).strip()))
    return sorted(seeds, key=lambda x: x[0])


def build_mapping(
    seeds: List[Tuple[int, str]],
    cases: List[Case],
    mapping_primary_unique: bool,
) -> List[Dict[str, str]]:
    by_bucket: Dict[Tuple[str, str], List[str]] = {}
    for c in cases:
        by_bucket.setdefault((c.dimension, c.source_doc), []).append(c.question_id)

    bucket_idx: Dict[Tuple[str, str], int] = {k: 0 for k in by_bucket}
    all_ids = [c.question_id for c in cases]
    global_idx = 0
    used_primary: set[str] = set()

    def next_qid(bucket: Tuple[str, str], need_unique: bool, forbidden: Optional[Set[str]] = None) -> str:
        nonlocal global_idx
        forbidden = forbidden or set()
        ids = by_bucket.get(bucket, [])
        while True:
            i = bucket_idx.get(bucket, 0)
            if i < len(ids):
                cand = ids[i]
                bucket_idx[bucket] = i + 1
            else:
                cand = all_ids[global_idx % len(all_ids)]
                global_idx += 1
            if cand in forbidden:
                continue
            if not need_unique or cand not in used_primary:
                return cand

    rows: List[Dict[str, str]] = []
    seen_norm: Dict[str, Dict[str, str]] = {}
    merge_counter = 1

    for sid, sq in seeds:
        src = classify_source(sq)
        dim = classify_dimension(sq)
        bucket = (dim, src)
        norm = normalize(sq)

        if norm in seen_norm:
            group_id = seen_norm[norm].get("merge_group_id", "")
            if not group_id:
                group_id = f"MG-{merge_counter:03d}"
                merge_counter += 1
                seen_norm[norm]["merge_group_id"] = group_id
                for row in rows:
                    if row["primary_qid"] == seen_norm[norm]["primary_qid"] and row["merge_group_id"] == "":
                        row["merge_group_id"] = group_id

            rows.append(
                {
                    "seed_id": str(sid),
                    "seed_question": sq,
                    "status": "合并",
                    "primary_qid": seen_norm[norm]["primary_qid"],
                    "secondary_qids": "[]",
                    "merge_group_id": group_id,
                    "reason": "与既有种子问题语义等价，合并到同一主映射题号。",
                }
            )
            continue

        primary = next_qid(bucket, need_unique=mapping_primary_unique)
        used_primary.add(primary)

        if len(sq) >= 60 and ("、" in sq or "和" in sq or "以及" in sq):
            forbidden = {primary}
            s1 = next_qid(bucket, need_unique=False, forbidden=forbidden)
            forbidden.add(s1)
            s2 = next_qid(bucket, need_unique=False, forbidden=forbidden)
            secondary = f"[{s1},{s2}]"
            status = "拆分"
            reason = "问题复合度较高，按维度差异拆分为两个子问题。"
        else:
            secondary = "[]"
            status = "重写"
            reason = "重写为可证据化、可裁决问法。"

        row = {
            "seed_id": str(sid),
            "seed_question": sq,
            "status": status,
            "primary_qid": primary,
            "secondary_qids": secondary,
            "merge_group_id": "",
            "reason": reason,
        }
        rows.append(row)
        seen_norm[norm] = {
            "primary_qid": primary,
            "merge_group_id": "",
        }

    if mapping_primary_unique:
        primaries = [r["primary_qid"] for r in rows]
        counter = Counter(primaries)
        duplicate_count = sum(v - 1 for v in counter.values() if v > 1)
        duplicate_rate = duplicate_count / max(1, len(primaries))
        if duplicate_rate > 0.10:
            raise ValueError(f"primary_qid duplicate rate {duplicate_rate:.2%} exceeds 10%")
        dup_ids = {k for k, v in counter.items() if v > 1}
        for r in rows:
            if r["primary_qid"] in dup_ids and not r["merge_group_id"]:
                raise ValueError("duplicate primary_qid found without merge_group_id")

    return rows


def render_main_doc(cases: List[Case]) -> str:
    def md_cell(text: str) -> str:
        return str(text).replace("|", "\\|").replace("\n", "<br>")

    lines: List[str] = []
    lines.append("# 数据直通车 · 口径文档质询题库 R2（240题）")
    lines.append("")
    lines.append("> **目标**：形成可直接评审使用的 240 题对抗质询题库，覆盖六维度与四样本，支持设计组与质询组逐轮对抗。")
    lines.append("")
    lines.append("> **范围**：仅使用背景文档、03目录样本与业界参考、接口文档与详细设计文档；不依赖系统总体设计文档。")
    lines.append("")
    lines.append("> **读者**：设计组、质询组、裁决组、项目管理负责人。")
    lines.append("")
    lines.append("> **当前阶段定位**：治理先行阶段的主评审题库输入。")
    lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("## 1. 题库结构")
    lines.append("")
    lines.append("1. 维度：语义歧义、多跳路径、复杂 SQL（结构化查询语言）、治理门禁、审计追溯、评测稳定性。")
    lines.append("2. 样本：09/10/11/12 四份口径文档。")
    lines.append("3. 网格：`6×4×10 = 240`。")
    lines.append("4. 证据：`required_evidence` 统一为 `file:Lx-Ly | excerpt:\"...\"`。")
    lines.append("")
    lines.append("## 2. 覆盖矩阵")
    lines.append("")
    lines.append("| 维度 | 09 | 10 | 11 | 12 | 小计 |")
    lines.append("|---|---:|---:|---:|---:|---:|")
    for d in DIMENSIONS:
        lines.append(f"| {d} | 10 | 10 | 10 | 10 | 40 |")
    lines.append("| 合计 | 60 | 60 | 60 | 60 | 240 |")
    lines.append("")
    lines.append("## 3. 风险配比")
    lines.append("")
    p0 = sum(1 for c in cases if c.severity_default == "P0")
    p1 = sum(1 for c in cases if c.severity_default == "P1")
    p2 = sum(1 for c in cases if c.severity_default == "P2")
    total = len(cases)
    lines.append("| 风险级别 | 数量 | 占比 |")
    lines.append("|---|---:|---:|")
    lines.append(f"| P0 | {p0} | {p0/total:.0%} |")
    lines.append(f"| P1 | {p1} | {p1/total:.0%} |")
    lines.append(f"| P2 | {p2} | {p2/total:.0%} |")
    lines.append("")
    lines.append("## 4. 主表（240题）")
    lines.append("")
    lines.append("| question_id（问题编号） | dimension（维度） | source_doc（来源文档） | scenario_anchor（场景锚点） | question（质询问题） | answer_direction（回答方向） | required_evidence（必须证据） | fail_condition（不通过条件） | severity_default（默认风险级别） | owner_role（责任角色） |")
    lines.append("|---|---|---|---|---|---|---|---|---|---|")
    for c in cases:
        lines.append(
            f"| {md_cell(c.question_id)} | {md_cell(c.dimension)} | {md_cell(c.source_doc)} | {md_cell(c.scenario_anchor)} | {md_cell(c.question)} | {md_cell(c.answer_direction)} | {md_cell(c.required_evidence)} | {md_cell(c.fail_condition)} | {md_cell(c.severity_default)} | {md_cell(c.owner_role)} |"
        )
    lines.append("")
    lines.append("## 5. 验收检查")
    lines.append("")
    lines.append("1. 问题总数为 240。")
    lines.append("2. 每个维度×样本单元格为 10 题。")
    lines.append("3. 每题均含回答方向、必须证据、不通过条件。")
    lines.append("4. P0/P1/P2 配比满足 48/96/96。")
    return "\n".join(lines) + "\n"


def render_mapping_doc(rows: List[Dict[str, str]]) -> str:
    def md_cell(text: str) -> str:
        return str(text).replace("|", "\\|").replace("\n", "<br>")

    primary_counter = Counter(r["primary_qid"] for r in rows)
    duplicate_count = sum(v - 1 for v in primary_counter.values() if v > 1)
    duplicate_rate = duplicate_count / max(1, len(rows))

    lines: List[str] = []
    lines.append("# 数据直通车 · 质询题库 100题映射追踪表")
    lines.append("")
    lines.append("> **目标**：跟踪用户种子100题在R2题库中的重构结果，保证覆盖率 100%。")
    lines.append("")
    lines.append("> **范围**：`seed_id 1-100` 的保留/合并/拆分/重写映射。")
    lines.append("")
    lines.append("> **读者**：设计组、质询组、裁决组、项目管理负责人。")
    lines.append("")
    lines.append("> **当前阶段定位**：R2 主评审题库上线前的映射追踪基线。")
    lines.append("")
    lines.append("| seed_id（种子编号） | seed_question（种子问题） | status（处理状态） | primary_qid（主映射题号） | secondary_qids[]（补充题号） | merge_group_id（合并组） | reason（处理原因） |")
    lines.append("|---:|---|---|---|---|---|---|")
    for r in rows:
        lines.append(
            f"| {md_cell(r['seed_id'])} | {md_cell(r['seed_question'])} | {md_cell(r['status'])} | {md_cell(r['primary_qid'])} | {md_cell(r['secondary_qids'])} | {md_cell(r['merge_group_id'])} | {md_cell(r['reason'])} |"
        )
    lines.append("")
    lines.append(f"总计：{len(rows)} 条，覆盖率 100%，primary_qid 重复率 {duplicate_rate:.2%}")
    return "\n".join(lines) + "\n"


# --- legacy compatibility mode ---
def make_cases(source_doc: str, text: str, min_count: int) -> List[LegacyCase]:
    questions: List[LegacyCase] = []

    def add(q: str, risk: str, expected: str) -> None:
        questions.append(LegacyCase("", source_doc, q, risk, expected))

    if "未提供（待补充）" in text:
        add("该文档存在“未提供（待补充）”，哪些字段必须在发布前补齐？", "治理门禁质询", "最小单元必填校验记录")

    if re.search(r"IN\s*\(\s*''\s*\)", text) or re.search(r"=\s*''", text):
        add("文档出现空占位条件，系统如何自动阻断并要求补参？", "治理门禁质询", "空条件阻断规则与命中日志")

    if re.search(r"JOIN|LEFT JOIN|INNER JOIN", text, re.IGNORECASE):
        add("多表关联路径如何打分并输出放弃路径理由？", "多跳路径质询", "路径评分与拒绝理由明细")

    if re.search(r"GROUP BY|ORDER BY|UNION|SUBSTRING|CASE", text, re.IGNORECASE):
        add("复杂查询分解为哪些子模块，是否有对应的自检步骤？", "复杂 SQL 质询", "分解计划与自检报告")

    if "数据不连续" in text or "缺数" in text:
        add("文档提示数据不连续/缺数，如何输出可信度标签并进入回放评测？", "评测稳定性质询", "可信度标注与回放样本")

    if re.search(r"码值|代码|状态", text):
        add("码值与状态代码是否有统一字典，跨表是否同义？", "语义歧义质询", "码值字典与冲突检测报告")

    generic_pool = [
        ("同一业务是否存在多个主键链路，冲突如何裁决？", "多跳路径质询", "主键链路对照与裁决记录"),
        ("该文档能否直接转为可机读结构，缺口在哪里？", "语义歧义质询", "结构化抽取缺口清单"),
        ("哪些请求应判定为 need_approval（需人工复核）而非 allow（放行）？", "治理门禁质询", "判定矩阵与样本回放"),
        ("哪些规则命中需要强制 deny（阻断）？", "治理门禁质询", "高风险规则清单与命中示例"),
        ("该文档改动后如何评估受影响场景范围？", "审计追溯质询", "影响分析结果"),
        ("回放样本如何覆盖该文档中的高风险路径？", "评测稳定性质询", "样本覆盖率报告"),
    ]

    i = 0
    while len(questions) < min_count:
        q, r, e = generic_pool[i % len(generic_pool)]
        questions.append(LegacyCase("", source_doc, q, r, e))
        i += 1

    return questions[:min_count]


def make_findings(all_cases: List[LegacyCase]) -> List[LegacyFinding]:
    findings: List[LegacyFinding] = []
    grouped: Dict[str, int] = {}
    for c in all_cases:
        grouped.setdefault(c.source_doc, 0)
        grouped[c.source_doc] += 1

    idx = 1
    for src in grouped:
        findings.append(
            LegacyFinding(
                finding_id=f"CF-{idx:03d}",
                severity="P0" if idx <= 4 else "P1",
                source_doc=src,
                missing_evidence="缺少可直接复核的规则命中与证据锚点",
                impact="无法形成稳定裁决，评审结论可执行性不足",
                owner="治理架构角色" if idx <= 4 else "口径质询角色",
            )
        )
        idx += 1

    while len(findings) < 10:
        findings.append(
            LegacyFinding(
                finding_id=f"CF-{idx:03d}",
                severity="P1",
                source_doc="跨文档",
                missing_evidence="缺少跨文档一致性证明",
                impact="可能产生语义冲突与回放波动",
                owner="稳定性质询角色",
            )
        )
        idx += 1

    return findings


def render_legacy(cases: List[LegacyCase], findings: List[LegacyFinding]) -> str:
    lines: List[str] = []
    lines.append("# 自动生成质询结果")
    lines.append("")
    lines.append("## ChallengeCase")
    lines.append("")
    lines.append("| case_id | source_doc | question | risk_type | expected_evidence |")
    lines.append("|---|---|---|---|---|")
    for i, c in enumerate(cases, 1):
        cid = f"CC-{i:03d}"
        lines.append(f"| {cid} | {c.source_doc} | {c.question} | {c.risk_type} | {c.expected_evidence} |")

    lines.append("")
    lines.append("## ChallengeFinding")
    lines.append("")
    lines.append("| finding_id | severity | source_doc | missing_evidence | impact | owner |")
    lines.append("|---|---|---|---|---|---|")
    for f in findings:
        lines.append(f"| {f.finding_id} | {f.severity} | {f.source_doc} | {f.missing_evidence} | {f.impact} | {f.owner} |")

    lines.append("")
    lines.append("## DecisionRecord Template")
    lines.append("")
    lines.append("| decision_id | case_or_finding | decision | rules_hit | trace_ref | owner | due_date |")
    lines.append("|---|---|---|---|---|---|---|")
    lines.append("| DR-001 | CC-001 | 补证 | RULE-XXX | TRACE-XXX | 治理架构角色 | YYYY-MM-DD |")
    lines.append("")
    lines.append(f"总计：ChallengeCase={len(cases)}，ChallengeFinding={len(findings)}")
    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()

    # legacy
    parser.add_argument("--source", action="append", dest="sources", help="relative source path (repeatable)")
    parser.add_argument("--min-per-source", type=int, default=8)
    parser.add_argument("--out", help="legacy output path")

    # R2 mode
    parser.add_argument("--seed-file", help="seed question file with 'N. question' format")
    parser.add_argument("--target-count", type=int, help="target question count, e.g. 240")
    parser.add_argument("--matrix", help="matrix like 6x4x10")
    parser.add_argument("--with-answer-direction", action="store_true")
    parser.add_argument("--with-fail-condition", action="store_true")
    parser.add_argument("--out-main", help="R2 main question bank markdown path")
    parser.add_argument("--out-mapping", help="R2 seed mapping markdown path")
    parser.add_argument("--evidence-mode", choices=["anchor", "line_excerpt"], default="line_excerpt")
    parser.add_argument("--evidence-catalog-out", help="evidence catalog markdown path")
    parser.add_argument("--qid-stable", default="true", help="keep QID stable (true/false)")
    parser.add_argument("--mapping-primary-unique", default="true", help="prefer unique primary_qid (true/false)")

    args = parser.parse_args()
    root = pathlib.Path.cwd()

    r2_mode = bool(args.target_count and args.matrix and args.out_main and args.out_mapping)

    if r2_mode:
        d, s, q = parse_matrix(args.matrix)
        if d != len(DIMENSIONS):
            raise ValueError(f"matrix dimension count must be {len(DIMENSIONS)}, got {d}")

        sources = args.sources if args.sources else DEFAULT_SOURCES
        if len(sources) != s:
            raise ValueError(f"matrix source count must be {s}, got {len(sources)}")

        expected = d * s * q
        if args.target_count != expected:
            raise ValueError(f"target-count mismatch: expected {expected}, got {args.target_count}")

        for src in sources:
            if src not in SOURCE_LABELS:
                raise ValueError(f"unsupported source in matrix mode: {src}")
            if not (root / src).exists():
                raise FileNotFoundError(f"source file not found: {src}")

        qid_stable = parse_bool(args.qid_stable)
        mapping_primary_unique = parse_bool(args.mapping_primary_unique)

        evidence_items: List[EvidenceItem] = []
        evidence_index: Dict[Tuple[str, str], EvidenceItem] = {}
        if args.evidence_mode == "line_excerpt":
            evidence_items, evidence_index = build_evidence_catalog(root, sources)
            catalog_out = args.evidence_catalog_out or "skills/caliber-redteam-review/references/evidence-catalog-r2.md"
            out_catalog = pathlib.Path(catalog_out)
            out_catalog = out_catalog if out_catalog.is_absolute() else (root / out_catalog)
            write_evidence_catalog(out_catalog, evidence_items)
            print(f"written evidence catalog: {out_catalog}")

        cases = generate_matrix_cases(
            dimensions=DIMENSIONS,
            sources=sources,
            per_cell=q,
            evidence_mode=args.evidence_mode,
            evidence_index=evidence_index,
            qid_stable=qid_stable,
        )

        if not args.with_answer_direction:
            for c in cases:
                c.answer_direction = ""
        if not args.with_fail_condition:
            for c in cases:
                c.fail_condition = ""

        mapping_rows: List[Dict[str, str]] = []
        if args.seed_file:
            seed_path = pathlib.Path(args.seed_file)
            seed_path = seed_path if seed_path.is_absolute() else (root / seed_path)
            if not seed_path.exists():
                raise FileNotFoundError(f"seed file not found: {seed_path}")
            seeds = parse_seed_questions(seed_path)
            if len(seeds) < 100:
                raise ValueError(f"seed file should contain at least 100 questions, got {len(seeds)}")
            mapping_rows = build_mapping(seeds[:100], cases, mapping_primary_unique=mapping_primary_unique)

        main_text = render_main_doc(cases)
        mapping_text = render_mapping_doc(mapping_rows)

        out_main = pathlib.Path(args.out_main)
        out_main = out_main if out_main.is_absolute() else (root / out_main)
        out_main.parent.mkdir(parents=True, exist_ok=True)
        out_main.write_text(main_text, encoding="utf-8")

        out_mapping = pathlib.Path(args.out_mapping)
        out_mapping = out_mapping if out_mapping.is_absolute() else (root / out_mapping)
        out_mapping.parent.mkdir(parents=True, exist_ok=True)
        out_mapping.write_text(mapping_text, encoding="utf-8")

        print(f"written main: {out_main}")
        print(f"written mapping: {out_mapping}")
        return

    if not args.out:
        raise ValueError("legacy mode requires --out, or use full R2 arguments")

    sources = args.sources if args.sources else DEFAULT_SOURCES
    all_cases: List[LegacyCase] = []
    for src in sources:
        path = root / src
        if not path.exists():
            raise FileNotFoundError(f"source file not found: {src}")
        text = path.read_text(encoding="utf-8", errors="ignore")
        all_cases.extend(make_cases(src, text, args.min_per_source))

    findings = make_findings(all_cases)
    output = render_legacy(all_cases, findings)

    out_path = pathlib.Path(args.out)
    out_path = out_path if out_path.is_absolute() else (root / out_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(output, encoding="utf-8")
    print(f"written: {out_path}")


if __name__ == "__main__":
    main()
