package com.example.onboarding.application.port.out;

import com.example.onboarding.application.domain.BusinessPartner;

public interface SavePartnerPort {

    void save(BusinessPartner partner);
}
