package com.example.onboarding.server.adapter.in.web.dto;

import com.example.partnerquality.validation.RuleViolation;
import com.example.partnerquality.validation.Severity;

import java.util.List;

public record ViolationDto(String ruleId, String field, Severity severity, String message) {

    public static ViolationDto from(RuleViolation violation) {
        return new ViolationDto(violation.ruleId(), violation.field(), violation.severity(), violation.message());
    }

    public static List<ViolationDto> fromAll(List<RuleViolation> violations) {
        return violations.stream().map(ViolationDto::from).toList();
    }
}
