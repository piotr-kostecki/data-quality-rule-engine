package com.example.onboarding.application.port.in;

/** Validate, check for duplicates and store a new business partner. */
public interface OnboardPartnerUseCase {

    OnboardingResult onboard(OnboardPartnerCommand command);
}
