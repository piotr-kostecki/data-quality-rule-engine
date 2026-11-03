package com.example.dq.engine;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

import static com.example.dq.engine.TestRules.presence;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * One million records through three rules on a 64 MB test heap (see build.gradle). Keeping the 3,000,000 results,
 * or the records, would not fit: the run must stream.
 */
class BoundedMemoryTest {

    private static final int RECORDS = 1_000_000;

    @Test
    void millionRecordsStreamThroughInBoundedMemory() {
        ValidationEngine engine = ValidationEngine.builder().catalog(RuleCatalog.of(
                presence("vat", "vatId").build(),
                presence("iban", "iban").build(),
                presence("name", "legalName").severity(Severity.WARNING).build())).build();
        CountingSink sink = new CountingSink();

        RunSummary summary = engine.run(generated(RECORDS), sink, RunOptions.DEFAULT.withBatchSize(500));

        assertThat(summary.records()).isEqualTo(RECORDS);
        assertThat(summary.results()).isEqualTo(3L * RECORDS);
        assertThat(sink.results).isEqualTo(3L * RECORDS);
        assertThat(sink.batches).isEqualTo(RECORDS / 500);
        assertThat(summary.count(Decision.INVALID)).as("every 10th record has no IBAN").isEqualTo(RECORDS / 10);
    }

    /** Records are created on demand and never held by the source. */
    private static Iterable<DataRecord> generated(int count) {
        return () -> new Iterator<>() {
            private int next;

            @Override
            public boolean hasNext() {
                return next < count;
            }

            @Override
            public DataRecord next() {
                int i = next++;
                return i % 10 == 0
                        ? DataRecord.of(Map.of("id", "r" + i, "country", "DE", "vatId", "DE" + i, "legalName", "Company " + i))
                        : DataRecord.of(Map.of("id", "r" + i, "country", "DE", "vatId", "DE" + i, "legalName", "Company " + i, "iban", "DE" + i));
            }
        };
    }

    private static final class CountingSink implements ResultSink {
        long results;
        long batches;

        @Override
        public void onResult(Result result) {
            results++;
        }

        @Override
        public void onError(RuleError error) {
        }

        @Override
        public void onBatchEnd(BatchInfo batch) {
            batches++;
        }
    }
}
