package com.example.partnerquality.internal.rules;

import com.example.partnerquality.PartnerData;
import com.example.partnerquality.internal.text.IsoCountries;
import com.example.partnerquality.validation.DataQualityRule;
import com.example.partnerquality.validation.RuleIds;
import com.example.partnerquality.validation.RuleViolation;

import java.util.List;

public final class CountryCodeRule implements DataQualityRule {

    @Override
    public String id() {
        return RuleIds.COUNTRY_CODE;
    }

    @Override
    public List<RuleViolation> check(PartnerData data) {
        String country = data.address().country();
        if (country == null || IsoCountries.isValid(country)) {
            return List.of();
        }
        return List.of(RuleViolation.error(id(), "address.country",
                "'%s' is not an ISO 3166-1 alpha-2 country code".formatted(country)));
    }
}
