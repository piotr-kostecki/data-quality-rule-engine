package com.example.onboarding.server.config;

import com.example.partnerquality.matching.MatchingPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code onboarding.matching.*} from application.yml. */
@ConfigurationProperties(prefix = "onboarding.matching")
public record MatchingProperties(double candidateThreshold, double blockingThreshold) {

    public MatchingPolicy toPolicy() {
        return new MatchingPolicy(candidateThreshold, blockingThreshold);
    }
}
