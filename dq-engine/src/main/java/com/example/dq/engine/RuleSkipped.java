package com.example.dq.engine;

/**
 * A selected rule that did not apply to a record. Emitted only at {@link ExplainLevel#SKIPS}; otherwise only counted.
 *
 * <p>ASSUMPTION(A-2): skips are separate events, never {@code NOT_APPLICABLE} results, so the summary's result
 * counts don't depend on the explain level. Requires confirmation with architects / BA, see DESIGN.md §10.
 *
 * @param record the record
 * @param ruleId the rule
 * @param reason why it did not apply
 */
public record RuleSkipped(RecordRef record, String ruleId, Reason reason) implements Outcome {

    /** Why a rule did not apply. */
    public enum Reason {
        /** The rule is scoped to other countries than the record's. */
        OUT_OF_COUNTRY_SCOPE
    }
}
