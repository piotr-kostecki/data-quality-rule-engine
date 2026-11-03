package com.example.dq.engine;

import org.jspecify.annotations.Nullable;

/**
 * Identifies a record in a run.
 *
 * @param id       the record's id field, or {@code null} if it has none
 * @param position zero-based position in the input; always present
 */
public record RecordRef(@Nullable String id, long position) {

    @Override
    public String toString() {
        return id != null ? id + " (#" + position + ")" : "#" + position;
    }
}
