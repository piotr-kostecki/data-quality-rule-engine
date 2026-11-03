package com.example.onboarding.application.domain;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier, so a partner id cannot be mixed up with any other UUID by accident. */
public record PartnerId(UUID value) {

    public PartnerId {
        Objects.requireNonNull(value, "value");
    }

    public static PartnerId newId() {
        return new PartnerId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
