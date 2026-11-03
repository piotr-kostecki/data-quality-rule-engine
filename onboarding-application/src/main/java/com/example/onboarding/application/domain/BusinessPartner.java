package com.example.onboarding.application.domain;

import com.example.partnerquality.PartnerData;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity: a business partner that passed onboarding. It wraps the library's {@link PartnerData} and adds
 * what only this application knows: identity and when it was onboarded.
 */
public record BusinessPartner(PartnerId id, PartnerData data, Instant onboardedAt) {

    public BusinessPartner {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(data.name(), "an onboarded partner always has a name");
        Objects.requireNonNull(onboardedAt, "onboardedAt");
    }

    public static BusinessPartner onboard(PartnerData data, Instant now) {
        return new BusinessPartner(PartnerId.newId(), data, now);
    }
}
