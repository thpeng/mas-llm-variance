package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class AnalysisFileNameFactoryTest {

    @Test
    void createsTimestampedAnalysisFilename() {
        String filename = new AnalysisFileNameFactory().create(
                "20260502-104530-123-0001-rundreise-schweiz.json",
                OffsetDateTime.parse("2026-05-02T11:15:00+02:00")
        );

        assertThat(filename)
                .isEqualTo("20260502-104530-123-0001-rundreise-schweiz-analyze-20260502-111500-000.json");
    }
}
