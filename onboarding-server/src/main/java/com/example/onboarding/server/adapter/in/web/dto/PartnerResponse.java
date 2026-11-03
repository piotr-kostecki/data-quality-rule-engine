package com.example.onboarding.server.adapter.in.web.dto;

import com.example.onboarding.application.domain.BusinessPartner;

import java.time.Instant;
import java.util.UUID;

public record PartnerResponse(UUID id, String name, AddressDto address, String vatId, String iban, Instant onboardedAt) {

    public static PartnerResponse from(BusinessPartner partner) {
        return new PartnerResponse(partner.id().value(), partner.data().name(), AddressDto.from(partner.data().address()),
                partner.data().vatId(), partner.data().iban(), partner.onboardedAt());
    }
}
