package com.example.onboarding.application.domain;

import java.time.Instant;

/** Domain event: something downstream (search index, downstream sync, analytics) may want to react to. */
public record BusinessPartnerOnboarded(PartnerId partnerId, String name, String country, Instant occurredAt) {

    public static BusinessPartnerOnboarded of(BusinessPartner partner) {
        return new BusinessPartnerOnboarded(
                partner.id(), partner.data().name(), partner.data().address().country(), partner.onboardedAt());
    }
}
