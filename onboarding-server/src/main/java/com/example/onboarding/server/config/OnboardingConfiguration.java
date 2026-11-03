package com.example.onboarding.server.config;

import com.example.onboarding.application.port.out.LoadPartnerPort;
import com.example.onboarding.application.port.out.PublishPartnerEventPort;
import com.example.onboarding.application.port.out.SavePartnerPort;
import com.example.onboarding.application.service.OnboardingService;
import com.example.partnerquality.matching.DuplicateDetector;
import com.example.partnerquality.validation.DataQualityEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Composition root: plugs the driven adapters into the framework-free service. The service bean implements
 * all incoming ports, so the controller receives it typed as OnboardPartnerUseCase, CheckPartnerUseCase
 * and FindPartnersQuery.
 */
@Configuration
public class OnboardingConfiguration {

    @Bean
    DataQualityEngine dataQualityEngine() {
        return DataQualityEngine.withBuiltInRules();
    }

    @Bean
    DuplicateDetector duplicateDetector(MatchingProperties properties) {
        return new DuplicateDetector(properties.toPolicy());
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OnboardingService onboardingService(
            DataQualityEngine dataQualityEngine,
            DuplicateDetector duplicateDetector,
            LoadPartnerPort loadPartnerPort,
            SavePartnerPort savePartnerPort,
            PublishPartnerEventPort publishPartnerEventPort,
            Clock clock) {
        return new OnboardingService(
                dataQualityEngine, duplicateDetector, loadPartnerPort, savePartnerPort, publishPartnerEventPort, clock);
    }
}
