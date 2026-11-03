package com.example.onboarding.application.port.out;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.domain.PartnerId;

import java.util.List;
import java.util.Optional;

public interface LoadPartnerPort {

    Optional<BusinessPartner> findById(PartnerId id);

    List<BusinessPartner> findAll();

    /** @param country upper-case ISO 3166-1 alpha-2 code */
    List<BusinessPartner> findByCountry(String country);

    /** @param iban compact IBAN as produced by {@link com.example.partnerquality.PartnerData} */
    List<BusinessPartner> findByIban(String iban);
}
