package com.example.onboarding.server.adapter.in.web.dto;

import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for checks and onboarding. Deliberately has no Bean Validation annotations: all semantic
 * validation lives in the partner-quality library, so the dry run can report every problem at once.
 */
public record PartnerRequest(
        @Schema(example = "Acme Chemicals GmbH") String name,
        AddressDto address,
        @Schema(example = "DE136695976") String vatId,
        @Schema(example = "DE89370400440532013000") String iban) {

    public PartnerData toPartnerData() {
        return new PartnerData(name, address == null ? Address.empty() : address.toDomain(), vatId, iban);
    }
}
