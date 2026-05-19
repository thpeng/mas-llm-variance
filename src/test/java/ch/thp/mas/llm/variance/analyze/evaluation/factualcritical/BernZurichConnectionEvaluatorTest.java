package ch.thp.mas.llm.variance.analyze.evaluation.factualcritical;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BernZurichConnectionEvaluatorTest {

    private final BernZurichConnectionEvaluator analyzer = new BernZurichConnectionEvaluator();
    private final BernZurichConnectionConfig config = new BernZurichConnectionConfig("08:02", "09:15", 0);

    @Test
    void normalizesSupportedTimeFormats() {
        assertThat(analyzer.normalizedTimes("8:02 08:02 8.02 08.02 9:15 09.15"))
                .containsExactly("08:02", "08:02", "08:02", "08:02", "09:15", "09:15");
    }

    @Test
    void detectsZeroChangeExpressions() {
        List<String> expressions = List.of(
                "0 Umstieg",
                "0 Umstiege",
                "null Umstieg",
                "null Umstiege",
                "keine Umstiege",
                "kein Umstieg",
                "kein Umsteigen",
                "ohne Umstieg",
                "ohne Umstiege",
                "ohne umzusteigen",
                "umstiegsfrei",
                "Umstiege: keine",
                "Umstiege: 0",
                "direkte Verbindung",
                "Direktverbindung",
                "direkt"
        );

        for (String expression : expressions) {
            BernZurichConnectionExtraction extraction = analyzer.extract(
                    1,
                    "Abfahrt 08:02, Ankunft 09:15, " + expression + ".",
                    config
            );

            assertThat(extraction.status()).as(expression).isEqualTo(BernZurichConnectionStatus.SUCCESS);
            assertThat(extraction.changes()).as(expression).isZero();
            assertThat(extraction.detectedChangeExpression()).as(expression).isEqualTo(expression);
        }
    }

    @Test
    void acceptsObservedGermanResponses() {
        List<String> responses = List.of(
                "Die Abfahrtszeit ab Bern ist 08:02 Uhr, Ankunftszeit in Zürich HB 09:15 Uhr und es gibt keine Umstiege.",
                "Abfahrt ab Bern: 08:02, Ankunft in Zürich HB: 09:15, Anzahl Umstiege: 0.",
                "Die Abfahrtszeit ab Bern beträgt 08:02, die Ankunftszeit in Zürich HB ist 09:15 und es sind keine Umstiege notwendig.",
                "Die Abfahrtszeit von Bern beträgt 08:02, die Ankunft in Zürich HB erfolgt um 09:15, und es sind keine Umstiege erforderlich.",
                "Die Abfahrt ab Bern erfolgt um 08:02 mit Ankunft in Zürich HB um 09:15 ohne Umstiege."
        );

        for (int i = 0; i < responses.size(); i++) {
            BernZurichConnectionExtraction extraction = analyzer.extract(i + 1, responses.get(i), config);

            assertThat(extraction.status()).as(responses.get(i)).isEqualTo(BernZurichConnectionStatus.SUCCESS);
            assertThat(extraction.normalizedTimes()).as(responses.get(i)).containsExactly("08:02", "09:15");
            assertThat(extraction.extraTimes()).as(responses.get(i)).isEmpty();
            assertThat(extraction.departureFound()).as(responses.get(i)).isTrue();
            assertThat(extraction.arrivalFound()).as(responses.get(i)).isTrue();
            assertThat(extraction.changesFound()).as(responses.get(i)).isTrue();
            assertThat(extraction.changes()).as(responses.get(i)).isZero();
            assertThat(extraction.failureReasons()).as(responses.get(i)).isEmpty();
        }
    }

    @Test
    void marksMissingFactsAsOutlierAndKeepsExtraTimes() {
        BernZurichConnectionExtraction extraction = analyzer.extract(
                1,
                "Abfahrt 08:34, Ankunft 09:15, keine Umstiege.",
                config
        );

        assertThat(extraction.status()).isEqualTo(BernZurichConnectionStatus.OUTLIER);
        assertThat(extraction.normalizedTimes()).containsExactly("08:34", "09:15");
        assertThat(extraction.extraTimes()).containsExactly("08:34");
        assertThat(extraction.departureFound()).isFalse();
        assertThat(extraction.arrivalFound()).isTrue();
        assertThat(extraction.changesFound()).isTrue();
        assertThat(extraction.failureReasons()).containsExactly("departure_missing");
    }
}
