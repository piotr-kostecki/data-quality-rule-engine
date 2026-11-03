package com.example.onboarding.server.adapter.in.web.dto;

import com.example.onboarding.application.port.in.OnboardingResult.Onboarded;

import java.util.List;

public record OnboardingResponse(
        PartnerResponse partner, List<ViolationDto> warnings, List<DuplicateCandidateDto> possibleDuplicates) {

    public static OnboardingResponse from(Onboarded onboarded) {
        return new OnboardingResponse(PartnerResponse.from(onboarded.partner()),
                ViolationDto.fromAll(onboarded.warnings()),
                DuplicateCandidateDto.fromAll(onboarded.possibleDuplicates()));
    }
}
