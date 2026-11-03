package com.example.onboarding.application.service;

import com.example.onboarding.application.port.in.OnboardPartnerCommand;
import com.example.onboarding.application.port.in.OnboardingResult;
import com.example.onboarding.application.port.in.OnboardingResult.DuplicateSuspected;
import com.example.onboarding.application.port.in.OnboardingResult.Onboarded;
import com.example.onboarding.application.port.in.OnboardingResult.Rejected;
import com.example.onboarding.application.port.in.QualityCheck;
import com.example.partnerquality.Address;
import com.example.partnerquality.PartnerData;
import com.example.partnerquality.matching.DuplicateDetector;
import com.example.partnerquality.matching.MatchReason;
import com.example.partnerquality.testing.PartnerFixtures;
import com.example.partnerquality.validation.DataQualityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** The use cases with the real library and a fake for every outgoing port; no framework, no mocks. */
class OnboardingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    private InMemoryPartnerStore store;
    private OnboardingService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryPartnerStore();
        service = new OnboardingService(
                DataQualityEngine.withBuiltInRules(),
                DuplicateDetector.withDefaultPolicy(),
                store, store, store,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void onboardsValidPartnerAndPublishesEvent() {
        OnboardingResult result = service.onboard(new OnboardPartnerCommand(PartnerFixtures.acme()));

        assertThat(result).isInstanceOfSatisfying(Onboarded.class, onboarded -> {
            assertThat(onboarded.partner().data()).isEqualTo(PartnerFixtures.acme());
            assertThat(onboarded.partner().onboardedAt()).isEqualTo(NOW);
        });
        assertThat(store.partners).hasSize(1);
        assertThat(store.events).singleElement().satisfies(e -> assertThat(e.country()).isEqualTo("DE"));
    }

    @Test
    void rejectsDataWithErrorsAndStoresNothing() {
        PartnerData broken = new PartnerData(null, new Address(null, null, null, "DE"), "DE000000000", null);

        OnboardingResult result = service.onboard(new OnboardPartnerCommand(broken));

        assertThat(result).isInstanceOfSatisfying(Rejected.class,
                rejected -> assertThat(rejected.report().errors()).hasSizeGreaterThanOrEqualTo(2));
        assertThat(store.partners).isEmpty();
        assertThat(store.events).isEmpty();
    }

    @Test
    void refusesDuplicate() {
        service.onboard(new OnboardPartnerCommand(PartnerFixtures.acme()));

        OnboardingResult second = service.onboard(new OnboardPartnerCommand(PartnerFixtures.acme()));

        assertThat(second).isInstanceOfSatisfying(DuplicateSuspected.class,
                d -> assertThat(d.candidates()).singleElement().extracting(m -> m.reason()).isEqualTo(MatchReason.SAME_VAT_ID));
        assertThat(store.partners).hasSize(1);
    }

    @Test
    void findsDuplicateWithSameIbanInAnotherCountry() {
        service.onboard(new OnboardPartnerCommand(PartnerFixtures.acme()));
        PartnerData swissSubsidiary = new PartnerData("Acme Schweiz AG",
                new Address("Bahnhofstrasse 1", "8001", "Zürich", "CH"), null, PartnerFixtures.VALID_DE_IBAN);

        QualityCheck check = service.check(swissSubsidiary);

        assertThat(check.duplicates()).singleElement().extracting(m -> m.reason()).isEqualTo(MatchReason.SAME_IBAN);
        assertThat(check.onboardable()).isTrue();
    }

    @Test
    void checkReportsABlockingDuplicateAsNotOnboardable() {
        service.onboard(new OnboardPartnerCommand(PartnerFixtures.acme()));

        QualityCheck check = service.check(PartnerFixtures.acme());

        assertThat(check.report().hasErrors()).isFalse();
        assertThat(check.duplicates()).singleElement().extracting(m -> m.reason()).isEqualTo(MatchReason.SAME_VAT_ID);
        assertThat(check.onboardable()).isFalse();
        assertThat(store.partners).hasSize(1);
    }

    @Test
    void checkIsADryRun() {
        QualityCheck check = service.check(PartnerFixtures.acme());

        assertThat(check.onboardable()).isTrue();
        assertThat(store.partners).isEmpty();
    }

    @Test
    void findByCountryIsCaseInsensitive() {
        service.onboard(new OnboardPartnerCommand(PartnerFixtures.polishLogistics()));

        assertThat(service.findByCountry("pl")).hasSize(1);
    }
}
