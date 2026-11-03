package com.example.onboarding.application.port.in;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.partnerquality.matching.Match;
import com.example.partnerquality.validation.ValidationReport;

import java.util.List;

/** Result of a dry run: what onboarding would say, without storing anything. */
public record QualityCheck(ValidationReport report, List<Match<BusinessPartner>> duplicates, boolean onboardable) {
}
