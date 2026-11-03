package com.example.onboarding.server.adapter.out.event;

import com.example.onboarding.application.domain.BusinessPartnerOnboarded;
import com.example.onboarding.application.port.out.PublishPartnerEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Driven adapter: publishes domain events in-process. A Kafka adapter would implement the same port. */
@Component
public class SpringEventPublisherAdapter implements PublishPartnerEventPort {

    private final ApplicationEventPublisher publisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(BusinessPartnerOnboarded event) {
        publisher.publishEvent(event);
    }
}
