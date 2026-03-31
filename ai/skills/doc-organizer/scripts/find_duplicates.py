#!/usr/bin/env python3
"""
重复文档检测脚本

检测方式：
1. 完全相同文件（MD5 哈希）
2. 相似内容文件（基于文本相似度）
3. 相似文件名（可能是版本重复）
"""

import os
import sys
import hashlib
import re
from pathlib import Path
from collections import defaultdict
from difflib import SequenceMatcher
from typing import Dict, List, Set, Tuple


def calculate_md5(file_path: str) -> str:
    """计算文件 MD5 哈希值"""
    hash_md5 = hashlib.md5()
    try:
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_md5.update(chunk)
        return hash_md5.hexdigest()
    except (IOError, OSError):
        return ""


def extract_text_content(file_path: str) -> str:
    """提取文本内容（用于相似度比较）"""
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            # 去除空白字符进行标准化
            content = re.sub(r'\s+', ' ', content).strip()
            return content[:10000]  # 限制长度避免内存问题
    except (IOError, OSError):
        return ""


def text_similarity(text1: str, text2: str) -> float:
    """计算两段文本的相似度（0-1）"""
    if not text1 or not text2:
        return 0.0
    return SequenceMatcher(None, text1, text2).ratio()


def find_exact_duplicates(target_dir: str, extensions: Set[str] = None) -> Dict[str, List[str]]:
    """查找完全相同的文件（基于 MD5）"""
    if extensions is None:
        extensions = {'.md', '.txt', '.json', '.yaml', '.yml'}
    
    hash_to_files = defaultdict(list)
    
    for root, dirs, files in os.walk(target_dir):
        # 过滤隐藏目录
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__']]
        
        for file in files:
            file_path = Path(root) / file
            if file_path.suffix.lower() in extensions:
                file_hash = calculate_md5(str(file_path))
                if file_hash:
                    hash_to_files[file_hash].append(str(file_path))
    
    # 只返回有重复的
    return {h: files for h, files in hash_to_files.items() if len(files) > 1}


def find_similar_content(target_dir: str, threshold: float = 0.8, extensions: Set[str] = None) -> List[Tuple[str, str, float]]:
    """查找内容相似的文件"""
    if extensions is None:
        extensions = {'.md'}
    
    files_content = {}
    
    for root, dirs, files in os.walk(target_dir):
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__']]
        
        for file in files:
            file_path = Path(root) / file
            if file_path.suffix.lower() in extensions:
                content = extract_text_content(str(file_path))
                if len(content) > 100:  # 只处理有足够内容的文件
                    files_content[str(file_path)] = content
    
    similar_pairs = []
    processed = set()
    file_list = list(files_content.keys())
    
    for i, file1 in enumerate(file_list):
        for file2 in file_list[i+1:]:
            pair_key = tuple(sorted([file1, file2]))
            if pair_key in processed:
                continue
            processed.add(pair_key)
            
            similarity = text_similarity(files_content[file1], files_content[file2])
            if similarity >= threshold:
                similar_pairs.append((file1, file2, similarity))
    
    return sorted(similar_pairs, key=lambda x: x[2], reverse=True)


def find_similar_names(target_dir: str, extensions: Set[str] = None) -> List[Tuple[str, str]]:
    """查找文件名相似的文件（可能是版本重复）"""
    if extensions is None:
        extensions = {'.md', '.txt', '.pdf', '.doc', '.docx'}
    
    # 文件名模式：去除版本号、日期等
    def normalize_name(name: str) -> str:
        # 移除版本号：v1, v2, _v1, -v2
        name = re.sub(r'[-_]?v\d+', '', name, flags=re.IGNORECASE)
        # 移除日期：20231225, 2023-12-25
        name = re.sub(r'[-_]?\d{8}', '', name)
        name = re.sub(r'[-_]?\d{4}[-_]\d{2}[-_]\d{2}', '', name)
        # 移除副本标记
        name = re.sub(r'[-_]?(copy|副本|备份|\(\d+\))', '', name, flags=re.IGNORECASE)
        return name.lower().strip()
    
    name_to_files = defaultdict(list)
    
    for root, dirs, files in os.walk(target_dir):
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__']]
        
        for file in files:
            file_path = Path(root) / file
            if file_path.suffix.lower() in extensions:
                normalized = normalize_name(file_path.stem)
                if normalized:
                    name_to_files[normalized].append(str(file_path))
    
    # 返回有重复的
    return [(name, files) for name, files in name_to_files.items() if len(files) > 1]


def generate_report(exact_dups: Dict, similar_content: List, similar_names: List, target_dir: str) -> str:
    """生成 Markdown 格式的重复检测报告"""
    report = []
    report.append("# 重复文档检测报告\n")
    report.append(f"**分析目录**: `{target_dir}`\n")
    
    target_path = Path(target_dir)
    
    # 完全相同的文件
    report.append("## 1. 完全相同的文件（MD5 一致）\n")
    if exact_dups:
        for hash_val, files in exact_dups.items():
            report.append(f"### 哈希: `{hash_val[:8]}...`\n")
            for f in files:
                try:
                    rel_path = Path(f).relative_to(target_path)
                except ValueError:
                    rel_path = f
                report.append(f"- `{rel_path}`")
            report.append("")
        report.append(f"**共发现 {len(exact_dups)} 组完全重复文件**\n")
    else:
        report.append("✅ 未发现完全重复的文件\n")
    
    # 内容相似的文件
    report.append("## 2. 内容相似的文件（相似度 ≥ 80%）\n")
    if similar_content:
        report.append("| 文件 1 | 文件 2 | 相似度 |")
        report.append("|--------|--------|--------|")
        for f1, f2, sim in similar_content[:20]:  # 限制显示数量
            try:
                rel1 = Path(f1).relative_to(target_path)
                rel2 = Path(f2).relative_to(target_path)
            except ValueError:
                rel1, rel2 = f1, f2
            report.append(f"| `{rel1}` | `{rel2}` | {sim:.1%} |")
        report.append(f"\n**共发现 {len(similar_content)} 对相似文件**\n")
    else:
        report.append("✅ 未发现高度相似的文件\n")
    
    # 文件名相似（可能版本重复）
    report.append("## 3. 文件名相似（可能是版本重复）\n")
    if similar_names:
        for normalized_name, files in similar_names[:15]:
            report.append(f"### 基础名: `{normalized_name}`\n")
            for f in files:
                try:
                    rel_path = Path(f).relative_to(target_path)
                except ValueError:
                    rel_path = f
                report.append(f"- `{rel_path}`")
            report.append("")
        report.append(f"**共发现 {len(similar_names)} 组文件名相似的文件**\n")
    else:
        report.append("✅ 未发现文件名相似的重复文件\n")
    
    # 建议操作
    report.append("## 4. 建议操作\n")
    total_issues = len(exact_dups) + len(similar_content) + len(similar_names)
    if total_issues > 0:
        report.append("1. **完全重复**：保留一份，删除其他副本")
        report.append("2. **内容相似**：合并内容或建立引用关系")
        report.append("3. **版本重复**：保留最新版本，归档或删除旧版本")
    else:
        report.append("✅ 文档库状态良好，无需去重操作")
    
    return "\n".join(report)


def main():
    if len(sys.argv) < 2:
        print("用法: python find_duplicates.py <目标目录> [--threshold 0.8]")
        print("示例: python find_duplicates.py ./docs")
        sys.exit(1)
    
    target_dir = sys.argv[1]
    threshold = 0.8
    
    # 解析阈值参数
    if '--threshold' in sys.argv:
        idx = sys.argv.index('--threshold')
        if idx + 1 < len(sys.argv):
            try:
                threshold = float(sys.argv[idx + 1])
            except ValueError:
                pass
    
    if not os.path.exists(target_dir):
        print(f"错误: 目录不存在 - {target_dir}", file=sys.stderr)
        sys.exit(1)
    
    print(f"正在分析目录: {target_dir}", file=sys.stderr)
    print("1/3 检测完全重复文件...", file=sys.stderr)
    exact_dups = find_exact_duplicates(target_dir)
    
    print("2/3 检测内容相似文件...", file=sys.stderr)
    similar_content = find_similar_content(target_dir, threshold=threshold)
    
    print("3/3 检测文件名相似...", file=sys.stderr)
    similar_names = find_similar_names(target_dir)
    
    report = generate_report(exact_dups, similar_content, similar_names, target_dir)
    print(report)


if __name__ == '__main__':
    main()
