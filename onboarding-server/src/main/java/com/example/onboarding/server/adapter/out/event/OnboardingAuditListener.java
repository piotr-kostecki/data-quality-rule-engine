package com.example.onboarding.server.adapter.out.event;

import com.example.onboarding.application.domain.BusinessPartnerOnboarded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for a downstream consumer of the event (search index, SAP sync). Here it only logs. */
@Component
public class OnboardingAuditListener {

    private static final Logger log = LoggerFactory.getLogger(OnboardingAuditListener.class);

    @EventListener
    public void on(BusinessPartnerOnboarded event) {
        log.info("Business partner onboarded: id={} name='{}' country={}", event.partnerId(), event.name(), event.country());
    }
}
