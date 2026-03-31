#!/usr/bin/env python3
"""Download full-paper PDFs for NL2SQL / Knowledge Graph / Data Governance research.

This script builds a local full-paper archive under:
research/papers/collection

It prioritizes platforms with accessible open PDFs in this environment:
- arXiv
- Research Square
- EarthArXiv
- IEEE (open-access papers with externally reachable PDF URLs)
"""

from __future__ import annotations

import csv
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional


START_DATE = "2023-01-01"
END_DATE = "2026-03-03"
USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
)

# Target counts by platform.
PLATFORM_QUOTAS = {
    "arXiv": 30,
    "Research Square": 10,
    "EarthArXiv": 5,
    "IEEE(Open)": 5,
}


@dataclass
class Candidate:
    platform: str
    topic: str
    title: str
    year: str
    id_value: str
    landing_url: str
    pdf_url: str
    source: str


def request_bytes(url: str, timeout: int = 45) -> bytes:
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "*/*",
        },
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read()


def request_text(url: str, timeout: int = 45) -> str:
    return request_bytes(url, timeout=timeout).decode("utf-8", "ignore")


def request_json(url: str, timeout: int = 45) -> dict:
    return json.loads(request_text(url, timeout=timeout))


def sanitize_filename(text: str, max_len: int = 80) -> str:
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    text = re.sub(r"\s+", "-", text.strip())
    if not text:
        text = "paper"
    return text[:max_len].strip("-")


def normalize_id(raw: str) -> str:
    # Normalize DOI/arXiv IDs for filenames and de-dup.
    rid = raw.strip()
    rid = rid.replace("https://doi.org/", "").replace("http://dx.doi.org/", "")
    rid = rid.replace("http://arxiv.org/abs/", "")
    rid = rid.replace("/", "_")
    rid = re.sub(r"[^A-Za-z0-9._-]", "_", rid)
    return rid


def arxiv_candidates() -> List[Candidate]:
    queries = [
        ("NL2SQL", 'all:"text to sql"'),
        ("NL2SQL", "all:nl2sql"),
        ("知识图谱", 'all:"knowledge graph"'),
        ("数据治理", 'all:"data governance"'),
    ]
    out: List[Candidate] = []
    ns = {"atom": "http://www.w3.org/2005/Atom"}

    for topic, q in queries:
        params = {
            "search_query": f"{q} AND submittedDate:[202301010000 TO 202603032359]",
            "start": 0,
            "max_results": 120,
            "sortBy": "submittedDate",
            "sortOrder": "descending",
        }
        url = "http://export.arxiv.org/api/query?" + urllib.parse.urlencode(params)
        try:
            xml_content = request_text(url)
            root = ET.fromstring(xml_content)
        except Exception:
            continue

        for entry in root.findall("atom:entry", ns):
            title = (entry.findtext("atom:title", default="", namespaces=ns) or "").strip()
            id_url = (entry.findtext("atom:id", default="", namespaces=ns) or "").strip()
            published = (entry.findtext("atom:published", default="", namespaces=ns) or "").strip()
            if not title or not id_url:
                continue
            # Keep versioned arXiv ID to match actual PDF URL.
            arxiv_id = id_url.split("/abs/")[-1]
            pdf_url = f"https://arxiv.org/pdf/{arxiv_id}"
            out.append(
                Candidate(
                    platform="arXiv",
                    topic=topic,
                    title=title,
                    year=published[:4] if published else "",
                    id_value=arxiv_id,
                    landing_url=id_url.replace("http://", "https://"),
                    pdf_url=pdf_url,
                    source="arXiv API",
                )
            )
    return out


def openalex_candidates(prefix: str, platform: str, terms: Iterable[str], per_term: int) -> List[Candidate]:
    out: List[Candidate] = []
    topic_map = {
        "text to sql": "NL2SQL",
        "nl2sql": "NL2SQL",
        "knowledge graph": "知识图谱",
        "data governance": "数据治理",
    }

    for term in terms:
        params = {
            "search": term,
            "filter": (
                f"doi_starts_with:{prefix},"
                f"from_publication_date:{START_DATE},"
                f"to_publication_date:{END_DATE},"
                "is_oa:true"
            ),
            "per-page": per_term,
            "sort": "relevance_score:desc",
        }
        url = "https://api.openalex.org/works?" + urllib.parse.urlencode(params)
        try:
            data = request_json(url)
        except Exception:
            continue
        for item in data.get("results", []):
            doi = (item.get("doi") or "").strip()
            title = (item.get("display_name") or "").strip()
            year = str(item.get("publication_year") or "")
            landing_url = ""
            pdf_url = ""

            best = item.get("best_oa_location") or {}
            open_access = item.get("open_access") or {}
            landing_url = (best.get("landing_page_url") or "").strip()
            pdf_url = (best.get("pdf_url") or open_access.get("oa_url") or "").strip()

            if not doi or not title or not pdf_url:
                continue

            # Skip DOI-style URLs that usually lead back to blocked landing pages.
            if "doi.org/10.1109/" in pdf_url and platform == "IEEE(Open)":
                continue

            out.append(
                Candidate(
                    platform=platform,
                    topic=topic_map.get(term, term),
                    title=title,
                    year=year,
                    id_value=doi.replace("https://doi.org/", ""),
                    landing_url=landing_url or doi,
                    pdf_url=pdf_url,
                    source="OpenAlex",
                )
            )
    return out


def download_pdf(url: str, dest: Path) -> str:
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/pdf,*/*;q=0.8",
            "Referer": "https://scholar.google.com/",
        },
    )
    with urllib.request.urlopen(req, timeout=90) as resp:
        data = resp.read()
        ctype = (resp.headers.get("content-type") or "").lower()
        final_url = resp.geturl()

    if not data.startswith(b"%PDF"):
        raise ValueError(f"not_pdf content-type={ctype} final_url={final_url}")

    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    return final_url


def select_by_quota(candidates: List[Candidate]) -> List[Candidate]:
    selected: List[Candidate] = []
    seen_ids = set()
    by_platform: Dict[str, List[Candidate]] = {}
    for c in candidates:
        by_platform.setdefault(c.platform, []).append(c)

    # Prefer newer papers within each platform.
    for plist in by_platform.values():
        plist.sort(key=lambda x: x.year, reverse=True)

    for platform, quota in PLATFORM_QUOTAS.items():
        pool = by_platform.get(platform, [])
        taken = 0
        for c in pool:
            key = f"{c.platform}:{normalize_id(c.id_value)}"
            if key in seen_ids:
                continue
            selected.append(c)
            seen_ids.add(key)
            taken += 1
            if taken >= quota:
                break
    return selected


def write_markdown_summary(output_dir: Path, rows: List[dict]) -> None:
    md = []
    md.append("# 跨平台论文全文下载结果")
    md.append("")
    md.append(f"- 时间范围：`{START_DATE}` 到 `{END_DATE}`")
    md.append("- 主题：`NL2SQL` / `知识图谱` / `数据治理`")
    md.append("- 说明：仅保存完整 PDF（`%PDF` 校验通过）")
    md.append("")

    by_platform: Dict[str, Dict[str, int]] = {}
    for r in rows:
        p = r["platform"]
        by_platform.setdefault(p, {"ok": 0, "fail": 0})
        if r["status"] == "downloaded":
            by_platform[p]["ok"] += 1
        else:
            by_platform[p]["fail"] += 1

    md.append("## 平台统计")
    md.append("")
    md.append("| 平台 | 下载成功 | 下载失败 |")
    md.append("|---|---:|---:|")
    for p in sorted(by_platform.keys()):
        md.append(f"| {p} | {by_platform[p]['ok']} | {by_platform[p]['fail']} |")

    md.append("")
    md.append("## 受限平台说明")
    md.append("")
    md.append("- `bioRxiv/medRxiv`：当前网络环境返回 403，无法直接下载全文 PDF。")
    md.append("- `ChemRxiv`：当前网络环境返回 403，无法直接下载全文 PDF。")
    md.append("- `Preprints.org`：当前网络环境返回 403，无法直接下载全文 PDF。")
    md.append("- `SSRN`：当前网络环境返回 403，无法直接下载全文 PDF。")
    md.append("- `IEEE`：非开放链接在当前网络环境可能返回 418/拒绝，仅保留可达开放全文。")
    md.append("")

    (output_dir / "02-下载结果.md").write_text("\n".join(md), encoding="utf-8")


def write_readme(output_dir: Path) -> None:
    content = """# 数据直通车-跨平台论文全集

## 目标
保存跨平台检索得到的**完整论文全文（PDF）**，用于 NL2SQL、知识图谱、数据治理研究支撑。

## 范围
- 时间范围：2023-01-01 到 2026-03-03
- 主题：NL2SQL / 知识图谱 / 数据治理
- 当前归档平台：arXiv、Research Square、EarthArXiv、IEEE(Open)

## 读者
- 架构评审人
- 算法与平台研发
- 方案例会讲解人员

## 当前阶段定位
本目录为“研究证据归档层”，与方案文档解耦，专注保存可复核全文与元数据索引。
"""
    (output_dir / "00-归档说明.md").write_text(content, encoding="utf-8")


def main() -> None:
    repo_root = Path(__file__).resolve().parents[1]
    output_dir = repo_root / "research" / "papers" / "collection"
    papers_root = output_dir / "papers"
    output_dir.mkdir(parents=True, exist_ok=True)
    papers_root.mkdir(parents=True, exist_ok=True)

    write_readme(output_dir)

    print("Collecting candidates...")
    candidates: List[Candidate] = []
    candidates.extend(arxiv_candidates())
    candidates.extend(
        openalex_candidates(
            prefix="10.21203",
            platform="Research Square",
            terms=["text to sql", "knowledge graph", "data governance"],
            per_term=80,
        )
    )
    candidates.extend(
        openalex_candidates(
            prefix="10.31223",
            platform="EarthArXiv",
            terms=["text to sql", "knowledge graph", "data governance"],
            per_term=80,
        )
    )
    candidates.extend(
        openalex_candidates(
            prefix="10.1109",
            platform="IEEE(Open)",
            terms=["text to sql", "knowledge graph", "data governance"],
            per_term=120,
        )
    )

    selected = select_by_quota(candidates)
    print(f"Selected candidates: {len(selected)}")

    rows: List[dict] = []
    file_seq = 1
    for c in selected:
        pid = normalize_id(c.id_value)
        title_slug = sanitize_filename(c.title, max_len=70)
        file_name = f"{file_seq:03d}-{pid}-{title_slug}.pdf"
        platform_dir = papers_root / c.platform.lower().replace("(", "_").replace(")", "_").replace(" ", "-")
        dest = platform_dir / file_name

        status = "downloaded"
        error = ""
        final_url = ""
        try:
            final_url = download_pdf(c.pdf_url, dest)
            file_seq += 1
            print(f"OK   [{c.platform}] {c.title[:100]}")
        except Exception as ex:
            status = "failed"
            error = str(ex)
            print(f"FAIL [{c.platform}] {c.title[:100]} :: {error}")

        rows.append(
            {
                "platform": c.platform,
                "topic": c.topic,
                "year": c.year,
                "id": c.id_value,
                "title": c.title,
                "landing_url": c.landing_url,
                "pdf_url": c.pdf_url,
                "final_url": final_url,
                "status": status,
                "file_path": str(dest.relative_to(output_dir)) if status == "downloaded" else "",
                "error": error,
                "source": c.source,
            }
        )
        time.sleep(0.15)

    csv_path = output_dir / "01-论文清单.csv"
    with csv_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=[
                "platform",
                "topic",
                "year",
                "id",
                "title",
                "landing_url",
                "pdf_url",
                "final_url",
                "status",
                "file_path",
                "error",
                "source",
            ],
        )
        writer.writeheader()
        writer.writerows(rows)

    write_markdown_summary(output_dir, rows)
    print("Done.")
    print(f"Output: {output_dir}")


if __name__ == "__main__":
    main()
