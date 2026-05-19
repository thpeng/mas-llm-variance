# Route Clustering Algorithm

## Ziel

Für Rundreise-Use-Cases wird ein eigener deterministischer Clusteralgorithmus eingeführt.

Die Route ist der zentrale semantische Inhalt der Antwort. Deshalb soll nicht mehr primär der gesamte Antworttext semantisch eingebettet werden. Stattdessen werden aus jeder Antwort die empfohlenen Stationen extrahiert, normalisiert und als geordnete Route ausgewertet.

Beispielantwort:

```text
1. **Zurich** : Point de départ stratégique offrant un excellent accès aux transports publics et une introduction à la culture urbaine suisse.
2. **Lucerne** : Située sur le lac, elle permet d'admirer les célèbres ponts médiévaux et de commencer l'ascension vers les Alpes.
3. **Interlaken** : Cœur du tourisme alpin, idéal pour explorer la région des Jungfrau et profiter des paysages de montagne spectaculaires.
4. **Zermatt** : Destination incontournable pour voir le Matterhorn et découvrir une ville sans voiture au sommet des Alpes.
5. **Geneve** : Fin de voyage dans une ville cosmopolite connue pour ses horloges, son lac et sa proximité avec la France.
```

Extrahierte Route:

```text
ZURICH|LUCERNE|INTERLAKEN|ZERMATT|GENEVE
```

## Motivation

Die bisherigen embedding-basierten Clusterverfahren sind bei Reiseplänen methodisch schwierig auszuwerten. Längere Antworten können sprachlich ähnlich sein, obwohl sich die entscheidenden Stationen unterscheiden. Umgekehrt können sprachlich unterschiedliche Antworten dieselbe Route beschreiben.

Der neue Algorithmus misst deshalb nicht die Ähnlichkeit des gesamten Texts, sondern die Stabilität der Route:

- Welche vollständigen Routen werden vorgeschlagen?
- Wie häufig tritt jede Route auf?
- Welche Stationen sind stabil?
- Welche Positionen innerhalb der Route sind stabil?
- Welche Antworten konnten nicht zuverlässig in eine Route überführt werden?

Der Algorithmus ist regelbasiert und vermeidet damit zusätzliche Varianz durch ein lokales Extraktions-LLM.

## Scope und Non-Goals

Diese Spec beschreibt einen neuen Analysepfad für Rundreise-Antworten.

Nicht Ziel dieses Increments:

- Keine Bewertung der fachlichen Qualität einer Route.
- Keine Geodaten, Distanzberechnung oder Prüfung der Reise-Logik.
- Keine LLM-basierte Extraktion.
- Keine automatische Korrektur unbekannter Orte über fuzzy matching in der ersten Version.
- Keine Änderung an Run-Logging.

## Konfiguration

Der neue Algorithmus wird als eigener Clustering-Algorithmus konfiguriert.

Vorschlag:

```yaml
analysis:
  clusteringAlgorithm: ROUTE
  route:
    expectedStationCount: 5
```

Die bestehenden DBSCAN- und Hierarchical-Scan-Blöcke können im YAML erhalten bleiben, werden bei `ROUTE` aber nicht verwendet.

`expectedStationCount` ist initial `5`, sollte aber konfigurierbar sein, damit spätere Reisepläne mit anderer Länge möglich sind.

## Domain-Modell

### Destination

Bekannte Ziele werden als Enum modelliert. Jedes Enum enthält bekannte Namensvarianten.

```java
enum Destination {
    GENEVE(Set.of("geneve", "genève", "geneva", "genf", "ginevra")),
    ZURICH(Set.of("zurich", "zürich", "zuerich", "zurigo")),
    LUCERNE(Set.of("lucerne", "luzern", "lucerna")),
    ZERMATT(Set.of("zermatt")),
    INTERLAKEN(Set.of("interlaken")),
    MONTREUX(Set.of("montreux")),
    LUGANO(Set.of("lugano")),
    ST_MORITZ(Set.of("st. moritz", "saint moritz", "sankt moritz")),
    BERN(Set.of("bern", "berne", "berna")),
    BASEL(Set.of("basel", "bâle", "basle", "basilia"))
}
```

Unbekannte Namen werden nicht als Enum-Wert modelliert, sondern in der extrahierten Antwort separat als `unknownNames` gespeichert.

### ExtractionStatus

```java
enum RouteExtractionStatus {
    SUCCESS,
    PARTIAL,
    FAILED
}
```

Semantik:

- `SUCCESS`: Genau `expectedStationCount` Stationsnamen wurden extrahiert und alle konnten normalisiert werden.
- `PARTIAL`: Es wurden Stationsnamen extrahiert, aber nicht genau `expectedStationCount` oder mindestens ein Name ist `UNKNOWN`.
- `FAILED`: Es konnten keine verwertbaren Stationsnamen extrahiert werden.

Für Clustering gelten nur `SUCCESS`-Antworten als reguläre Routen. `PARTIAL` und `FAILED` werden als Outlier behandelt.

### RouteExtraction

```java
public record RouteExtraction(
        int responseIndex,
        String rawResponse,
        List<String> rawExtractedNames,
        List<Destination> normalizedRoute,
        RouteExtractionStatus extractionStatus,
        List<String> unknownNames
) {
}
```

### RouteCluster

Eine Route wird als geordnete Liste von Destination-Werten verstanden.

Route-Key:

```text
GENEVE|ZERMATT|INTERLAKEN|LUCERNE|ZURICH
```

Cluster entsprechen identischen Route-Keys.

```java
public record RouteCluster(
        int clusterId,
        String routeKey,
        List<Destination> route,
        int size,
        List<Integer> repetitionIndices,
        double shareOfSuccessfulExtractions
) {
}
```

## Extraktion

### Primäre Regel

Pro Antwort werden nummerierte Listeneinträge gesucht, abgegrenzt durch Newlines.

Regel:

1. Suche Zeilen, die mit einer Nummerierung beginnen:

```text
1.
2.
3.
```

2. Extrahiere pro Listeneintrag den Stationsnamen am Anfang.
3. Wenn Markdown Bold vorhanden ist, nimm den Inhalt zwischen `**...**`.
4. Sonst nimm den Text nach der Nummer bis zum ersten Doppelpunkt, Bindestrich oder Zeilenende.
5. Entferne Markdown, führende und folgende Leerzeichen sowie Satzzeichen am Rand.

Beispiele:

```text
1. **Genève** : Point de départ...
-> Genève
```

```text
2. Zermatt: Étape incontournable...
-> Zermatt
```

```text
3. Interlaken - Située entre deux lacs...
-> Interlaken
```

### Rohkandidaten

Auch bei `PARTIAL` oder `FAILED` werden die gefundenen Rohkandidaten gespeichert. Dadurch bleibt nachvollziehbar, warum eine Antwort als Outlier behandelt wurde.

## Normalisierung

Normalisierung pro Kandidat:

1. Lowercase.
2. Trim.
3. Akzente und Umlaute entfernen oder vereinheitlichen.
4. Punkte entfernen beziehungsweise normalisieren.
5. Mehrfachleerzeichen auf ein Leerzeichen reduzieren.
6. Satzzeichen am Rand entfernen.
7. Gegen alle Varianten-Sets der bekannten Destinationen prüfen.

Beispiele:

```text
Genève -> geneve -> GENEVE
Geneve -> geneve -> GENEVE
Genf   -> genf   -> GENEVE
Zürich -> zurich -> ZURICH
Zuerich -> zuerich -> ZURICH
Lucerne -> lucerne -> LUCERNE
Luzern -> luzern -> LUCERNE
```

Wenn kein Treffer gefunden wird, wird der Rohname in `unknownNames` aufgenommen.

## Clustering

Der Route-Algorithmus benötigt keinen Epsilon- oder Threshold-Scan.

Cluster-Regel:

- Jede erfolgreich extrahierte Route wird über ihren Route-Key gruppiert.
- Identische Route-Keys bilden einen Cluster.
- `PARTIAL` und `FAILED` werden als Outlier geführt.

Outlier werden analog zu DBSCAN rapportiert: als Liste der Repetition-Indizes, die keinem regulären Cluster zugeordnet werden konnten.

## Aggregationen

### Routenaggregation

Pro Versuchsreihe berechnen:

- Anzahl erfolgreicher Extraktionen.
- Anzahl `PARTIAL`.
- Anzahl `FAILED`.
- Anzahl eindeutiger Routen.
- Häufigkeit jeder Route.
- Anteil der häufigsten Route an allen erfolgreichen Extraktionen.
- Anzahl unbekannter Ortsnamen.

Beispiel:

```text
uniqueRouteCount = 1
topRouteShare = 1.0
failedExtractionCount = 0
unknownNameCount = 0
```

### Stationshäufigkeit

Für jedes `Destination`-Enum:

- In wie vielen erfolgreich extrahierten Antworten kommt die Station vor?
- Anteil an allen erfolgreich extrahierten Antworten.

Beispiel:

```text
ZERMATT: 20/20
LUCERNE: 20/20
ZURICH: 20/20
GENEVE: 19/20
INTERLAKEN: 14/20
MONTREUX: 7/20
```

Doppelte Stationen innerhalb derselben Route zählen für die Stationshäufigkeit nur einmal.

### Positionsverteilung

Für jede Position `1..expectedStationCount`:

- Häufigkeit pro Destination.

Beispiel:

```text
Position 1:
GENEVE = 19
ZURICH = 1

Position 5:
ZURICH = 19
MONTREUX = 1
```

### Paarweise Jaccard-Ähnlichkeit

Für alle erfolgreich extrahierten Routenpaare:

1. Wandle jede Route in ein Set von `Destination`-Enums um.
2. Berechne Jaccard:

```text
intersectionSize / unionSize
```

Beispiel:

```text
A = [GENEVE, ZERMATT, INTERLAKEN, LUCERNE, ZURICH]
B = [GENEVE, MONTREUX, ZERMATT, LUCERNE, ZURICH]

intersection = 4
union = 6
jaccard = 0.6667
```

Aggregiere:

- median
- p10
- p90
- min
- max
- mean

Wenn weniger als zwei erfolgreiche Routen vorhanden sind, bleibt die Jaccard-Zusammenfassung leer beziehungsweise `count = 0`.

## Ergebnisstruktur

Der Route-Algorithmus liefert eine eigene semantische Analyse, soll aber in das bestehende `AnalysisResult` eingebettet werden können.

Vorschlag:

```java
public record RouteAnalysis(
        int responseCount,
        int successfulExtractionCount,
        int partialExtractionCount,
        int failedExtractionCount,
        int unknownNameCount,
        int uniqueRouteCount,
        Double topRouteShare,
        List<RouteExtraction> extractions,
        List<RouteCluster> clusters,
        List<Integer> outliers,
        List<StationFrequency> stationFrequencies,
        List<PositionDistribution> positionDistributions,
        MetricSummary pairwiseJaccard
) {
}
```

`AnalysisScan` kann für `ROUTE` entweder genau einen Scan-Eintrag enthalten oder der Route-Algorithmus bekommt ein eigenes Ergebnisfeld. Bevorzugt wird ein eigener `route`-Block, weil Route-Clustering keinen Scan-Parameter besitzt.

Empfohlene Erweiterung:

```java
public record AnalysisResult(
        String sourceRun,
        OffsetDateTime analyzedAt,
        AnalysisConfig config,
        AnalysisRunInfo run,
        List<AnalysisScan> scans,
        RouteAnalysis route,
        LiteralAnalysis literal
) {
}
```

Bei DBSCAN und Hierarchical ist `route = null`. Bei `ROUTE` ist `scans = []` oder `null`, je nachdem was besser zum bestehenden JSON passt. Bevorzugt wird `scans = []`, damit der JSON-Typ stabil bleibt.

## Syntaktische und Literale Analyse

Literal bleibt unverändert:

- Immer auf Basis der gesamten Rohantwortmenge.
- Nicht auf Basis der extrahierten Route.

Syntaktisch:

- Wenn Route-Cluster vorhanden sind, wird syntaktische Analyse pro Route-Cluster auf Basis der Rohantworten berechnet.
- Outlier werden nicht in die clusterbasierte syntaktische Analyse aufgenommen.
- Optional kann zusätzlich eine syntaktische Gesamtanalyse über alle Rohantworten berechnet werden. Diese Erweiterung ist nützlich, aber nicht zwingend für das erste Increment.

Damit bleibt die Interpretation sauber:

- Route-Analyse misst inhaltliche Stabilität der Stationen.
- Syntaktik misst Formulierungsvarianz innerhalb gleicher Route.
- Literal misst exakte Rohantwortstabilität.

## Integrationstest

Lege einen Test-Runlog als Testresource ab, zum Beispiel:

```text
src/test/resources/analyze/integration/route/0003-qwen35-rundreise-french-run.json
```

Der Test verwendet den vom Benutzer gelieferten Runlog mit 50 Wiederholungen:

- `planName = 0003-qwen35-rundreise-french`
- `model = qwen/qwen3.5-9b`
- alle 50 Antworten enthalten dieselbe französische nummerierte Liste:

```text
Zurich, Lucerne, Interlaken, Zermatt, Geneve
```

Erwartungen:

- `responseCount = 50`
- `successfulExtractionCount = 50`
- `partialExtractionCount = 0`
- `failedExtractionCount = 0`
- `unknownNameCount = 0`
- `uniqueRouteCount = 1`
- `topRouteShare = 1.0`
- `clusters.size = 1`
- `clusters[0].routeKey = ZURICH|LUCERNE|INTERLAKEN|ZERMATT|GENEVE`
- `clusters[0].size = 50`
- `outliers = []`
- Stationshäufigkeiten:
  - `ZURICH = 50`
  - `LUCERNE = 50`
  - `INTERLAKEN = 50`
  - `ZERMATT = 50`
  - `GENEVE = 50`
- Positionsverteilung:
  - Position 1: `ZURICH = 50`
  - Position 2: `LUCERNE = 50`
  - Position 3: `INTERLAKEN = 50`
  - Position 4: `ZERMATT = 50`
  - Position 5: `GENEVE = 50`
- Paarweise Jaccard:
  - `count = 1225` (`50 * 49 / 2`)
  - `min = 1.0`
  - `median = 1.0`
  - `p10 = 1.0`
  - `p90 = 1.0`
  - `max = 1.0`
  - `mean = 1.0`

Zusätzlicher Testfall:

- Eine Antwort mit nur vier extrahierten Stationen wird als Outlier behandelt.
- Eine Antwort mit unbekanntem Ort wird `PARTIAL` und ebenfalls Outlier.
- Rohkandidaten und unbekannte Namen werden im Ergebnis gespeichert.

## Implementierungsschritte

1. `ClusteringAlgorithm` um `ROUTE` erweitern.
2. `YamlAnalysisConfig` und `AnalysisConfig` um `route.expectedStationCount` erweitern.
3. `Destination` Enum mit Varianten anlegen.
4. `DestinationNormalizer` implementieren.
5. `RouteStationExtractor` implementieren.
6. `RouteAnalyzer` implementieren:
   - Extraktion
   - Normalisierung
   - Route-Key-Bildung
   - Clusterbildung
   - Outlier
   - Stationshäufigkeiten
   - Positionsverteilung
   - Jaccard-Zusammenfassung
7. `Analyzer` so erweitern, dass bei `clusteringAlgorithm == ROUTE` der Route-Pfad genutzt wird.
8. Syntaktische Analyse pro Route-Cluster auf Rohantworten anwenden.
9. Literal-Analyse unverändert über alle Rohantworten berechnen.
10. JSON-Golden-Test ergänzen.
11. Integrationstest mit dem französischen Qwen-Runlog ergänzen.

## Risiken und Grenzen

- Die Extraktion setzt eine nummerierte Liste voraus. Stark freie Fließtexte können fehlschlagen.
- Bekannte Ziele sind durch das Enum begrenzt. Neue Orte müssen bewusst ergänzt werden.
- Zusammengesetzte Stationen wie `Interlaken und Lauterbrunnen` benötigen gepflegte Varianten.
- Die Methode ist sehr gut für den Rundreise-Use-Case, aber nicht generisch für beliebige Antworttypen.

Diese Einschränkung ist erwünscht: Der Algorithmus ist use-case-spezifisch und dadurch methodisch besser interpretierbar als ein generischer semantischer Clustervergleich.
