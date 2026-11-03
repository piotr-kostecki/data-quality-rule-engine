package com.example.dq.engine;

import java.time.Duration;
import java.util.Map;

/**
 * Counts of a run. Computed from fixed-size counters while the run streams, never from a list of results.
 *
 * @param records            records read
 * @param rulesSelected      rules selected by the filter and the {@code RELEASED} guard
 * @param results            results emitted
 * @param errors             errors emitted
 * @param skipped            rule/record pairs skipped because the rule's country scope excluded the record
 * @param resultsByDecision  results per decision; every decision is present, possibly with 0
 * @param resultsBySeverity  results per severity; every severity is present, possibly with 0
 * @param elapsed            wall-clock duration of the run
 */
public record RunSummary(
        long records,
        int rulesSelected,
        long results,
        long errors,
        long skipped,
        Map<Decision, Long> resultsByDecision,
        Map<Severity, Long> resultsBySeverity,
        Duration elapsed) {

    public RunSummary {
        resultsByDecision = Map.copyOf(resultsByDecision);
        resultsBySeverity = Map.copyOf(resultsBySeverity);
    }

    public long count(Decision decision) {
        return resultsByDecision.getOrDefault(decision, 0L);
    }

    public long count(Severity severity) {
        return resultsBySeverity.getOrDefault(severity, 0L);
    }
}
