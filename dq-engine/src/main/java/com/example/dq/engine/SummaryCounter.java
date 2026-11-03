package com.example.dq.engine;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/** Fixed-size counters of a running run; memory does not grow with the number of records. Not thread-safe. */
final class SummaryCounter {

    private final int rulesSelected;
    private final long startNanos = System.nanoTime();
    private final long[] byDecision = new long[Decision.values().length];
    private final long[] bySeverity = new long[Severity.values().length];
    private long records;
    private long results;
    private long errors;
    private long skipped;

    SummaryCounter(int rulesSelected) {
        this.rulesSelected = rulesSelected;
    }

    void record() {
        records++;
    }

    void result(Result result) {
        results++;
        byDecision[result.decision().ordinal()]++;
        bySeverity[result.severity().ordinal()]++;
    }

    void error() {
        errors++;
    }

    void skipped(int count) {
        skipped += count;
    }

    RunSummary snapshot() {
        Map<Decision, Long> decisions = new EnumMap<>(Decision.class);
        for (Decision decision : Decision.values()) {
            decisions.put(decision, byDecision[decision.ordinal()]);
        }
        Map<Severity, Long> severities = new EnumMap<>(Severity.class);
        for (Severity severity : Severity.values()) {
            severities.put(severity, bySeverity[severity.ordinal()]);
        }
        return new RunSummary(records, rulesSelected, results, errors, skipped, decisions, severities,
                Duration.ofNanos(System.nanoTime() - startNanos));
    }
}
