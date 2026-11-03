package com.example.partnerquality.internal.rules;

import com.example.partnerquality.PartnerData;
import com.example.partnerquality.validation.DataQualityRule;
import com.example.partnerquality.validation.RuleIds;
import com.example.partnerquality.validation.RuleViolation;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates an optional IBAN (ISO 13616): structure, country-specific length and the mod-97 check digits.
 * An IBAN from another country than the address is allowed but flagged, because it is a classic
 * payment-fraud pattern ("please pay to our new bank account").
 */
public final class IbanRule implements DataQualityRule {

    private static final String ID = RuleIds.IBAN;
    private static final String FIELD = "iban";

    private static final Pattern STRUCTURE = Pattern.compile("[A-Z]{2}\\d{2}[A-Z0-9]{11,30}");

    private static final Map<String, Integer> LENGTHS = Map.ofEntries(
            Map.entry("AT", 20), Map.entry("BE", 16), Map.entry("CH", 21), Map.entry("DE", 22),
            Map.entry("ES", 24), Map.entry("FR", 27), Map.entry("GB", 22), Map.entry("IT", 27),
            Map.entry("NL", 18), Map.entry("PL", 28));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<RuleViolation> check(PartnerData data) {
        String iban = data.iban();
        if (iban == null) {
            return List.of();
        }
        if (!STRUCTURE.matcher(iban).matches()) {
            return List.of(RuleViolation.error(ID, FIELD, "IBAN '%s' has an invalid structure".formatted(iban)));
        }

        String ibanCountry = iban.substring(0, 2);
        Integer expectedLength = LENGTHS.get(ibanCountry);
        if (expectedLength != null && iban.length() != expectedLength) {
            return List.of(RuleViolation.error(ID, FIELD,
                    "IBAN for %s must have %d characters, got %d".formatted(ibanCountry, expectedLength, iban.length())));
        }
        if (!hasValidCheckDigits(iban)) {
            return List.of(RuleViolation.error(ID, FIELD, "IBAN '%s' has invalid check digits".formatted(iban)));
        }

        String addressCountry = data.address().country();
        if (addressCountry != null && !addressCountry.equals(ibanCountry)) {
            return List.of(RuleViolation.warning(ID, FIELD,
                    "IBAN country %s differs from address country %s".formatted(ibanCountry, addressCountry)));
        }
        return List.of();
    }

    /**
     * Move the first four characters to the end, replace letters with numbers (A=10 ... Z=35); the resulting
     * integer mod 97 must be 1. The number has ~30 digits, so the remainder is computed digit by digit
     * instead of with BigInteger: (r * 10 + d) mod 97, or (r * 100 + n) mod 97 for a two-digit letter value.
     */
    static boolean hasValidCheckDigits(String iban) {
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        int remainder = 0;
        for (int i = 0; i < rearranged.length(); i++) {
            int value = Character.getNumericValue(rearranged.charAt(i));
            remainder = (remainder * (value >= 10 ? 100 : 10) + value) % 97;
        }
        return remainder == 1;
    }
}
