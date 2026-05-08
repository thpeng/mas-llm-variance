#!/usr/bin/env bash
set -euo pipefail

case "${1:-}" in
  qwen)
    INPUT_FILE="../../../../../main/results/rundreise_qwen3_8b.txt"
    OUTPUT_FILE="qwen3-8b-e5-embedding-response.json"
    START_PATTERN='Hier ist eine **Rundreise'
    NEXT_PATTERN='Hier ist eine \*\*Rundreise'
    ;;
  sonnet)
    INPUT_FILE="../../../../../main/results/rundreise_sonnet45.txt"
    OUTPUT_FILE="sonnet45-e5-embedding-response.json"
    START_PATTERN='# Rundreise'
    NEXT_PATTERN='# Rundreise'
    ;;
  *)
    echo "usage: $0 qwen|sonnet" >&2
    exit 2
    ;;
esac

OUTPUT_DIR="e5-results"
PAYLOAD_FILE="${OUTPUT_DIR}/payload-${1}.json"
mkdir -p "$OUTPUT_DIR"

curl -s -X POST http://localhost:8000/load > "${OUTPUT_DIR}/load-${1}.json"

export INPUT_FILE START_PATTERN NEXT_PATTERN
python3 - "$PAYLOAD_FILE" <<'PY'
import json
import os
import re
import sys

output_file = sys.argv[1]
input_file = os.environ["INPUT_FILE"]
start_pattern = os.environ["START_PATTERN"]
next_pattern = os.environ["NEXT_PATTERN"]

with open(input_file, "r", encoding="utf-8") as f:
    raw = f.read()

start = raw.find(start_pattern)
if start < 0:
    raise SystemExit(f"start marker not found: {start_pattern}")

body = raw[start:]
body = re.sub(r"\r?\nProcess finished with exit code 0\s*$", "", body)
parts = re.split(r",\r?\n(?=" + next_pattern + r")", body)
texts = []
for part in parts:
    value = part.strip()
    if value.endswith(","):
        value = value[:-1].strip()
    if value:
        texts.append("passage: " + value)

if not texts:
    raise SystemExit("no model outputs found")

with open(output_file, "w", encoding="utf-8") as f:
    json.dump({"texts": texts}, f, ensure_ascii=False)
PY

curl -s -X POST http://localhost:8000/embed \
  -H "Content-Type: application/json" \
  --data-binary "@${PAYLOAD_FILE}" \
  > "$OUTPUT_FILE"

curl -s -X POST http://localhost:8000/unload > "${OUTPUT_DIR}/unload-${1}.json"

count=$(python3 - <<PY
import json
with open("$OUTPUT_FILE", "r", encoding="utf-8") as f:
    print(json.load(f).get("count"))
PY
)

echo "written $OUTPUT_FILE with $count embeddings"
