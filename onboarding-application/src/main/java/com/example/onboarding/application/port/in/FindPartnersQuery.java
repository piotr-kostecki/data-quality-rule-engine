package com.example.onboarding.application.port.in;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.domain.PartnerId;

import java.util.List;
import java.util.Optional;

/** Read side. Kept separate from the commands so it can later be served from a read model (CQRS). */
public interface FindPartnersQuery {

    Optional<BusinessPartner> findById(PartnerId id);

    List<BusinessPartner> findAll();

    /** @param country ISO 3166-1 alpha-2 code, case-insensitive */
    List<BusinessPartner> findByCountry(String country);
}
