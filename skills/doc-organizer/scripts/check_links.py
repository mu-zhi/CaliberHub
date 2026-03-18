#!/usr/bin/env python3
"""
Markdown 链接检查脚本

检测内容：
1. 内部链接（相对路径）是否有效
2. 图片引用是否存在
3. 锚点链接是否有效（可选）
"""

import os
import sys
import re
from pathlib import Path
from typing import Dict, List, Set, Tuple
from urllib.parse import unquote


def extract_links_from_markdown(file_path: str) -> Tuple[List[str], List[str]]:
    """从 Markdown 文件中提取链接和图片引用"""
    links = []
    images = []
    
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
    except (IOError, OSError):
        return links, images
    
    # 匹配 Markdown 链接: [text](url)
    link_pattern = r'\[([^\]]*)\]\(([^)]+)\)'
    for match in re.finditer(link_pattern, content):
        url = match.group(2).strip()
        # 排除外部链接和锚点
        if not url.startswith(('http://', 'https://', 'mailto:', '#')):
            # 处理带锚点的链接
            url = url.split('#')[0]
            if url:
                links.append(unquote(url))
    
    # 匹配图片: ![alt](src)
    img_pattern = r'!\[([^\]]*)\]\(([^)]+)\)'
    for match in re.finditer(img_pattern, content):
        src = match.group(2).strip()
        if not src.startswith(('http://', 'https://', 'data:')):
            images.append(unquote(src))
    
    # 匹配 HTML 图片标签
    html_img_pattern = r'<img[^>]+src=["\']([^"\']+)["\']'
    for match in re.finditer(html_img_pattern, content, re.IGNORECASE):
        src = match.group(1).strip()
        if not src.startswith(('http://', 'https://', 'data:')):
            images.append(unquote(src))
    
    return links, images


def check_link_validity(source_file: str, link: str, base_dir: str) -> Tuple[bool, str]:
    """检查链接是否有效"""
    source_path = Path(source_file)
    source_dir = source_path.parent
    
    # 处理相对路径
    if link.startswith('/'):
        # 绝对路径（相对于基目录）
        target_path = Path(base_dir) / link.lstrip('/')
    else:
        # 相对路径
        target_path = source_dir / link
    
    # 规范化路径
    try:
        target_path = target_path.resolve()
    except (OSError, ValueError):
        return False, f"路径解析失败: {link}"
    
    if target_path.exists():
        return True, ""
    else:
        return False, str(target_path)


def scan_markdown_files(target_dir: str) -> Dict[str, Dict]:
    """扫描目录中的所有 Markdown 文件并检查链接"""
    results = {
        'total_files': 0,
        'total_links': 0,
        'total_images': 0,
        'broken_links': [],
        'broken_images': [],
        'orphan_images': [],
    }
    
    # 收集所有图片引用
    all_referenced_images: Set[str] = set()
    # 收集所有存在的图片
    all_existing_images: Set[str] = set()
    
    target_path = Path(target_dir)
    
    # 先收集所有图片文件
    image_extensions = {'.png', '.jpg', '.jpeg', '.gif', '.svg', '.webp', '.bmp'}
    for root, dirs, files in os.walk(target_dir):
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__']]
        for file in files:
            if Path(file).suffix.lower() in image_extensions:
                all_existing_images.add(str(Path(root) / file))
    
    # 扫描 Markdown 文件
    for root, dirs, files in os.walk(target_dir):
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in ['node_modules', '__pycache__']]
        
        for file in files:
            if not file.endswith('.md'):
                continue
            
            file_path = str(Path(root) / file)
            results['total_files'] += 1
            
            links, images = extract_links_from_markdown(file_path)
            results['total_links'] += len(links)
            results['total_images'] += len(images)
            
            # 检查链接
            for link in links:
                is_valid, error_msg = check_link_validity(file_path, link, target_dir)
                if not is_valid:
                    try:
                        rel_source = Path(file_path).relative_to(target_path)
                    except ValueError:
                        rel_source = file_path
                    results['broken_links'].append({
                        'source': str(rel_source),
                        'link': link,
                        'error': error_msg
                    })
            
            # 检查图片
            for img in images:
                is_valid, resolved_path = check_link_validity(file_path, img, target_dir)
                if is_valid:
                    all_referenced_images.add(resolved_path)
                else:
                    try:
                        rel_source = Path(file_path).relative_to(target_path)
                    except ValueError:
                        rel_source = file_path
                    results['broken_images'].append({
                        'source': str(rel_source),
                        'image': img,
                        'error': resolved_path
                    })
    
    # 查找孤立图片（存在但未被引用）
    for img_path in all_existing_images:
        resolved = str(Path(img_path).resolve())
        if resolved not in all_referenced_images:
            try:
                rel_path = Path(img_path).relative_to(target_path)
            except ValueError:
                rel_path = img_path
            results['orphan_images'].append(str(rel_path))
    
    return results


def generate_report(results: Dict, target_dir: str) -> str:
    """生成 Markdown 格式的链接检查报告"""
    report = []
    report.append("# 链接检查报告\n")
    report.append(f"**分析目录**: `{target_dir}`\n")
    
    # 概览
    report.append("## 概览\n")
    report.append(f"| 指标 | 数量 |")
    report.append(f"|------|------|")
    report.append(f"| 扫描文件数 | {results['total_files']} |")
    report.append(f"| 内部链接数 | {results['total_links']} |")
    report.append(f"| 图片引用数 | {results['total_images']} |")
    report.append(f"| 失效链接 | {len(results['broken_links'])} |")
    report.append(f"| 失效图片 | {len(results['broken_images'])} |")
    report.append(f"| 孤立图片 | {len(results['orphan_images'])} |")
    report.append("")
    
    # 失效链接
    report.append("## 失效链接\n")
    if results['broken_links']:
        report.append("| 源文件 | 失效链接 |")
        report.append("|--------|----------|")
        for item in results['broken_links'][:50]:  # 限制显示数量
            report.append(f"| `{item['source']}` | `{item['link']}` |")
        if len(results['broken_links']) > 50:
            report.append(f"\n... 还有 {len(results['broken_links']) - 50} 条未显示")
        report.append("")
    else:
        report.append("✅ 所有内部链接均有效\n")
    
    # 失效图片
    report.append("## 失效图片引用\n")
    if results['broken_images']:
        report.append("| 源文件 | 失效图片 |")
        report.append("|--------|----------|")
        for item in results['broken_images'][:50]:
            report.append(f"| `{item['source']}` | `{item['image']}` |")
        if len(results['broken_images']) > 50:
            report.append(f"\n... 还有 {len(results['broken_images']) - 50} 条未显示")
        report.append("")
    else:
        report.append("✅ 所有图片引用均有效\n")
    
    # 孤立图片
    report.append("## 孤立图片（未被引用）\n")
    if results['orphan_images']:
        report.append("以下图片存在但未被任何 Markdown 文件引用：\n")
        for img in results['orphan_images'][:30]:
            report.append(f"- `{img}`")
        if len(results['orphan_images']) > 30:
            report.append(f"\n... 还有 {len(results['orphan_images']) - 30} 个未显示")
        report.append("")
    else:
        report.append("✅ 所有图片均被引用\n")
    
    # 建议操作
    report.append("## 建议操作\n")
    has_issues = (results['broken_links'] or results['broken_images'] or results['orphan_images'])
    if has_issues:
        if results['broken_links']:
            report.append(f"1. 修复 {len(results['broken_links'])} 个失效链接")
        if results['broken_images']:
            report.append(f"2. 修复 {len(results['broken_images'])} 个失效图片引用")
        if results['orphan_images']:
            report.append(f"3. 考虑清理 {len(results['orphan_images'])} 个孤立图片以减少仓库体积")
    else:
        report.append("✅ 文档链接状态良好，无需修复")
    
    return "\n".join(report)


def main():
    if len(sys.argv) < 2:
        print("用法: python check_links.py <目标目录>")
        print("示例: python check_links.py ./docs")
        sys.exit(1)
    
    target_dir = sys.argv[1]
    
    if not os.path.exists(target_dir):
        print(f"错误: 目录不存在 - {target_dir}", file=sys.stderr)
        sys.exit(1)
    
    print(f"正在扫描目录: {target_dir}", file=sys.stderr)
    results = scan_markdown_files(target_dir)
    
    report = generate_report(results, target_dir)
    print(report)


if __name__ == '__main__':
    main()
