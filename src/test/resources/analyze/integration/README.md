# Multilingual E5 Analysis Integration Fixture

`rundreise-responses.txt` contains one generated LLM response per line.

To activate `MultilingualE5AnalysisIntegrationTest`, call the local E5 service
with these lines in the same order and paste the complete JSON response into
`rundreise-e5-embedding-response.json`.

Example payload shape:

```json
{
  "texts": [
    "passage: Bern, Zürich",
    "passage: Zürich, Bern"
  ]
}
```

The Java analysis code adds the `passage:` prefix when it calls the HTTP
service. For this fixture-based test the pasted response only needs the raw
`dim`, `count`, and `embeddings` fields returned by `/embed`.
