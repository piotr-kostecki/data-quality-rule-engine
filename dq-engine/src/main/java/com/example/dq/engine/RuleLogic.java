package com.example.dq.engine;

import java.util.Set;
import java.util.function.Function;

/**
 * Computes a rule's <em>value</em> (e.g. {@code "ok"} or {@code "blocked"}) from one record. It never returns a
 * {@link Decision}: turning the value into a decision is the job of the rule's {@link DecisionMapping}.
 *
 * <p>Implementations must be pure (no I/O, no other records, no clock) and thread-safe. They may throw: the
 * engine reports any exception as a {@link RuleError} for this rule and record, and continues. Throw
 * {@link MissingFieldException} when a required field is absent, so the error says which field.
 *
 * <p>The {@code dq-engine-cel} module provides implementations compiled from CEL expressions.
 */
public interface RuleLogic {

    /** The computed value; never {@code null}. */
    String evaluate(DataRecord record);

    /** The fields this logic reads. Their values are reported in each result's {@link Provenance}. */
    Set<String> referencedFields();

    /** Logic written in Java, e.g. for tests or host-specific checks. */
    static RuleLogic of(Set<String> referencedFields, Function<DataRecord, String> logic) {
        Set<String> fields = Set.copyOf(referencedFields);
        return new RuleLogic() {
            @Override
            public String evaluate(DataRecord record) {
                return logic.apply(record);
            }

            @Override
            public Set<String> referencedFields() {
                return fields;
            }
        };
    }
}
