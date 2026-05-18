package ch.thp.mas.llm.variance.run;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class RunFileNameFactoryTest {

    @Test
    void createsTimestampedJsonFileNameWithPlanName() {
        RunFileNameFactory factory = new RunFileNameFactory();

        String filename = factory.create(
                OffsetDateTime.parse("2026-05-02T10:45:30.123+02:00"),
                "0001-rundreise-schweiz"
        );

        assertThat(filename).isEqualTo("20260502-104530-123-run-0001-rundreise-schweiz.json");
    }

    @Test
    void sanitizesUnsafeCharacters() {
        RunFileNameFactory factory = new RunFileNameFactory();

        String filename = factory.create(
                OffsetDateTime.parse("2026-05-02T10:45:30.123+02:00"),
                "0001-weird/name"
        );

        assertThat(filename).isEqualTo("20260502-104530-123-run-0001-weird_name.json");
    }
}
