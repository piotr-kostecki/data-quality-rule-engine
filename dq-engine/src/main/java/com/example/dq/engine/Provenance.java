package com.example.dq.engine;

import java.util.List;

/**
 * Explains a decision: which rule, which record, which fields with which values, and how the computed value was mapped.
 *
 * @param ruleId        the rule that decided
 * @param record        the record it decided on
 * @param value         the value the rule's logic computed
 * @param fields        the fields the rule references, with their values in this record
 * @param defaultMapped {@code true} if no explicit mapping case matched and the default decision applied
 */
public record Provenance(String ruleId, RecordRef record, String value, List<FieldValue> fields, boolean defaultMapped) {

    public Provenance {
        fields = List.copyOf(fields);
    }
}
