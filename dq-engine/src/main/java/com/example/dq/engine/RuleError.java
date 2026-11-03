package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A failure isolated to one record, and to one rule unless the record itself is unusable. The run continues.
 *
 * @param record        the record
 * @param ruleId        the failing rule, or {@code null} for {@link Kind#BAD_RECORD}
 * @param kind          what went wrong
 * @param message       human-readable detail
 * @param fields        the fields the rule references, with their values in this record (empty for a bad record)
 * @param exceptionType class name of the exception thrown by the rule, if any
 */
public record RuleError(
        RecordRef record,
        @Nullable String ruleId,
        Kind kind,
        String message,
        List<FieldValue> fields,
        @Nullable String exceptionType) implements Outcome {

    public RuleError {
        fields = List.copyOf(fields);
    }

    /** Category of an isolated failure. */
    public enum Kind {
        /** The rule required a field the record does not have. */
        MISSING_FIELD,
        /** The rule's logic threw, or returned no value. */
        EVALUATION_FAILED,
        /** The record can't be evaluated at all, e.g. its country is not text. No rule ran on it. */
        BAD_RECORD
    }
}
