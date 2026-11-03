package com.example.dq.cel;

import com.example.dq.engine.DataRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/** The task's records (raw/REQUIREMENT.md §5), read from src/test/resources/fixtures/records.json. */
final class FixtureRecords {

    private FixtureRecords() {
    }

    static List<DataRecord> load() {
        try (InputStream json = FixtureRecords.class.getResourceAsStream("/fixtures/records.json")) {
            List<Map<String, Object>> objects = new ObjectMapper().readValue(json, new TypeReference<>() {
            });
            return objects.stream().map(DataRecord::of).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
