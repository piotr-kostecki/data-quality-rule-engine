package com.example.onboarding.application.port.in;

import com.example.partnerquality.PartnerData;

import java.util.Objects;

/**
 * Input of {@link OnboardPartnerUseCase}. A dedicated command type lets the use case grow (e.g. a "force"
 * flag with a reason) without changing its method signature.
 */
public record OnboardPartnerCommand(PartnerData data) {

    public OnboardPartnerCommand {
        Objects.requireNonNull(data, "data");
    }
}
