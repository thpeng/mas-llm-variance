# Literal-Formatkritische Reisendenlenkung

## Ziel

Für den Archetyp "literal-formatkritische Reisendenlenkung" wird eine referenzbasierte Auswertung eingeführt.

Bei diesem Use Case ist nicht nur der fachliche Inhalt relevant, sondern auch die konkrete Formulierung und das Ausgabeformat. Die Antwort soll möglichst exakt einer erwarteten Zielantwort entsprechen. Kleine Whitespace-Abweichungen sollen separat toleriert und ausgewiesen werden, ohne Gross-/Kleinschreibung, Punktuation oder Wörter zu normalisieren.

Die bestehende generische `LiteralAnalysis` bleibt zusätzlich aktiv. Sie misst die literale Stabilität innerhalb der Versuchsreihe. Die neue Analyse misst die Konformität gegenüber einer Referenzantwort. Diese beiden Konzepte dürfen nicht vermischt werden.

## Formatvorlage

Die Modelle erhalten eine Formatvorlage dieser Art:

```text
Reisende ab Sargans bis Chur benützen ab Sargans bis Landquart die Linien RE5
oder S12 und ab Landquart bis Chur die Linie S1.
Reisende ab Muttenz bis Basel SBB benützen ab Muttenz bis Basel, St. Jakob die
Busse der Linie 47 und ab Basel, St. Jakob bis Basel SBB die Ersatzbusse (EV).
```

Die Formatvorlage dient nur als Stil- und Strukturbeispiel. Inhalte aus der Vorlage dürfen nicht in die Zielantwort übernommen werden.

## Referenz

Die erwartete Zielantwort lautet exakt:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.
```

## Scope und Non-Goals

Diese Spec beschreibt einen referenzbasierten Analysepfad für einen literal- und formatkritischen Reisehinweis.

Nicht Ziel dieses Increments:

- Keine semantische Embedding-Auswertung.
- Keine DBSCAN- oder hierarchische Clusterbildung.
- Keine BLEU-/ROUGE-basierte Bewertung als Primärmetrik.
- Keine Korrektur von Rechtschreibung oder Umlauten.
- Keine Normalisierung von Gross-/Kleinschreibung.
- Keine Normalisierung von Punktuation.
- Keine fachliche Rekonstruktion, wenn die Antwort anders formuliert ist.
- Keine LLM-basierte Bewertung.
- Keine Erweiterung oder Änderung der Semantik von `LiteralAnalyzer`.

## Konfiguration

Der Analysepfad soll als eigener Algorithmus konfigurierbar sein.

```yaml
analysis:
  clusteringAlgorithm: LITERAL_FORMAT_TRAVELER_GUIDANCE
  literalFormatTravelerGuidance:
    reference: "Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3."
```

Die bestehenden DBSCAN-, Hierarchical-, Route- und Factual-Travel-Info-Konfigurationen werden bei diesem Algorithmus nicht verwendet.

## Verhältnis Zur Bestehenden LiteralAnalysis

Die bestehende `LiteralAnalysis` bleibt unverändert.

Sie beantwortet diese Frage:

```text
Wie stark sind die Antworten innerhalb einer Versuchsreihe literal stabil?
```

Beispiele für bestehende Metriken:

- `allResponsesIdentical`
- `responseCount`
- `distinctResponseCount`
- `exactMatchRate`
- optionale `LiteralClusterAnalysis`

Die neue `LiteralFormatTravelerGuidanceAnalysis` beantwortet eine andere Frage:

```text
Wie stark entsprechen die Antworten der erwarteten Referenzausgabe?
```

Beispiel: 20x exakt dieselbe falsche Antwort.

Dann gilt:

```text
LiteralAnalysis:
allResponsesIdentical = true
distinctResponseCount = 1
exactMatchRate = 1.0

LiteralFormatTravelerGuidanceAnalysis:
exactMatchShare = 0.0
noMatchShare = 1.0
```

Daraus folgt:

- `LiteralAnalyzer` nicht umbauen.
- `LiteralFormatTravelerGuidanceAnalyzer` separat implementieren.
- Beide Analysen können im `AnalysisResult` gleichzeitig enthalten sein.

## Input

Eine Versuchsreihe besteht aus `N` Antworten zum gleichen Prompt, Modell und gleicher Run-Konfiguration.

Jede Antwort ist freier Text. Die Antwort kann exakt der Referenz entsprechen, nur Whitespace-Unterschiede enthalten oder formal beziehungsweise fachlich abweichen.

## Pro-Antwort-Auswertung

Für jede Antwort wird ein Vergleichsergebnis erzeugt.

### 1. Raw Trim

```java
rawTrimmed = rawResponse.trim()
```

Nur führende und folgende Whitespace-Zeichen werden entfernt.

### 2. Exact Match

```java
exactMatch = rawTrimmed.equals(reference)
```

`exactMatch` ist `true`, wenn die getrimmte Rohantwort exakt gleich der Referenz ist.

### 3. Normalized Exact Match

`normalizedExactMatch` ist `true`, wenn Antwort und Referenz nach Whitespace-Normalisierung gleich sind.

Whitespace-Normalisierung:

1. `trim`
2. alle Tabs, Zeilenumbrüche und mehrfachen Leerzeichen durch ein einzelnes Leerzeichen ersetzen
3. keine Änderung an Gross-/Kleinschreibung
4. keine Änderung an Punktuation
5. keine Änderung an Wörtern

Beispiel:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf
die Linie S3.
```

wird zu:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.
```

### 4. Klassifikation

```java
enum LiteralFormatTravelerGuidanceClassification {
    EXACT_MATCH,
    NORMALIZED_EXACT_MATCH,
    NO_MATCH
}
```

Regeln:

- `EXACT_MATCH`, wenn `exactMatch == true`
- `NORMALIZED_EXACT_MATCH`, wenn `exactMatch == false && normalizedExactMatch == true`
- `NO_MATCH` sonst

Wichtig: Semantisch ähnliche Paraphrasen gelten nicht als Erfolg. Wenn die Richtlinienformulierung verletzt wird, ist die Antwort `NO_MATCH`.

Beispiele für `NO_MATCH`:

```text
Reisende von Bern nach Zürich sollen ab Bern bis Bern Wankdorf die Linie S3 verwenden.
Reisende ab Bern bis Zürich verwenden ab Bern bis Bern Wankdorf die Linie S3.
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3
```

Der letzte Fall ist `NO_MATCH`, weil der Punkt fehlt. Punktuation wird nicht normalisiert.

## Diagnoseflags Für NO_MATCH

Für Antworten mit `classification == NO_MATCH` werden Diagnoseflags berechnet. Diese Flags dienen nur der Fehlerdiagnose, nicht der Primärklassifikation.

### Zielinhaltsflags

Diese Flags zeigen, ob zentrale Zielbestandteile enthalten sind:

- `hasBern`
- `hasZurich`
- `hasBernWankdorf`
- `hasS3`

Empfohlene Prüfung:

- `hasBern`: Antwort enthält `Bern`
- `hasZurich`: Antwort enthält `Zürich`
- `hasBernWankdorf`: Antwort enthält `Bern Wankdorf`
- `hasS3`: Antwort enthält `S3`

Die Prüfung soll case-sensitive erfolgen, weil der Use Case literal-formatkritisch ist.

Falls später gewünscht, kann eine separate case-insensitive Diagnose ergänzt werden. Die Klassifikation darf davon nicht abhängen.

### Forbidden Template Content

`containsForbiddenTemplateContent` ist `true`, wenn eine Antwort Begriffe aus der Formatvorlage enthält, die nicht in der Zielantwort vorkommen.

Forbidden Terms:

```text
Sargans
Chur
Landquart
Muttenz
Basel
St. Jakob
RE5
S12
S1
47
EV
```

Die Prüfung ist als Diagnose gedacht. Eine Antwort mit solchen Begriffen ist ohnehin `NO_MATCH`, weil sie nicht exakt der Referenz entspricht.

Wichtig: `S1` darf nicht fälschlich in `S12` oder `S10` matchen. Für alphanumerische Linienbegriffe sollen Wort- oder Token-Grenzen verwendet werden.

### Additional Sentence Candidate

`containsAdditionalSentenceCandidate` ist `true`, wenn die Antwort mehr als einen Satz enthält.

Heuristik:

- Whitespace normalisieren ist dafür nicht nötig, aber erlaubt.
- Zähle Satzendzeichen `.`, `!`, `?`, sofern danach Whitespace oder Textende folgt.
- Wenn Anzahl Satzendzeichen > 1, dann `true`.
- Zeilenumbrüche alleine zählen nicht als zusätzlicher Satz.

Beispiel:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3. Dies ist die schnellste Verbindung.
```

ergibt:

```text
containsAdditionalSentenceCandidate = true
```

Die Heuristik bleibt bewusst einfach. Sie ist keine linguistische Satzsegmentierung.

## Domain-Modell

### Extraction / Comparison Result

```java
record LiteralFormatTravelerGuidanceExtraction(
        int responseIndex,
        String rawResponse,
        String rawTrimmed,
        boolean exactMatch,
        boolean normalizedExactMatch,
        LiteralFormatTravelerGuidanceClassification classification,
        boolean hasBern,
        boolean hasZurich,
        boolean hasBernWankdorf,
        boolean hasS3,
        boolean containsForbiddenTemplateContent,
        List<String> forbiddenTemplateTerms,
        boolean containsAdditionalSentenceCandidate
) {
}
```

Die Diagnoseflags können für `EXACT_MATCH` und `NORMALIZED_EXACT_MATCH` ebenfalls berechnet werden, sind aber primär für `NO_MATCH` relevant.

### Analysis

```java
record LiteralFormatTravelerGuidanceAnalysis(
        int responseCount,
        int exactMatchCount,
        int normalizedExactMatchCount,
        int noMatchCount,
        double exactMatchShare,
        double normalizedAcceptedShare,
        List<Integer> outliers,
        Map<String, Integer> forbiddenTemplateTermCounts,
        int additionalSentenceCandidateCount,
        List<LiteralFormatTravelerGuidanceExtraction> extractions
) {
}
```

Definitionen:

- `exactMatchShare = exactMatchCount / responseCount`
- `normalizedAcceptedShare = (exactMatchCount + normalizedExactMatchCount) / responseCount`
- `outliers`: alle Antworten mit `classification == NO_MATCH`
- `forbiddenTemplateTermCounts`: zählt gefundene verbotene Template-Terme über alle Antworten
- `additionalSentenceCandidateCount`: Anzahl Antworten mit mehr als einem Satz

Division-by-zero:

Wenn `responseCount == 0`, sollen Shares `0.0` sein oder die Analyse soll mit einer klaren Exception abbrechen. Entscheide konsistent mit bestehenden Analysepfaden.

## Integration In AnalysisResult

Der neue Analysepfad soll analog zu den anderen use-case-spezifischen Analysen als optionales Feld im `AnalysisResult` erscheinen.

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        List<AnalysisScan> scans,
        RouteAnalysis route,
        FactualTravelInfoAnalysis factualTravelInfo,
        LiteralFormatTravelerGuidanceAnalysis literalFormatTravelerGuidance,
        LiteralAnalysis literal
) {
}
```

Bei `clusteringAlgorithm: LITERAL_FORMAT_TRAVELER_GUIDANCE` gilt:

- `scans` ist leer.
- `route` ist `null`.
- `factualTravelInfo` ist `null`.
- `literalFormatTravelerGuidance` ist gesetzt.
- `literal` wird weiterhin über alle Rohantworten berechnet.
- Embedding-Service wird nicht aufgerufen.
- Semantic Clustering wird nicht ausgeführt.
- BLEU-/ROUGE-Cluster werden nicht berechnet.

## Analyzer-Orchestrierung

Verhalten im Haupt-Analyzer:

```text
always:
  compute LiteralAnalysis over all raw responses

if clusteringAlgorithm == LITERAL_FORMAT_TRAVELER_GUIDANCE:
  compute LiteralFormatTravelerGuidanceAnalysis
  set scans = empty
  set route = null
  set factualTravelInfo = null
  set literalFormatTravelerGuidance = result
  set literal = generic LiteralAnalysis
  do not call embedding service
  do not run DBSCAN
  do not run hierarchical clustering
  do not run route analysis
  do not run factual travel info analysis
```

## Beispiele

### Exact Match

Input:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.
```

Ergebnis:

```text
exactMatch = true
normalizedExactMatch = true
classification = EXACT_MATCH
outlier = false
```

### Normalized Exact Match

Input:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf
die Linie S3.
```

Ergebnis:

```text
exactMatch = false
normalizedExactMatch = true
classification = NORMALIZED_EXACT_MATCH
outlier = false
```

### No Match Mit Zusatzsatz

Input:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3. Dies gilt tagsüber.
```

Ergebnis:

```text
classification = NO_MATCH
containsAdditionalSentenceCandidate = true
outlier = true
```

### No Match Mit Template-Leak

Input:

```text
Reisende ab Sargans bis Chur benützen ab Sargans bis Landquart die Linien RE5 oder S12.
```

Ergebnis:

```text
classification = NO_MATCH
containsForbiddenTemplateContent = true
forbiddenTemplateTerms = [Sargans, Chur, Landquart, RE5, S12]
hasBern = false
hasZurich = false
hasBernWankdorf = false
hasS3 = false
outlier = true
```

### No Match Mit Falscher Linie

Input:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S4.
```

Ergebnis:

```text
classification = NO_MATCH
hasBern = true
hasZurich = true
hasBernWankdorf = true
hasS3 = false
outlier = true
```

### No Match Wegen Fehlender Punktuation

Input:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3
```

Ergebnis:

```text
classification = NO_MATCH
exactMatch = false
normalizedExactMatch = false
outlier = true
```

### No Match Wegen Paraphrase

Input:

```text
Reisende von Bern nach Zürich sollen von Bern bis Bern Wankdorf die Linie S3 verwenden.
```

Ergebnis:

```text
classification = NO_MATCH
hasBern = true
hasZurich = true
hasBernWankdorf = true
hasS3 = true
outlier = true
```

## Implementierungsschritte

1. `ClusteringAlgorithm` um `LITERAL_FORMAT_TRAVELER_GUIDANCE` erweitern.
2. `AnalysisConfig` um `LiteralFormatTravelerGuidanceConfig` erweitern.
3. YAML-Mapping für `analysis.literalFormatTravelerGuidance.reference` ergänzen.
4. Domain-Records für Extraction und Analysis anlegen.
5. `LiteralFormatTravelerGuidanceClassification` anlegen.
6. `LiteralFormatTravelerGuidanceAnalyzer` implementieren.
7. Analyzer-Orchestrierung um den neuen Pfad erweitern.
8. `AnalysisResult` um optionales Feld `literalFormatTravelerGuidance` erweitern.
9. Sicherstellen, dass `LiteralAnalyzer` weiterhin unverändert für alle Rohantworten läuft.
10. Sicherstellen, dass bei diesem Algorithmus kein Embedding-Service aufgerufen wird.
11. Golden-Fixtures anpassen.
12. Unit-Tests für Whitespace-Normalisierung, Klassifikation und Diagnoseflags ergänzen.
13. Integrationstest mit gemischten Antworten ergänzen.

## Testfälle

### Whitespace-Normalisierung

Referenz:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.
```

Antwort:

```text
  Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf
  die Linie S3.  
```

Erwartung:

```text
classification = NORMALIZED_EXACT_MATCH
```

### Keine Case-Normalisierung

Antwort:

```text
reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3.
```

Erwartung:

```text
classification = NO_MATCH
```

### Keine Punktuations-Normalisierung

Antwort:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3
```

Erwartung:

```text
classification = NO_MATCH
```

### Forbidden Template Content

Antwort:

```text
Reisende ab Sargans bis Chur benützen ab Sargans bis Landquart die Linien RE5 oder S12.
```

Erwartung:

```text
classification = NO_MATCH
containsForbiddenTemplateContent = true
forbiddenTemplateTerms = [Sargans, Chur, Landquart, RE5, S12]
```

### Zusatzsatz

Antwort:

```text
Reisende ab Bern bis Zürich benützen ab Bern bis Bern Wankdorf die Linie S3. Bitte beachten Sie die Anzeigen am Bahnhof.
```

Erwartung:

```text
classification = NO_MATCH
containsAdditionalSentenceCandidate = true
```

### Paraphrase Ist Kein Erfolg

Antwort:

```text
Reisende von Bern nach Zürich sollen von Bern bis Bern Wankdorf die Linie S3 verwenden.
```

Erwartung:

```text
classification = NO_MATCH
hasBern = true
hasZurich = true
hasBernWankdorf = true
hasS3 = true
```

