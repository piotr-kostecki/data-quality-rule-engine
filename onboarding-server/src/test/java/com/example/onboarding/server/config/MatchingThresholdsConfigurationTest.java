package com.example.onboarding.server.config;

import com.example.onboarding.server.PartnerOnboardingServer;
import com.example.partnerquality.matching.DuplicateDetector;
import com.example.partnerquality.matching.MatchingPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Duplicate thresholds come from configuration (onboarding.matching.*), and invalid values stop the start-up. */
@SpringBootTest(properties = {
        "onboarding.matching.candidate-threshold=0.5",
        "onboarding.matching.blocking-threshold=0.7"
})
class MatchingThresholdsConfigurationTest {

    @Autowired
    private DuplicateDetector duplicateDetector;

    @Test
    void detectorUsesTheConfiguredThresholds() {
        assertThat(duplicateDetector.policy()).isEqualTo(new MatchingPolicy(0.5, 0.7));
    }

    @Test
    void inconsistentThresholdsFailFastAtStartUp() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(PartnerOnboardingServer.class)
                .web(WebApplicationType.NONE);

        // Command-line arguments, because builder properties() are defaults that application.yml overrides
        assertThatThrownBy(() -> app.run(
                "--onboarding.matching.candidate-threshold=0.9", "--onboarding.matching.blocking-threshold=0.8"))
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidateThreshold <= blockingThreshold");
    }
}
