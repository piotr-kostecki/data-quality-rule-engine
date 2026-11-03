package com.example.onboarding.server.config;

import com.example.onboarding.application.port.in.OnboardPartnerCommand;
import com.example.onboarding.application.port.in.OnboardPartnerUseCase;
import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Seeds a few fictional partners when started with the {@code demo} profile, through the normal use case. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final OnboardPartnerUseCase onboardPartner;

    @Override
    public void run(ApplicationArguments args) {
        onboard(new PartnerData("Acme Chemicals GmbH",
                new Address("Industriestraße 1", "10115", "Berlin", "DE"), "DE136695976", "DE89370400440532013000"));
        onboard(new PartnerData("Przykład Logistyka sp. z o.o.",
                new Address("ul. Testowa 1", "30-001", "Kraków", "PL"), "PL1234563218", "PL61109010140000071219812874"));
        onboard(new PartnerData("Alpen Präzision AG",
                new Address("Bahnhofstrasse 10", "8001", "Zürich", "CH"), "CHE-116.281.710 MWST", "CH9300762011623852957"));
    }

    private void onboard(PartnerData data) {
        onboardPartner.onboard(new OnboardPartnerCommand(data));
    }
}
