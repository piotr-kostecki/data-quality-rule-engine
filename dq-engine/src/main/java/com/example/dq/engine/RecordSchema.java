package com.example.dq.engine;

/**
 * Which record fields the engine itself reads: the record id (for provenance) and the country (for rule scope).
 *
 * @param idField      field holding the record id; optional in each record
 * @param countryField field holding the ISO country code; optional in each record
 */
public record RecordSchema(String idField, String countryField) {

    /** {@code id} and {@code country}, as in the business-partner JSON records. */
    public static final RecordSchema DEFAULT = new RecordSchema("id", "country");

    public RecordSchema {
        if (idField.isBlank() || countryField.isBlank()) {
            throw new IllegalArgumentException("Schema field names must not be blank");
        }
    }
}
