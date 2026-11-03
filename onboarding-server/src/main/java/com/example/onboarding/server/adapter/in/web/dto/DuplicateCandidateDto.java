package com.example.onboarding.server.adapter.in.web.dto;

import com.example.onboarding.application.domain.BusinessPartner;
import com.example.partnerquality.matching.Match;
import com.example.partnerquality.matching.MatchReason;

import java.util.List;
import java.util.UUID;

public record DuplicateCandidateDto(UUID partnerId, String name, double score, MatchReason reason) {

    public static DuplicateCandidateDto from(Match<BusinessPartner> match) {
        double rounded = Math.round(match.score() * 1000) / 1000.0;
        return new DuplicateCandidateDto(
                match.candidate().id().value(), match.candidate().data().name(), rounded, match.reason());
    }

    public static List<DuplicateCandidateDto> fromAll(List<Match<BusinessPartner>> matches) {
        return matches.stream().map(DuplicateCandidateDto::from).toList();
    }
}
