# Kreativ-Generativer Marketingtext

## Ziel

Für den Archetyp "kreativ-generativer Marketingtext" wird eine einfache regelbasierte Auswertung eingeführt.

Der Prompt lautet:

```text
Erstelle einen kurzen Werbetext für ein deutsches Publikum, der die Stadt Luzern in drei Sätzen positiv als Reiseziel beschreibt.
```

Bei diesem Use Case existiert kein eindeutig richtiger Zieltext.
Unterschiedliche Formulierungen, touristische Argumente und Schwerpunktsetzungen sind zulässig.
Die Auswertung soll deshalb nicht semantisch clustern und keine fachliche Referenz rekonstruieren.

Stattdessen wird geprüft, ob der minimale Rahmen des Prompts eingehalten wird:

- Die Antwort besteht aus genau drei Sätzen.
- Die Antwort erwähnt Luzern.

Die bestehende generische `LiteralAnalysis` bleibt zusätzlich aktiv.
BLEU und ROUGE können ergänzend über die gesamte Antwortmenge berechnet werden, um die oberflächennahe Formulierungsvarianz zu messen.

## Scope und Non-Goals

Diese Spec beschreibt einen use-case-spezifischen Analysepfad für kreative Marketingtexte.

Nicht Ziel dieses Increments:

- Keine semantische Embedding-Auswertung.
- Keine DBSCAN- oder hierarchische Clusterbildung.
- Keine Referenzantwort.
- Keine Prüfung, ob der Text tatsächlich "gut" oder überzeugend ist.
- Keine Sentiment-Analyse.
- Keine Prüfung auf deutsches Publikum.
- Keine LLM-basierte Bewertung.
- Keine Korrektur von Rechtschreibung oder Stil.

## Konfiguration

Der Analysepfad soll als eigener Algorithmus konfigurierbar sein.

```yaml
analysis:
  clusteringAlgorithm: CREATIVE_MARKETING_TEXT
  creativeMarketingText:
    expectedSentenceCount: 3
    requiredTerm: "Luzern"
```

Die bestehenden DBSCAN-, Hierarchical-, Route-, Factual-Travel-Info- und Literal-Format-Traveler-Guidance-Konfigurationen werden bei diesem Algorithmus nicht verwendet.

## Verhältnis Zu Bestehenden Analysen

Die bestehende `LiteralAnalysis` bleibt unverändert.
Sie beantwortet weiterhin die Frage, wie stark die Antworten innerhalb einer Versuchsreihe literal stabil sind.

Für diesen Archetyp sind zusätzlich BLEU und ROUGE über die gesamte Antwortmenge sinnvoll.
Sie werden nicht als Qualitätsmetrik interpretiert, sondern als Oberflächenvarianz:

- Hohe BLEU-/ROUGE-Ähnlichkeit: ähnliche Formulierungen über Wiederholungen.
- Niedrige BLEU-/ROUGE-Ähnlichkeit: stärkere syntaktische beziehungsweise lexikalische Varianz.

Wichtig:

Ein niedriger BLEU-/ROUGE-Wert ist bei kreativ-generativen Antworten nicht automatisch schlecht.

## Input

Eine Versuchsreihe besteht aus `N` Antworten zum gleichen Prompt, Modell und gleicher Run-Konfiguration.

Jede Antwort ist freier Text.

## Pro-Antwort-Auswertung

Für jede Antwort wird ein Ergebnis erzeugt.

### 1. Raw Trim

```java
rawTrimmed = rawResponse.trim()
```

Nur führende und folgende Whitespace-Zeichen werden entfernt.

### 2. Satzanzahl

Die Anzahl Sätze wird über eine einfache Heuristik bestimmt:

- Zähle Satzendzeichen `.`, `!`, `?`, sofern danach Whitespace oder Textende folgt.
- Zeilenumbrüche alleine zählen nicht als Satzende.
- Mehrere Satzendzeichen am gleichen Satzende, zum Beispiel `!`, zählen als ein Satzende.

Beispiele:

```text
Luzern begeistert am Vierwaldstättersee. Die Altstadt ist charmant. Die Berge liegen direkt vor der Tür.
```

ergibt:

```text
sentenceCount = 3
```

```text
Luzern begeistert am Vierwaldstättersee und bietet eine charmante Altstadt.
```

ergibt:

```text
sentenceCount = 1
```

### 3. Luzern-Erwähnung

`containsRequiredTerm` ist `true`, wenn die Antwort den konfigurierten Begriff enthält.

Für den aktuellen Use Case gilt:

```text
requiredTerm = Luzern
```

Die Prüfung soll case-sensitive erfolgen.

Beispiele:

```text
Luzern begeistert ...
```

ergibt:

```text
containsRequiredTerm = true
```

```text
lucerne begeistert ...
```

ergibt:

```text
containsRequiredTerm = false
```

## Klassifikation

```java
enum CreativeMarketingTextStatus {
    SUCCESS,
    OUTLIER
}
```

Eine Antwort ist `SUCCESS`, wenn beide Bedingungen erfüllt sind:

- `sentenceCount == expectedSentenceCount`
- `containsRequiredTerm == true`

Sonst ist sie `OUTLIER`.

Zusätzlich werden Fehlergründe gespeichert:

- `sentence_count_mismatch`
- `required_term_missing`

## Domain-Modell

### Extraction / Evaluation Result

```java
record CreativeMarketingTextExtraction(
        int responseIndex,
        String rawResponse,
        String rawTrimmed,
        int sentenceCount,
        int expectedSentenceCount,
        boolean containsRequiredTerm,
        String requiredTerm,
        CreativeMarketingTextStatus status,
        List<String> failureReasons
) {
}
```

### Analysis

```java
record CreativeMarketingTextAnalysis(
        int responseCount,
        int successCount,
        int outlierCount,
        double successShare,
        List<Integer> outliers,
        int expectedSentenceCount,
        String requiredTerm,
        int sentenceCountMismatchCount,
        int requiredTermMissingCount,
        List<CreativeMarketingTextExtraction> extractions,
        SyntacticAnalysis syntactic
) {
}
```

Definitionen:

- `successShare = successCount / responseCount`
- `outliers`: alle Antworten mit `status == OUTLIER`
- `sentenceCountMismatchCount`: Anzahl Antworten, deren Satzanzahl nicht der Erwartung entspricht
- `requiredTermMissingCount`: Anzahl Antworten ohne den konfigurierten Begriff
- `syntactic`: BLEU-/ROUGE-Auswertung über alle erfolgreichen Antworten oder, falls keine Antwort erfolgreich ist, leer

Division-by-zero:

Wenn `responseCount == 0`, soll die Analyse analog zu den bestehenden Analysepfaden mit einer klaren Exception abbrechen.

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
        CreativeMarketingTextAnalysis creativeMarketingText,
        LiteralAnalysis literal
) {
}
```

Bei `clusteringAlgorithm: CREATIVE_MARKETING_TEXT` gilt:

- `scans` ist leer.
- `route` ist `null`.
- `factualTravelInfo` ist `null`.
- `literalFormatTravelerGuidance` ist `null`.
- `creativeMarketingText` ist gesetzt.
- `literal` wird weiterhin über alle Rohantworten berechnet.
- Embedding-Service wird nicht aufgerufen.
- Semantic Clustering wird nicht ausgeführt.
- BLEU-/ROUGE-Cluster werden nicht auf semantischen Clustern berechnet.

## Analyzer-Orchestrierung

Verhalten im Haupt-Analyzer:

```text
always:
  compute LiteralAnalysis over all raw responses

if clusteringAlgorithm == CREATIVE_MARKETING_TEXT:
  compute CreativeMarketingTextAnalysis
  set scans = empty
  set route = null
  set factualTravelInfo = null
  set literalFormatTravelerGuidance = null
  set creativeMarketingText = result
  set literal = generic LiteralAnalysis
  do not call embedding service
  do not run DBSCAN
  do not run hierarchical clustering
  do not run route analysis
  do not run factual travel info analysis
  do not run literal format traveler guidance analysis
```

## Beispiele

### Success

Input:

```text
Luzern begeistert mit seiner malerischen Lage am Vierwaldstättersee. Die historische Altstadt, die Kapellbrücke und die nahen Berge machen die Stadt besonders reizvoll. Für Reisende aus Deutschland ist Luzern ein charmantes Ziel für Kultur, Natur und entspannte Auszeiten.
```

Ergebnis:

```text
sentenceCount = 3
containsRequiredTerm = true
status = SUCCESS
outlier = false
```

### Outlier Wegen Falscher Satzanzahl

Input:

```text
Luzern begeistert mit seiner malerischen Lage am Vierwaldstättersee. Die historische Altstadt und die nahen Berge machen die Stadt besonders reizvoll.
```

Ergebnis:

```text
sentenceCount = 2
containsRequiredTerm = true
status = OUTLIER
failureReasons = [sentence_count_mismatch]
outlier = true
```

### Outlier Wegen Fehlender Luzern-Erwähnung

Input:

```text
Die Stadt begeistert mit ihrer malerischen Lage am Vierwaldstättersee. Die historische Altstadt und die nahen Berge machen sie besonders reizvoll. Für Reisende aus Deutschland ist sie ein charmantes Ziel für Kultur, Natur und entspannte Auszeiten.
```

Ergebnis:

```text
sentenceCount = 3
containsRequiredTerm = false
status = OUTLIER
failureReasons = [required_term_missing]
outlier = true
```

## Implementierungsschritte

1. `ClusteringAlgorithm` um `CREATIVE_MARKETING_TEXT` erweitern.
2. `AnalysisConfig` um `CreativeMarketingTextConfig` erweitern.
3. YAML-Mapping für `analysis.creativeMarketingText.expectedSentenceCount` und `analysis.creativeMarketingText.requiredTerm` ergänzen.
4. Domain-Records für Extraction und Analysis anlegen.
5. `CreativeMarketingTextStatus` anlegen.
6. `CreativeMarketingTextAnalyzer` implementieren.
7. Analyzer-Orchestrierung um den neuen Pfad erweitern.
8. `AnalysisResult` um optionales Feld `creativeMarketingText` erweitern.
9. Sicherstellen, dass `LiteralAnalyzer` weiterhin unverändert für alle Rohantworten läuft.
10. Sicherstellen, dass bei diesem Algorithmus kein Embedding-Service aufgerufen wird.
11. Unit-Tests für Satzanzahl, Luzern-Erwähnung und Klassifikation ergänzen.
12. Integrationstest mit gemischten Antworten ergänzen.

## Testfälle

### Genau Drei Sätze Und Luzern Enthalten

Antwort:

```text
Luzern begeistert mit seiner Lage am Vierwaldstättersee. Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. Für Reisende aus Deutschland ist Luzern ein ideales Ziel für Kultur, Natur und Erholung.
```

Erwartung:

```text
status = SUCCESS
sentenceCount = 3
containsRequiredTerm = true
```

### Nur Zwei Sätze

Antwort:

```text
Luzern begeistert mit seiner Lage am Vierwaldstättersee. Die Altstadt und die Kapellbrücke machen die Stadt zu einem idealen Reiseziel.
```

Erwartung:

```text
status = OUTLIER
failureReasons = [sentence_count_mismatch]
```

### Luzern Fehlt

Antwort:

```text
Die Stadt begeistert mit ihrer Lage am Vierwaldstättersee. Die Altstadt und die Kapellbrücke schaffen eine besondere Atmosphäre. Für Reisende aus Deutschland ist sie ein ideales Ziel für Kultur, Natur und Erholung.
```

Erwartung:

```text
status = OUTLIER
failureReasons = [required_term_missing]
```
