package com.example.partnerquality;

import com.example.partnerquality.internal.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Business-partner data to check, as a client submitted it. It may be incomplete or wrong; finding that out
 * is the library's job.
 *
 * <p>Input is normalized on construction: strings are stripped and blank becomes {@code null}; VAT ID and
 * IBAN are compacted ({@code "de 136.695-976"} becomes {@code "DE136695976"}).
 */
public record PartnerData(
        @Nullable String name,
        Address address,
        @Nullable String vatId,
        @Nullable String iban) {

    public PartnerData {
        Objects.requireNonNull(address, "address (use Address.empty() when unknown)");
        name = Text.clean(name);
        vatId = Text.compactIdentifier(vatId);
        iban = Text.compactIdentifier(iban);
    }
}
