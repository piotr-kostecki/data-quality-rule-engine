package com.example.dq.engine;

/**
 * What one rule decided for one record.
 *
 * @param decision   the decision from the rule's mapping
 * @param severity   the rule's severity, copied for convenience
 * @param provenance why: rule, record, fields read and computed value
 */
public record Result(Decision decision, Severity severity, Provenance provenance) implements Outcome {

    public String ruleId() {
        return provenance.ruleId();
    }

    @Override
    public RecordRef record() {
        return provenance.record();
    }
}
