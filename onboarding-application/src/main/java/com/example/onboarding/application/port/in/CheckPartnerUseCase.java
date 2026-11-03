package com.example.onboarding.application.port.in;

import com.example.partnerquality.PartnerData;

/** Dry run of onboarding: report rule violations and duplicate candidates, store nothing. */
public interface CheckPartnerUseCase {

    QualityCheck check(PartnerData data);
}
