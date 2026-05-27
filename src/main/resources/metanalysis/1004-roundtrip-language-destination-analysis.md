# Roundtrip prompt language destination analysis

Scope: main_100_iterations, prompt evaluation ADVISORY_RECOMMENDATION_SWISS_ROUND_TRIP, languages DE/FR/IT/EN, baseline roundtrip plans only. Destination order is ignored.

- Series included: 27
- Models with all four languages: 6
- Language pairs compared: 36
- Mean weighted Jaccard across language pairs: 0.528
- Perfect weighted matches: 2
- Global station mentions: 12505
- Expected-vs-observed baseline: per-model destination distribution, i.e. `P(destination | model)`

## Strongest language effects

| model | languages | weighted_jaccard | set_jaccard | top set A | top set B |
| --- | --- | ---: | ---: | --- | --- |
| swiss-ai_apertus-8b-instruct-2509 | DE-EN | 0 | 0 | BERN|LAUTERBRUNNEN|LUCERNE|ST_MORITZ|ZURICH |  |
| swiss-ai_apertus-8b-instruct-2509 | FR-EN | 0 | 0 | BASEL|GENEVE|LAUSANNE|LUCERNE|ZERMATT |  |
| swiss-ai_apertus-8b-instruct-2509 | IT-EN | 0 | 0 | BERN|LAUSANNE|LUCERNE|ST_MORITZ|ZERMATT |  |
| swiss-ai_apertus-8b-instruct-2509 | DE-FR | 0.111111 | 0.1 | BERN|LAUTERBRUNNEN|LUCERNE|ST_MORITZ|ZURICH | BASEL|GENEVE|LAUSANNE|LUCERNE|ZERMATT |
| gemini-3.5-flash | FR-IT | 0.25 | 0.25 | GENEVE|INTERLAKEN|LUCERNE|ZERMATT|ZURICH | GRINDELWALD|LUCERNE|LUGANO|SCHAFFHAUSEN|ZERMATT |
| gemini-3.5-flash | IT-EN | 0.25 | 0.25 | GRINDELWALD|LUCERNE|LUGANO|SCHAFFHAUSEN|ZERMATT | GENEVE|INTERLAKEN|LUCERNE|ZERMATT|ZURICH |
| claude-sonnet-4-6 | DE-IT | 0.256739 | 0.363636 | GENEVE|INTERLAKEN|LUCERNE|ZERMATT|ZURICH | BERN|GENEVE|GRUYERES|LUCERNE|LUGANO |
| claude-sonnet-4-6 | IT-EN | 0.317872 | 0.545455 | BERN|GENEVE|GRUYERES|LUCERNE|LUGANO | GENEVE|INTERLAKEN|LUCERNE|ZERMATT|ZURICH |
| claude-sonnet-4-6 | FR-IT | 0.408895 | 0.545455 | BERN|GENEVE|INTERLAKEN|LUCERNE|ZURICH | BERN|GENEVE|GRUYERES|LUCERNE|LUGANO |
| gemini-3.5-flash | DE-IT | 0.428571 | 0.428571 | INTERLAKEN|LUCERNE|LUGANO|ZERMATT|ZURICH | GRINDELWALD|LUCERNE|LUGANO|SCHAFFHAUSEN|ZERMATT |

## Destination profile by prompt language

| language | destination | share | count | series |
| --- | --- | ---: | ---: | ---: |
| DE | LUCERNE | 0.2 | 900 | 9 |
| DE | ZURICH | 0.2 | 900 | 9 |
| DE | INTERLAKEN | 0.178 | 801 | 9 |
| DE | ZERMATT | 0.177778 | 800 | 9 |
| DE | GENEVE | 0.133333 | 600 | 9 |
| DE | BASEL | 0.022222 | 100 | 9 |
| DE | BERN | 0.022222 | 100 | 9 |
| DE | LUGANO | 0.022222 | 100 | 9 |
| DE | ST_MORITZ | 0.022222 | 100 | 9 |
| DE | LAUTERBRUNNEN | 0.022 | 99 | 9 |
| EN | GENEVE | 0.2 | 401 | 6 |
| EN | LUCERNE | 0.2 | 401 | 6 |
| EN | INTERLAKEN | 0.183541 | 368 | 6 |
| EN | ZERMATT | 0.172569 | 346 | 6 |
| EN | ZURICH | 0.150125 | 301 | 6 |
| EN | GRINDELWALD | 0.049875 | 100 | 6 |
| EN | LAUSANNE | 0.026933 | 54 | 6 |
| EN | BERN | 0.016958 | 34 | 6 |
| FR | LUCERNE | 0.2 | 600 | 6 |
| FR | GENEVE | 0.166667 | 500 | 6 |
| FR | ZERMATT | 0.136 | 408 | 6 |
| FR | ZURICH | 0.130667 | 392 | 6 |
| FR | INTERLAKEN | 0.127 | 381 | 6 |
| FR | LAUSANNE | 0.108 | 324 | 6 |
| FR | BERN | 0.054333 | 163 | 6 |
| FR | BASEL | 0.033333 | 100 | 6 |
| FR | MONTREUX | 0.033333 | 100 | 6 |
| FR | ST_MORITZ | 0.010667 | 32 | 6 |
| IT | LUCERNE | 0.2 | 600 | 6 |
| IT | ZURICH | 0.106 | 318 | 6 |
| IT | ZERMATT | 0.103 | 309 | 6 |
| IT | GENEVE | 0.1 | 300 | 6 |
| IT | INTERLAKEN | 0.1 | 300 | 6 |
| IT | BERN | 0.09 | 270 | 6 |
| IT | BASEL | 0.066333 | 199 | 6 |
| IT | LUGANO | 0.066333 | 199 | 6 |
| IT | LAUSANNE | 0.039 | 117 | 6 |
| IT | ST_MORITZ | 0.035333 | 106 | 6 |
| IT | GRINDELWALD | 0.033333 | 100 | 6 |
| IT | SCHAFFHAUSEN | 0.033333 | 100 | 6 |
| IT | GRUYERES | 0.027 | 81 | 6 |
| IT | DAVOS | 0.000333 | 1 | 6 |

## Expected vs observed destination mentions

Expected probability is the destination's share within the same model across all prompt languages, P(destination | model). Expected count is that probability multiplied by the station mentions in the model-language series. The tables below exclude tiny series with fewer than 100 station mentions.

### Largest over-representation

| model | language | destination | expected_probability | observed_probability | delta_probability | expected_count | observed_count |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| gemini-3.5-flash | IT | SCHAFFHAUSEN | 0.05 | 0.2 | 0.15 | 25 | 100 |
| gemini-3.5-flash | IT | GRINDELWALD | 0.05 | 0.2 | 0.15 | 25 | 100 |
| qwen/qwen3.5-9b | EN | GRINDELWALD | 0.05 | 0.2 | 0.15 | 25 | 100 |
| qwen/qwen3.5-9b | FR | MONTREUX | 0.05 | 0.2 | 0.15 | 25 | 100 |
| qwen/qwen3.5-9b | IT | BASEL | 0.05 | 0.2 | 0.15 | 25 | 100 |
| gpt-5.4-mini-2026-03-17 | FR | LAUSANNE | 0.049 | 0.186 | 0.137 | 24.5 | 93 |
| openai/gpt-oss-20b | FR | LAUSANNE | 0.066445 | 0.2 | 0.133555 | 33.223 | 100 |
| swiss-ai_apertus-8b-instruct-2509 | DE | ZURICH | 0.066667 | 0.2 | 0.133333 | 33.333 | 100 |
| swiss-ai_apertus-8b-instruct-2509 | FR | GENEVE | 0.066667 | 0.2 | 0.133333 | 33.333 | 100 |
| swiss-ai_apertus-8b-instruct-2509 | FR | BASEL | 0.066667 | 0.2 | 0.133333 | 33.333 | 100 |
| swiss-ai_apertus-8b-instruct-2509 | DE | LAUTERBRUNNEN | 0.066 | 0.198 | 0.132 | 33 | 99 |
| claude-sonnet-4-6 | DE | ZERMATT | 0.074 | 0.2 | 0.126 | 37 | 100 |

### Largest under-representation

| model | language | destination | expected_probability | observed_probability | delta_probability | expected_count | observed_count |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| gemini-3.5-flash | IT | INTERLAKEN | 0.15 | 0 | -0.15 | 75 | 0 |
| gemini-3.5-flash | IT | ZURICH | 0.15 | 0 | -0.15 | 75 | 0 |
| qwen/qwen3.5-9b | IT | ZERMATT | 0.15 | 0 | -0.15 | 75 | 0 |
| swiss-ai_apertus-8b-instruct-2509 | DE | LAUSANNE | 0.133333 | 0 | -0.133333 | 66.667 | 0 |
| swiss-ai_apertus-8b-instruct-2509 | DE | ZERMATT | 0.133333 | 0 | -0.133333 | 66.667 | 0 |
| swiss-ai_apertus-8b-instruct-2509 | FR | BERN | 0.133333 | 0 | -0.133333 | 66.667 | 0 |
| swiss-ai_apertus-8b-instruct-2509 | FR | ST_MORITZ | 0.133333 | 0 | -0.133333 | 66.667 | 0 |
| openai/gpt-oss-20b | FR | BASEL | 0.132226 | 0 | -0.132226 | 66.113 | 0 |
| gpt-5.4-mini-2026-03-17 | FR | INTERLAKEN | 0.156 | 0.024 | -0.132 | 78 | 12 |
| claude-sonnet-4-6 | IT | ZURICH | 0.159 | 0.036 | -0.123 | 79.5 | 18 |
| claude-sonnet-4-6 | IT | INTERLAKEN | 0.118 | 0 | -0.118 | 59 | 0 |
| claude-sonnet-4-6 | DE | BERN | 0.101 | 0 | -0.101 | 50.5 | 0 |

## BFS language-region aggregation

Destination language regions are taken from the BFS report `be-d-40-langreg-01.pdf` and stored as Java reference data in `RoundTripBfsDestinationLanguageRegion`. D=German, F=French, I=Italian, R=Romansh.

### Largest BFS-region over-representation

| model | prompt language | BFS region | expected_probability | observed_probability | delta_probability | expected_count | observed_count | destinations |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| swiss-ai_apertus-8b-instruct-2509 | DE | German | 0.8 | 1 | 0.2 | 400 | 500 | BASEL|BERN|INTERLAKEN|LAUTERBRUNNEN|LUCERNE|ST_MORITZ|ZERMATT|ZURICH |
| swiss-ai_apertus-8b-instruct-2509 | FR | French | 0.2 | 0.4 | 0.2 | 100 | 200 | GENEVE|LAUSANNE |
| qwen/qwen3.5-9b | FR | French | 0.25 | 0.4 | 0.15 | 125 | 200 | GENEVE|MONTREUX |
| gpt-5.4-mini-2026-03-17 | FR | French | 0.249 | 0.386 | 0.137 | 124.5 | 193 | GENEVE|LAUSANNE |
| openai/gpt-oss-20b | FR | French | 0.06711 | 0.2 | 0.13289 | 33.555 | 100 | GENEVE|LAUSANNE |
| claude-sonnet-4-6 | DE | German | 0.671 | 0.8 | 0.129 | 335.5 | 400 | BERN|DAVOS|INTERLAKEN|LUCERNE|ST_MORITZ|ZERMATT|ZURICH |
| claude-sonnet-4-6 | IT | Italian | 0.04 | 0.16 | 0.12 | 20 | 80 | LUGANO |
| gemini-3.5-flash | DE | Italian | 0.1 | 0.2 | 0.1 | 50 | 100 | LUGANO |
| gemini-3.5-flash | EN | French | 0.1 | 0.2 | 0.1 | 50 | 100 | GENEVE |
| gemini-3.5-flash | FR | French | 0.1 | 0.2 | 0.1 | 50 | 100 | GENEVE |
| gemini-3.5-flash | IT | Italian | 0.1 | 0.2 | 0.1 | 50 | 100 | LUGANO |
| claude-sonnet-4-6 | IT | French | 0.289 | 0.386 | 0.097 | 144.5 | 193 | GENEVE|GRUYERES|LAUSANNE |

### Largest BFS-region under-representation

| model | prompt language | BFS region | expected_probability | observed_probability | delta_probability | expected_count | observed_count | destinations |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| claude-sonnet-4-6 | IT | German | 0.671 | 0.454 | -0.217 | 335.5 | 227 | BERN|DAVOS|INTERLAKEN|LUCERNE|ST_MORITZ|ZERMATT|ZURICH |
| swiss-ai_apertus-8b-instruct-2509 | DE | French | 0.2 | 0 | -0.2 | 100 | 0 | GENEVE|LAUSANNE |
| swiss-ai_apertus-8b-instruct-2509 | FR | German | 0.8 | 0.6 | -0.2 | 400 | 300 | BASEL|BERN|INTERLAKEN|LAUTERBRUNNEN|LUCERNE|ST_MORITZ|ZERMATT|ZURICH |
| qwen/qwen3.5-9b | FR | German | 0.75 | 0.6 | -0.15 | 375 | 300 | BASEL|GRINDELWALD|INTERLAKEN|LUCERNE|ZERMATT|ZURICH |
| openai/gpt-oss-20b | FR | German | 0.93289 | 0.8 | -0.13289 | 466.445 | 400 | BASEL|INTERLAKEN|LUCERNE|ST_MORITZ|ZERMATT|ZURICH |
| gpt-5.4-mini-2026-03-17 | FR | German | 0.7415 | 0.614 | -0.1275 | 370.75 | 307 | BERN|INTERLAKEN|LUCERNE|ZERMATT|ZURICH |
| gemini-3.5-flash | DE | French | 0.1 | 0 | -0.1 | 50 | 0 | GENEVE |
| gemini-3.5-flash | EN | Italian | 0.1 | 0 | -0.1 | 50 | 0 | LUGANO |
| gemini-3.5-flash | FR | Italian | 0.1 | 0 | -0.1 | 50 | 0 | LUGANO |
| gemini-3.5-flash | IT | French | 0.1 | 0 | -0.1 | 50 | 0 | GENEVE |
| claude-sonnet-4-6 | DE | French | 0.289 | 0.2 | -0.089 | 144.5 | 100 | GENEVE|GRUYERES|LAUSANNE |
| openai/gpt-oss-20b | DE | French | 0.06711 | 0 | -0.06711 | 33.555 | 0 | GENEVE|LAUSANNE |

Generated files:

- `1004-roundtrip-language-destination-summary.csv`
- `1004-roundtrip-language-destination-stations.csv`
- `1004-roundtrip-language-destination-language-pairs.csv`
- `1004-roundtrip-language-destination-language-destinations.csv`
- `1004-roundtrip-language-destination-expected-vs-observed.csv`
- `1004-roundtrip-language-destination-bfs-language-region-expected-vs-observed.csv`
