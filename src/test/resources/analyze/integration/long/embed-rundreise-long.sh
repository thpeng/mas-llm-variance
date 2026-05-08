#!/usr/bin/env bash
set -euo pipefail

case "${1:-}" in
  qwen)
    INPUT_FILE="qwen3-8b-answers.txt"
    OUTPUT_FILE="qwen3-8b-e5-embedding-response.json"
    ;;
  sonnet)
    INPUT_FILE="sonnet45-answers.txt"
    OUTPUT_FILE="sonnet45-e5-embedding-response.json"
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

export INPUT_FILE
python3 - "$PAYLOAD_FILE" <<'PY'
import json
import os
import sys

output_file = sys.argv[1]
input_file = os.environ["INPUT_FILE"]
separator = "\n\n===== ANSWER =====\n\n"

with open(input_file, "r", encoding="utf-8") as f:
    parts = f.read().split(separator)

texts = ["passage: " + part.strip() for part in parts if part.strip()]

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
