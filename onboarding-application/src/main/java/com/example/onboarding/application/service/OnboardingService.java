package com.example.onboarding.application.service;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.onboarding.application.domain.BusinessPartnerOnboarded;
import com.example.onboarding.application.domain.PartnerId;
import com.example.onboarding.application.port.in.CheckPartnerUseCase;
import com.example.onboarding.application.port.in.FindPartnersQuery;
import com.example.onboarding.application.port.in.OnboardPartnerCommand;
import com.example.onboarding.application.port.in.OnboardPartnerUseCase;
import com.example.onboarding.application.port.in.OnboardingResult;
import com.example.onboarding.application.port.in.OnboardingResult.DuplicateSuspected;
import com.example.onboarding.application.port.in.OnboardingResult.Onboarded;
import com.example.onboarding.application.port.in.OnboardingResult.Rejected;
import com.example.onboarding.application.port.in.QualityCheck;
import com.example.onboarding.application.port.out.LoadPartnerPort;
import com.example.onboarding.application.port.out.PublishPartnerEventPort;
import com.example.onboarding.application.port.out.SavePartnerPort;
import com.example.partnerquality.PartnerData;
import com.example.partnerquality.matching.DuplicateDetector;
import com.example.partnerquality.matching.Match;
import com.example.partnerquality.validation.DataQualityEngine;
import com.example.partnerquality.validation.ValidationReport;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Implements the incoming ports. Orchestrates the partner-quality library and the outgoing ports; contains
 * no HTTP, storage or framework code.
 */
public class OnboardingService implements OnboardPartnerUseCase, CheckPartnerUseCase, FindPartnersQuery {

    private final DataQualityEngine qualityEngine;
    private final DuplicateDetector duplicateDetector;
    private final LoadPartnerPort loadPartnerPort;
    private final SavePartnerPort savePartnerPort;
    private final PublishPartnerEventPort publishPartnerEventPort;
    private final Clock clock;

    public OnboardingService(
            DataQualityEngine qualityEngine,
            DuplicateDetector duplicateDetector,
            LoadPartnerPort loadPartnerPort,
            SavePartnerPort savePartnerPort,
            PublishPartnerEventPort publishPartnerEventPort,
            Clock clock) {
        this.qualityEngine = qualityEngine;
        this.duplicateDetector = duplicateDetector;
        this.loadPartnerPort = loadPartnerPort;
        this.savePartnerPort = savePartnerPort;
        this.publishPartnerEventPort = publishPartnerEventPort;
        this.clock = clock;
    }

    @Override
    public QualityCheck check(PartnerData data) {
        ValidationReport report = qualityEngine.validate(data);
        List<Match<BusinessPartner>> duplicates = findDuplicates(data);
        boolean onboardable = !report.hasErrors() && duplicates.stream().noneMatch(duplicateDetector::isBlocking);
        return new QualityCheck(report, duplicates, onboardable);
    }

    /**
     * {@code synchronized} makes the check-then-save sequence atomic within this single JVM; with a real
     * database and several instances that guarantee must come from the database instead (see docs/adr/0004).
     */
    @Override
    public synchronized OnboardingResult onboard(OnboardPartnerCommand command) {
        PartnerData data = command.data();
        ValidationReport report = qualityEngine.validate(data);
        if (report.hasErrors()) {
            return new Rejected(report);
        }

        List<Match<BusinessPartner>> duplicates = findDuplicates(data);
        if (duplicates.stream().anyMatch(duplicateDetector::isBlocking)) {
            return new DuplicateSuspected(duplicates);
        }

        BusinessPartner partner = BusinessPartner.onboard(data, clock.instant());
        savePartnerPort.save(partner);
        publishPartnerEventPort.publish(BusinessPartnerOnboarded.of(partner));
        return new Onboarded(partner, report.warnings(), duplicates);
    }

    @Override
    public Optional<BusinessPartner> findById(PartnerId id) {
        return loadPartnerPort.findById(id);
    }

    @Override
    public List<BusinessPartner> findAll() {
        return loadPartnerPort.findAll();
    }

    @Override
    public List<BusinessPartner> findByCountry(String country) {
        return loadPartnerPort.findByCountry(country.toUpperCase(Locale.ROOT));
    }

    /**
     * "Blocking" in the record-linkage sense: only compare against partners that can plausibly match
     * (same country, or same IBAN in any country) instead of the whole database.
     */
    private List<Match<BusinessPartner>> findDuplicates(PartnerData data) {
        Map<PartnerId, BusinessPartner> pool = new LinkedHashMap<>();
        String country = data.address().country();
        if (country != null) {
            loadPartnerPort.findByCountry(country).forEach(p -> pool.put(p.id(), p));
        }
        if (data.iban() != null) {
            loadPartnerPort.findByIban(data.iban()).forEach(p -> pool.put(p.id(), p));
        }
        return duplicateDetector.findMatches(data, pool.values(), BusinessPartner::data);
    }
}
