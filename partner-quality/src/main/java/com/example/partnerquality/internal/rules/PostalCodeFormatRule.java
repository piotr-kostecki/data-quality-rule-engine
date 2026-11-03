package com.example.partnerquality.internal.rules;

import com.example.partnerquality.PartnerData;
import com.example.partnerquality.validation.DataQualityRule;
import com.example.partnerquality.validation.RuleIds;
import com.example.partnerquality.validation.RuleViolation;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Checks the postal code format for countries we know; other countries are not checked. */
public final class PostalCodeFormatRule implements DataQualityRule {

    private static final Map<String, Pattern> FORMATS = Map.ofEntries(
            Map.entry("AT", Pattern.compile("\\d{4}")),
            Map.entry("BE", Pattern.compile("\\d{4}")),
            Map.entry("CH", Pattern.compile("\\d{4}")),
            Map.entry("DE", Pattern.compile("\\d{5}")),
            Map.entry("ES", Pattern.compile("\\d{5}")),
            Map.entry("FR", Pattern.compile("\\d{5}")),
            Map.entry("GB", Pattern.compile("[A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}")),
            Map.entry("IT", Pattern.compile("\\d{5}")),
            Map.entry("NL", Pattern.compile("\\d{4} ?[A-Z]{2}")),
            Map.entry("PL", Pattern.compile("\\d{2}-\\d{3}")),
            Map.entry("US", Pattern.compile("\\d{5}(-\\d{4})?")));

    @Override
    public String id() {
        return RuleIds.POSTAL_CODE_FORMAT;
    }

    @Override
    public List<RuleViolation> check(PartnerData data) {
        String postalCode = data.address().postalCode();
        String country = data.address().country();
        Pattern format = country == null ? null : FORMATS.get(country);
        if (postalCode == null || format == null || format.matcher(postalCode).matches()) {
            return List.of();
        }
        return List.of(RuleViolation.error(id(), "address.postalCode",
                "'%s' is not a valid postal code for %s".formatted(postalCode, country)));
    }
}
