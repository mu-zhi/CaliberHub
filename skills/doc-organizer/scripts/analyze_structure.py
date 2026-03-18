#!/usr/bin/env python3
"""
文档目录结构分析脚本

扫描指定目录，生成统计报告，包括：
- 文件类型分布
- 目录层级深度
- 文件数量统计
- 大文件检测
"""

import os
import sys
import json
from pathlib import Path
from collections import defaultdict
from typing import Dict, List, Tuple


def analyze_directory(target_dir: str, ignore_patterns: List[str] = None) -> Dict:
    """分析目录结构并返回统计数据"""
    if ignore_patterns is None:
        ignore_patterns = ['.git', 'node_modules', '__pycache__', '.venv', 'venv', '.idea', '.vscode']
    
    target_path = Path(target_dir)
    if not target_path.exists():
        raise FileNotFoundError(f"目录不存在: {target_dir}")
    
    stats = {
        'total_files': 0,
        'total_dirs': 0,
        'total_size_bytes': 0,
        'max_depth': 0,
        'file_types': defaultdict(int),
        'depth_distribution': defaultdict(int),
        'large_files': [],  # > 1MB
        'empty_dirs': [],
        'markdown_files': [],
    }
    
    base_depth = len(target_path.parts)
    
    for root, dirs, files in os.walk(target_dir):
        # 过滤忽略目录
        dirs[:] = [d for d in dirs if d not in ignore_patterns and not d.startswith('.')]
        
        current_path = Path(root)
        current_depth = len(current_path.parts) - base_depth
        stats['max_depth'] = max(stats['max_depth'], current_depth)
        stats['depth_distribution'][current_depth] += 1
        stats['total_dirs'] += 1
        
        # 检测空目录
        if not files and not dirs:
            stats['empty_dirs'].append(str(current_path.relative_to(target_path)))
        
        for file in files:
            if file.startswith('.'):
                continue
                
            file_path = current_path / file
            stats['total_files'] += 1
            
            # 文件类型统计
            suffix = file_path.suffix.lower() or '(无扩展名)'
            stats['file_types'][suffix] += 1
            
            # 文件大小
            try:
                size = file_path.stat().st_size
                stats['total_size_bytes'] += size
                
                # 大文件检测 (> 1MB)
                if size > 1024 * 1024:
                    stats['large_files'].append({
                        'path': str(file_path.relative_to(target_path)),
                        'size_mb': round(size / (1024 * 1024), 2)
                    })
                
                # Markdown 文件收集
                if suffix == '.md':
                    stats['markdown_files'].append({
                        'path': str(file_path.relative_to(target_path)),
                        'size_kb': round(size / 1024, 2)
                    })
            except OSError:
                pass
    
    # 转换 defaultdict 为普通 dict
    stats['file_types'] = dict(stats['file_types'])
    stats['depth_distribution'] = dict(stats['depth_distribution'])
    
    return stats


def generate_report(stats: Dict, target_dir: str) -> str:
    """生成 Markdown 格式的分析报告"""
    report = []
    report.append(f"# 文档目录分析报告\n")
    report.append(f"**分析目录**: `{target_dir}`\n")
    
    # 概览
    report.append("## 概览\n")
    report.append(f"| 指标 | 值 |")
    report.append(f"|------|-----|")
    report.append(f"| 总文件数 | {stats['total_files']} |")
    report.append(f"| 总目录数 | {stats['total_dirs']} |")
    report.append(f"| 总大小 | {stats['total_size_bytes'] / (1024*1024):.2f} MB |")
    report.append(f"| 最大深度 | {stats['max_depth']} 层 |")
    report.append(f"| Markdown 文件数 | {len(stats['markdown_files'])} |")
    report.append("")
    
    # 文件类型分布
    report.append("## 文件类型分布\n")
    report.append("| 类型 | 数量 | 占比 |")
    report.append("|------|------|------|")
    sorted_types = sorted(stats['file_types'].items(), key=lambda x: x[1], reverse=True)
    for ext, count in sorted_types[:15]:
        pct = (count / stats['total_files'] * 100) if stats['total_files'] > 0 else 0
        report.append(f"| {ext} | {count} | {pct:.1f}% |")
    report.append("")
    
    # 目录深度分布
    report.append("## 目录深度分布\n")
    report.append("| 深度 | 目录数 |")
    report.append("|------|--------|")
    for depth in sorted(stats['depth_distribution'].keys()):
        report.append(f"| {depth} 层 | {stats['depth_distribution'][depth]} |")
    report.append("")
    
    # 大文件列表
    if stats['large_files']:
        report.append("## 大文件列表 (> 1MB)\n")
        report.append("| 文件路径 | 大小 |")
        report.append("|----------|------|")
        for f in sorted(stats['large_files'], key=lambda x: x['size_mb'], reverse=True):
            report.append(f"| {f['path']} | {f['size_mb']} MB |")
        report.append("")
    
    # 空目录列表
    if stats['empty_dirs']:
        report.append("## 空目录列表\n")
        for d in stats['empty_dirs']:
            report.append(f"- `{d}`")
        report.append("")
    
    # 问题诊断
    report.append("## 问题诊断\n")
    issues = []
    if stats['max_depth'] > 4:
        issues.append(f"⚠️ 目录层级过深（{stats['max_depth']} 层），建议扁平化至 ≤ 3 层")
    if len(stats['empty_dirs']) > 0:
        issues.append(f"⚠️ 存在 {len(stats['empty_dirs'])} 个空目录，建议清理")
    if len(stats['large_files']) > 0:
        issues.append(f"⚠️ 存在 {len(stats['large_files'])} 个大文件（> 1MB），检查是否应存入 Git LFS 或外部存储")
    
    if issues:
        for issue in issues:
            report.append(f"- {issue}")
    else:
        report.append("✅ 未发现明显问题")
    
    return "\n".join(report)


def main():
    if len(sys.argv) < 2:
        print("用法: python analyze_structure.py <目标目录> [--json]")
        print("示例: python analyze_structure.py ./docs")
        sys.exit(1)
    
    target_dir = sys.argv[1]
    output_json = '--json' in sys.argv
    
    try:
        stats = analyze_directory(target_dir)
        
        if output_json:
            print(json.dumps(stats, ensure_ascii=False, indent=2))
        else:
            report = generate_report(stats, target_dir)
            print(report)
            
    except FileNotFoundError as e:
        print(f"错误: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
