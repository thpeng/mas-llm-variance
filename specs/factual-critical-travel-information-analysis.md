# Faktisch-Kritische Reiseinformation

## Ziel

Fuer den Use Case "faktisch-kritische Reiseinformation" wird ein eigener, deterministischer Analysepfad eingefuehrt.

Ziel ist nicht, die sprachliche Aehnlichkeit der Antworten zu bewerten. Stattdessen sollen aus jeder LLM-Antwort die fachlich relevanten Informationen extrahiert und gegen eine erwartete Referenz geprueft werden.

Der Archetyp deckt Antworten ab, bei denen einzelne Fakten korrekt sein muessen, weil nachgelagerte Systeme oder Nutzer daraus konkrete Reiseentscheidungen ableiten koennen.

## Prompt-Kontext

Die getesteten Modelle beantworten folgenden Prompt:

```text
Extrahiere aus folgendem Text die Abfahrtszeit ab Bern, die Ankunftszeit in Zuerich HB und die Anzahl Umstiege.
Antworte in einem kurzen Satz.

Text: Die Verbindung von Lausanne nach Zuerich HB faehrt um 06:45 in Lausanne ab, ab Bern um 08:02, ab Olten um 08:34, ab Brugg um 08:55 und kommt um 09:15 in Zuerich HB an.
Es ist eine direkte Verbindung.
```

## Fachliche Referenz

```text
departureFromBern = 08:02
arrivalAtZurich = 09:15
changes = 0
```

Die Zeiten `06:45`, `08:34` und `08:55` sind Ablenkungswerte. Sie duerfen nicht als Abfahrt ab Bern oder Ankunft in Zuerich HB gewertet werden.

Die Formulierung "direkte Verbindung" bedeutet `0` Umstiege.

## Scope und Non-Goals

Diese Spec beschreibt eine use-case-spezifische Analyse fuer eine einzelne fachliche Aufgabe.

Nicht Ziel dieses Increments:

- Keine allgemeine Information-Extraction-Engine.
- Keine LLM-basierte Extraktion.
- Keine semantische Bewertung mit Embeddings.
- Keine Toleranz fuer fachlich falsche Zeiten.
- Keine Korrektur aufgrund des Quelltexts waehrend der Analyse. Bewertet wird nur die Modellantwort.
- Keine Bewertung der sprachlichen Qualitaet der Antwort.

## Konfiguration

Der Analysepfad soll als eigener Algorithmus konfigurierbar sein.

Vorschlag:

```yaml
analysis:
  clusteringAlgorithm: FACTUAL_TRAVEL_INFO
  factualTravelInfo:
    departureFromBern: "08:02"
    arrivalAtZurich: "09:15"
    changes: 0
```

Alternativ kann der Block spaeter generischer benannt werden, z.B. `factExtraction`, falls weitere faktisch-kritische Use Cases folgen. Fuer dieses Increment ist ein use-case-spezifischer Block ausdruecklich ausreichend.

Die bestehenden DBSCAN-, Hierarchical- und Route-Konfigurationsbloecke werden bei diesem Algorithmus nicht verwendet.

## Input

Eine Versuchsreihe besteht aus `N` Antworten zum gleichen Prompt, Modell und gleicher Run-Konfiguration.

Jede Antwort ist freier Text, typischerweise z.B.:

```text
Die Verbindung faehrt um 08:02 ab Bern, kommt um 09:15 in Zuerich HB an und hat keine Umstiege.
```

oder:

```text
Abfahrt ab Bern: 08:02, Ankunft Zuerich HB: 09:15, Umstiege: keine.
```

## Zeitwerte Extrahieren

Aus jeder Antwort werden alle Zeitangaben extrahiert.

Akzeptierte Zeitformate:

- `HH:mm`
- `H:mm`
- `HH.mm`
- `H.mm`

Beispiele:

```text
08:02
8:02
09:15
9:15
08.02
8.02
09.15
9.15
```

Alle erkannten Zeiten werden auf `HH:mm` normalisiert.

Beispiele:

```text
8:02  -> 08:02
8.02  -> 08:02
9.15  -> 09:15
09.15 -> 09:15
```

Regex-Vorschlag:

```regex
\b([01]?\d|2[0-3])[:.]([0-5]\d)\b
```

Die Normalisierung erfolgt durch:

1. Stunde als Integer parsen.
2. Minute als zweistellige Gruppe uebernehmen.
3. Ausgabe als `String.format("%02d:%02d", hour, minute)`.

## Erwartete Zeiten Pruefen

Eine Antwort erfuellt die Zeitbedingung, wenn beide erwarteten Zeiten in den normalisierten Zeiten enthalten sind:

- `08:02`
- `09:15`

Zusatzzeiten werden separat markiert, machen die Antwort aber nicht automatisch falsch.

Beispiel:

```text
Die Verbindung ab Lausanne faehrt um 06:45, ab Bern um 08:02 und kommt um 09:15 in Zuerich HB an. Keine Umstiege.
```

Diese Antwort enthaelt eine Zusatzzeit `06:45`, kann aber fachlich trotzdem `SUCCESS` sein, weil die erwarteten Fakten enthalten sind.

Statuslogik:

- Wenn `08:02` fehlt: `departureFound = false`
- Wenn `09:15` fehlt: `arrivalFound = false`
- Alle erkannten Zeiten, die nicht `08:02` oder `09:15` sind, werden in `extraTimes` gespeichert.

Wenn eine Antwort nur Ablenkungszeiten wie `06:45`, `08:34` oder `08:55` enthaelt und eine erwartete Zeit fehlt, wird sie als `OUTLIER` klassifiziert.

## Umstiege Pruefen

Die Antwort muss ausdruecken, dass die Verbindung `0` Umstiege hat.

Akzeptierte Muster, case-insensitive:

```text
0 Umstieg
0 Umstiege
null Umstieg
null Umstiege
keine Umstiege
kein Umstieg
kein Umsteigen
ohne Umstieg
ohne Umstiege
ohne umzusteigen
umstiegsfrei
direkte Verbindung
Direktverbindung
direkt
no changes
no transfers
0 changes
0 transfers
aucun changement
aucune correspondance
senza cambi
0 cambi
```

Bei Treffer wird normalisiert auf:

```text
changes = 0
```

Zusaetzlich wird die konkret gefundene Rohphrase gespeichert:

```text
detectedChangeExpression = "keine Umstiege"
```

Wenn kein akzeptiertes Muster gefunden wird:

```text
changesFound = false
```

## Klassifikation

Eine Antwort ist `SUCCESS`, wenn alle drei fachlichen Bedingungen erfuellt sind:

```text
departureFromBern == 08:02
arrivalAtZurich == 09:15
changes == 0
```

Sonst ist sie `OUTLIER`.

Die Klassifikation ist bewusst streng. Wenn eine erwartete Information fehlt, wird die Antwort als Outlier behandelt, auch wenn andere Teile korrekt sind.

## Detailstatus Pro Antwort

Pro Antwort wird ein Extraktionsergebnis gespeichert.

Vorschlag:

```java
record FactualTravelInfoExtraction(
        int responseIndex,
        String rawResponse,
        List<String> normalizedTimes,
        List<String> extraTimes,
        boolean departureFound,
        boolean arrivalFound,
        boolean changesFound,
        Integer changes,
        String detectedChangeExpression,
        FactualTravelInfoStatus status,
        List<String> failureReasons
) {
}
```

Status:

```java
enum FactualTravelInfoStatus {
    SUCCESS,
    OUTLIER
}
```

Failure-Reasons:

```text
departure_missing
arrival_missing
changes_missing
```

Optional, falls spaeter feiner unterschieden werden soll:

```text
departure_wrong_or_missing
arrival_wrong_or_missing
```

Fuer dieses Increment reicht `departure_missing` und `arrival_missing`, weil die Analyse nur prueft, ob die erwarteten Zeiten in der Antwort enthalten sind. Ablenkungszeiten werden ueber `extraTimes` sichtbar.

## Aggregation Pro Versuchsreihe

Die Analyse aggregiert ueber alle Antworten:

- `responseCount`
- `successCount`
- `outlierCount`
- `successShare`
- `outliers`
- `departureFoundCount`
- `arrivalFoundCount`
- `changesFoundCount`
- `extraTimeCounts`
- `extractions`

Vorschlag:

```java
record FactualTravelInfoAnalysis(
        int responseCount,
        int successCount,
        int outlierCount,
        double successShare,
        List<Integer> outliers,
        int departureFoundCount,
        int arrivalFoundCount,
        int changesFoundCount,
        Map<String, Integer> extraTimeCounts,
        List<FactualTravelInfoExtraction> extractions
) {
}
```

`outliers` enthaelt die `responseIndex`-Werte aller Antworten mit Status `OUTLIER`.

`extraTimeCounts` zaehlt alle Zusatzzeiten ueber die Versuchsreihe, z.B.:

```json
{
  "06:45": 3,
  "08:34": 1
}
```

## Beispiele

### Success: Kurzer Satz

Input:

```text
Die Verbindung faehrt um 08:02 ab Bern, kommt um 09:15 in Zuerich HB an und hat keine Umstiege.
```

Ergebnis:

```text
status = SUCCESS
normalizedTimes = [08:02, 09:15]
extraTimes = []
departureFound = true
arrivalFound = true
changesFound = true
changes = 0
detectedChangeExpression = "keine Umstiege"
failureReasons = []
```

### Success: Punkt-Notation

Input:

```text
Abfahrt 08.02, Ankunft 9.15, 0 Umstiege.
```

Ergebnis:

```text
status = SUCCESS
normalizedTimes = [08:02, 09:15]
extraTimes = []
departureFound = true
arrivalFound = true
changesFound = true
changes = 0
detectedChangeExpression = "0 Umstiege"
failureReasons = []
```

### Outlier: Ablenkungszeit Statt Bern-Abfahrt

Input:

```text
Abfahrt 08:34, Ankunft 09:15, keine Umstiege.
```

Ergebnis:

```text
status = OUTLIER
normalizedTimes = [08:34, 09:15]
extraTimes = [08:34]
departureFound = false
arrivalFound = true
changesFound = true
changes = 0
detectedChangeExpression = "keine Umstiege"
failureReasons = [departure_missing]
```

### Outlier: Umstiege Fehlen

Input:

```text
Die Verbindung faehrt um 08:02 ab Bern und kommt um 09:15 in Zuerich HB an.
```

Ergebnis:

```text
status = OUTLIER
normalizedTimes = [08:02, 09:15]
extraTimes = []
departureFound = true
arrivalFound = true
changesFound = false
changes = null
detectedChangeExpression = null
failureReasons = [changes_missing]
```

### Success Mit Zusatzzeit

Input:

```text
Die Verbindung startet in Lausanne um 06:45, faehrt ab Bern um 08:02, kommt um 09:15 in Zuerich HB an und ist direkt.
```

Ergebnis:

```text
status = SUCCESS
normalizedTimes = [06:45, 08:02, 09:15]
extraTimes = [06:45]
departureFound = true
arrivalFound = true
changesFound = true
changes = 0
detectedChangeExpression = "direkt"
failureReasons = []
```

## Integration In AnalysisResult

Der neue Analysepfad soll analog zur Route-Analyse als optionales Feld im `AnalysisResult` erscheinen.

Vorschlag:

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        List<AnalysisScan> scans,
        RouteAnalysis route,
        FactualTravelInfoAnalysis factualTravelInfo,
        LiteralAnalysis literal,
        SyntacticAnalysis syntactic
) {
}
```

Bei `clusteringAlgorithm: FACTUAL_TRAVEL_INFO` gilt:

- `scans` ist leer.
- `route` ist `null`.
- `factualTravelInfo` ist gesetzt.
- `literal` wird weiterhin ueber alle Antworten berechnet.
- `syntactic` kann entweder ueber die erfolgreiche Menge und Outlier-Gruppen berechnet werden oder initial leer bleiben. Fuer die erste Version wird empfohlen, syntaktische Cluster anhand der fachlichen Klassifikation zu bilden:
  - Cluster `0`: alle `SUCCESS`-Antworten
  - Outlier: alle `OUTLIER`-Antworten

## Implementierungsschritte

1. `ClusteringAlgorithm` um `FACTUAL_TRAVEL_INFO` erweitern.
2. Konfiguration um `FactualTravelInfoConfig` erweitern.
3. YAML-Mapping fuer `analysis.factualTravelInfo` ergaenzen.
4. Domain-Records fuer Extraktion und Analyse anlegen.
5. `FactualTravelInfoAnalyzer` implementieren.
6. `Analyzer` um den neuen Analysepfad erweitern.
7. JSON-Serialisierung in `AnalysisResult` pruefen.
8. Unit-Tests fuer Zeitnormalisierung und Umstiegsphrasen ergaenzen.
9. Integrationstest mit gemischten `SUCCESS`- und `OUTLIER`-Antworten ergaenzen.
10. Bestehende Tests anpassen, falls `AnalysisResult` ein neues Feld bekommt.

## Testfaelle

### Zeitnormalisierung

- `8:02` -> `08:02`
- `08:02` -> `08:02`
- `8.02` -> `08:02`
- `08.02` -> `08:02`
- `9:15` -> `09:15`
- `09.15` -> `09:15`

### Umstiegsphrasen

Alle akzeptierten Muster muessen `changes = 0` ergeben.

Mindestens testen:

- `keine Umstiege`
- `kein Umstieg`
- `ohne Umstieg`
- `direkte Verbindung`
- `Direktverbindung`
- `direkt`
- `no changes`
- `aucun changement`
- `senza cambi`
- `0 cambi`

### Integration

Empfohlene Antworten fuer einen Integrationstest:

```text
Die Verbindung faehrt um 08:02 ab Bern, kommt um 09:15 in Zuerich HB an und hat keine Umstiege.
```

```text
Abfahrt ab Bern: 08:02, Ankunft Zuerich HB: 09:15, Umstiege: keine.
```

```text
Abfahrt 08.02, Ankunft 9.15, 0 Umstiege.
```

```text
Die Verbindung faehrt um 06:45 ab Lausanne und kommt um 09:15 in Zuerich HB an.
```

```text
Abfahrt 08:34, Ankunft 09:15, keine Umstiege.
```

Erwartung:

- 3 `SUCCESS`
- 2 `OUTLIER`
- Outlier-Indizes fuer die letzten zwei Antworten
- `extraTimeCounts` enthaelt `06:45` und `08:34`

