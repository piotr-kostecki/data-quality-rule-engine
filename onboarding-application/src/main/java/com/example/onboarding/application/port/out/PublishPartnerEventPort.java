package com.example.onboarding.application.port.out;

import com.example.onboarding.application.domain.BusinessPartnerOnboarded;

public interface PublishPartnerEventPort {

    void publish(BusinessPartnerOnboarded event);
}
