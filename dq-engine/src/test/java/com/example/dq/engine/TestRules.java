package com.example.dq.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Rules with plain-Java logic, so the engine is tested without an expression language. */
final class TestRules {

    private TestRules() {
    }

    /** {@code "ok"} if the field is present, {@code "bad"} otherwise; ok → VALID, bad → INVALID. */
    static Rule.Builder presence(String id, String field) {
        return Rule.builder(id)
                .status(RuleStatus.RELEASED)
                .logic(RuleLogic.of(Set.of(field), record -> record.has(field) ? "ok" : "bad"))
                .mapping(DecisionMapping.when("ok", Decision.VALID).when("bad", Decision.INVALID).withImplicitDefault());
    }

    static DataRecord record(Object... keysAndValues) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            fields.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return DataRecord.of(fields);
    }
}
