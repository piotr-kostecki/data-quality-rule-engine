package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One input record, e.g. a business partner parsed from a JSON object: field names to values.
 *
 * <p>Values are {@code String}, {@code Long}, {@code Double}, {@code Boolean}, {@code List} or {@code Map}.
 * Smaller integer types are widened to {@code Long} and {@code Float} to {@code Double}, the types expression
 * languages such as CEL expect. A {@code null} value is treated as an absent field.
 *
 * @param fields the record's fields, in input order
 */
public record DataRecord(Map<String, Object> fields) {

    public DataRecord {
        Map<String, Object> copy = new LinkedHashMap<>();
        fields.forEach((name, value) -> {
            if (value != null) {
                copy.put(name, normalize(value));
            }
        });
        fields = Collections.unmodifiableMap(copy);
    }

    /** Record from any map, e.g. a parsed JSON object; {@code null} values are dropped. */
    public static DataRecord of(Map<String, ? extends @Nullable Object> fields) {
        return new DataRecord(Collections.unmodifiableMap(fields));
    }

    /** The field's value, or {@code null} if the record has no such field. */
    public @Nullable Object get(String field) {
        return fields.get(field);
    }

    public boolean has(String field) {
        return fields.containsKey(field);
    }

    private static Object normalize(Object value) {
        return switch (value) {
            case Integer i -> i.longValue();
            case Short s -> s.longValue();
            case Byte b -> b.longValue();
            case Float f -> f.doubleValue();
            default -> value;
        };
    }
}
