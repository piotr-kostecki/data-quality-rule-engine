package com.example.onboarding.server.adapter.out.event;

import com.example.onboarding.application.domain.BusinessPartnerOnboarded;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for a downstream consumer of the event (search index, ERP sync). Here it only logs. */
@Component
@Slf4j
public class OnboardingAuditListener {

    @EventListener
    public void on(BusinessPartnerOnboarded event) {
        log.info("Business partner onboarded: id={} name='{}' country={}", event.partnerId(), event.name(), event.country());
    }
}
