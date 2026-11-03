package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

/**
 * A field a rule read, with the value it had in the record.
 *
 * @param field field name
 * @param value the value, or {@code null} if the record has no such field
 */
public record FieldValue(String field, @Nullable Object value) {

    public boolean present() {
        return value != null;
    }
}
