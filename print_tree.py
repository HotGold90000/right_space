#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import argparse

# 为特定文件/目录添加注释
ANNOTATIONS = {
    ".github/workflows/update-json.yml": "GitHub Actions 工作流",
    "build.gradle.kts": "Gradle 构建脚本",
    "gradle/wrapper": "Gradle Wrapper 配置（会自动生成）",
    "gradlew": "Gradle 可执行脚本 (Linux/Mac)",
    "gradlew.bat": "Gradle 可执行脚本 (Windows)",
    "settings.gradle.kts": "Gradle 设置",
    "src/main/kotlin/UpdateJson.kt": "你的 Kotlin 主程序",
    "data.json": "存储数据的 JSON 文件",
}

TEMP_DIRS = ["build", ".gradle", ".idea", ".gradle"]  # 可按需扩展

def get_max_length(root, ignore_temp=False):
    max_len = 0
    for dirpath, dirnames, filenames in os.walk(root):
        if ignore_temp:
            dirnames[:] = [d for d in dirnames if d not in TEMP_DIRS]
        for name in dirnames + filenames:
            rel_path = os.path.relpath(os.path.join(dirpath, name), root).replace("\\", "/")
            if rel_path in ANNOTATIONS:
                max_len = max(max_len, len(name))
    return max_len

def print_tree(root, prefix="", max_name_len=0, depth=None, ignore_temp=False, show_annotations=True, current_depth=0, output_file=None):
    if depth is not None and current_depth > depth:
        return
    try:
        entries = sorted(os.listdir(root))
    except PermissionError:
        return
    entries_count = len(entries)
    for index, entry in enumerate(entries):
        if ignore_temp and entry in TEMP_DIRS:
            continue
        path = os.path.join(root, entry)
        connector = "└── " if index == entries_count - 1 else "├── "
        rel_path = os.path.relpath(path, ".").replace("\\", "/")
        annotation = ""
        if show_annotations and rel_path in ANNOTATIONS:
            padding = " " * (max_name_len - len(entry) + 2)
            annotation = f"{padding}# {ANNOTATIONS[rel_path]}"
        line = f"{prefix}{connector}{entry}{annotation}"
        if output_file:
            print(line, file=output_file)
        else:
            print(line)
        if os.path.isdir(path):
            extension_prefix = "    " if index == entries_count - 1 else "│   "
            print_tree(
                path,
                prefix + extension_prefix,
                max_name_len=max_name_len,
                depth=depth,
                ignore_temp=ignore_temp,
                show_annotations=show_annotations,
                current_depth=current_depth + 1,
                output_file=output_file
            )

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="打印 Android 工程文件结构树")
    parser.add_argument("--ignore-temp", action="store_true", help="忽略临时目录（build/.gradle 等）")
    parser.add_argument("--max-depth", type=int, help="最大递归深度")
    parser.add_argument("--no-annotations", action="store_true", help="不打印注释")
    parser.add_argument("--output", type=str, help="输出到文件，而不是终端")
    args = parser.parse_args()

    max_len = get_max_length(".", ignore_temp=args.ignore_temp)
    output_file = open(args.output, "w", encoding="utf-8") if args.output else None
    print_tree(
        ".",
        max_name_len=max_len,
        depth=args.max_depth,
        ignore_temp=args.ignore_temp,
        show_annotations=not args.no_annotations,
        output_file=output_file
    )
    if output_file:
        output_file.close()
