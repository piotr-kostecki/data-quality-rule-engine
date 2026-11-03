package com.example.onboarding.server.adapter.in.web.dto;

import com.example.onboarding.application.port.in.QualityCheck;

import java.util.List;

public record CheckResponse(boolean onboardable, List<ViolationDto> violations, List<DuplicateCandidateDto> duplicates) {

    public static CheckResponse from(QualityCheck check) {
        return new CheckResponse(check.onboardable(),
                ViolationDto.fromAll(check.report().violations()),
                DuplicateCandidateDto.fromAll(check.duplicates()));
    }
}
