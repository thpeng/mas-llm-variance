# Long Rundreise E5 Integration Fixture

The long-response integration tests use the raw model outputs in:

- `src/main/results/rundreise_qwen3_8b.txt`
- `src/main/results/rundreise_sonnet45.txt`

Those files contain console preambles plus multiple generated answers. The test
harness splits them into one answer per embedding input.

To activate the E5-backed tests, run `embed-rundreise-long.sh` from this
directory inside WSL while `server.py` is listening on port `8000`.

```bash
./embed-rundreise-long.sh qwen
./embed-rundreise-long.sh sonnet
```

The generated JSON files are:

- `qwen3-8b-e5-embedding-response.json`
- `sonnet45-e5-embedding-response.json`

Until those files contain real embeddings, the analysis tests are skipped.
