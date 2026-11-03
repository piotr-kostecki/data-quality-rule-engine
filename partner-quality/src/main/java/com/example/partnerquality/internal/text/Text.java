package com.example.partnerquality.internal.text;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/** Input-hygiene helpers. */
public final class Text {

    private Text() {
    }

    /** Strips surrounding whitespace and turns blank strings into {@code null}, so rules only need a null check. */
    public static @Nullable String clean(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /** Canonical form for identifiers such as VAT IDs and IBANs: no whitespace, dots or dashes, upper case. */
    public static @Nullable String compactIdentifier(@Nullable String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }
}
