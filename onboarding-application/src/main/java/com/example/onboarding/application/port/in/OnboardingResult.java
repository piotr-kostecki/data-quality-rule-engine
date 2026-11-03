package com.example.onboarding.application.port.in;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.partnerquality.matching.Match;
import com.example.partnerquality.validation.RuleViolation;
import com.example.partnerquality.validation.ValidationReport;

import java.util.List;

/**
 * Every possible outcome of onboarding. Expected business outcomes are values, not exceptions, and because
 * the interface is sealed, a {@code switch} over it fails to compile when a new outcome is added.
 */
public sealed interface OnboardingResult {

    record Onboarded(BusinessPartner partner, List<RuleViolation> warnings, List<Match<BusinessPartner>> possibleDuplicates)
            implements OnboardingResult {
    }

    record Rejected(ValidationReport report) implements OnboardingResult {
    }

    record DuplicateSuspected(List<Match<BusinessPartner>> candidates) implements OnboardingResult {
    }
}
