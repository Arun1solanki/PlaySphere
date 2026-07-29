#!/usr/bin/env python3
from pathlib import Path
import json
import re
import sys
import xml.etree.ElementTree as ET

try:
    import yaml
except ImportError:
    yaml = None

root = Path(__file__).resolve().parents[1]
issues: list[str] = []

for path in root.rglob('*.json'):
    try:
        json.loads(path.read_text(encoding='utf-8'))
    except Exception as exc:
        issues.append(f'Invalid JSON: {path.relative_to(root)}: {exc}')

try:
    ET.parse(root / 'backend' / 'pom.xml')
except Exception as exc:
    issues.append(f'Invalid Maven XML: {exc}')

if yaml:
    for path in root.rglob('*.yml'):
        try:
            yaml.safe_load(path.read_text(encoding='utf-8'))
        except Exception as exc:
            issues.append(f'Invalid YAML: {path.relative_to(root)}: {exc}')

frontend = root / 'frontend' / 'src'
for path in frontend.rglob('*'):
    if path.suffix not in {'.ts', '.tsx'}:
        continue
    text = path.read_text(encoding='utf-8')
    for match in re.finditer(r"from\s+['\"](\.[^'\"]+)['\"]", text):
        value = match.group(1)
        base = path.parent / value
        candidates = [base, base.with_suffix('.ts'), base.with_suffix('.tsx'), base / 'index.ts', base / 'index.tsx']
        if not any(candidate.exists() for candidate in candidates):
            issues.append(f'Missing frontend import {value} in {path.relative_to(root)}')

java_root = root / 'backend' / 'src' / 'main' / 'java'
for path in java_root.rglob('*.java'):
    text = path.read_text(encoding='utf-8')
    for match in re.finditer(r'^import\s+(com\.playsphere\.[\w.]+);', text, re.MULTILINE):
        class_name = match.group(1)
        target = java_root / Path(*class_name.split('.')).with_suffix('.java')
        if not target.exists():
            issues.append(f'Missing Java import {class_name} in {path.relative_to(root)}')

if issues:
    print('\n'.join(issues))
    sys.exit(1)
print('PlaySphere structural checks passed.')
