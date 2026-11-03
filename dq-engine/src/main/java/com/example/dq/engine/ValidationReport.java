package com.example.dq.engine;

import java.util.List;
import java.util.Optional;

/**
 * Everything a run produced, collected in memory. Only for bounded input such as one HTTP request; large runs
 * use {@link ValidationEngine#run} with a {@link ResultSink}.
 *
 * @param results all results in emission order
 * @param errors  all errors in emission order
 * @param skipped skip events (only at {@link ExplainLevel#SKIPS})
 * @param summary the run's counts
 */
public record ValidationReport(List<Result> results, List<RuleError> errors, List<RuleSkipped> skipped, RunSummary summary) {

    public ValidationReport {
        results = List.copyOf(results);
        errors = List.copyOf(errors);
        skipped = List.copyOf(skipped);
    }

    /** The result of one rule for the record with this id, if the rule produced one. */
    public Optional<Result> result(String recordId, String ruleId) {
        return results.stream()
                .filter(r -> recordId.equals(r.record().id()) && ruleId.equals(r.ruleId()))
                .findFirst();
    }
}
